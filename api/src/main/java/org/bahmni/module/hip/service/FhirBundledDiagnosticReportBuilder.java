package org.bahmni.module.hip.service;

import org.bahmni.module.hip.model.CareContext;
import org.bahmni.module.hip.model.DiagnosticReportBundle;
import org.bahmni.module.hip.model.FhirDiagnosticReport;
import org.bahmni.module.hip.model.FhirLabResult;
import org.bahmni.module.hip.model.OpenMrsDiagnosticReport;
import org.bahmni.module.hip.model.OpenMrsLabResults;
import org.bahmni.module.hip.model.OrganizationContext;
import org.hl7.fhir.r4.model.Bundle;
import org.openmrs.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FhirBundledDiagnosticReportBuilder {
    private final CareContextService careContextService;
    private final OrganizationContextService organizationContextService;
    private final FHIRResourceMapper fhirResourceMapper;

    @Autowired
    public FhirBundledDiagnosticReportBuilder(CareContextService careContextService, OrganizationContextService organizationContextService, FHIRResourceMapper fhirResourceMapper) {
        this.careContextService = careContextService;
        this.organizationContextService = organizationContextService;
        this.fhirResourceMapper = fhirResourceMapper;
    }

    public DiagnosticReportBundle fhirBundleResponseFor(OpenMrsDiagnosticReport openMrsDiagnosticReport) {
        Optional<Location> location = OrganizationContextService.findOrganization(openMrsDiagnosticReport.getEncounter().getVisit().getLocation());
        OrganizationContext organizationContext = organizationContextService.buildContext(location);

        Bundle diagnosticReportBundle = FhirDiagnosticReport
                .fromOpenMrsDiagnosticReport(openMrsDiagnosticReport, fhirResourceMapper)
                .bundleDiagnosticReport(organizationContext);

        CareContext careContext = careContextService.careContextFor(
                openMrsDiagnosticReport.getEncounter(),
                organizationContext.careContextType());

        return DiagnosticReportBundle.builder()
                .bundle(diagnosticReportBundle)
                .careContext(careContext)
                .build();
    }

    public DiagnosticReportBundle fhirBundleResponseFor(OpenMrsLabResults results) {
        Optional<Location> location = OrganizationContextService.findOrganization(results.getEncounter().getVisit().getLocation());
        OrganizationContext organizationContext = organizationContextService.buildContext(location);

        Bundle diagnosticReportBundle = FhirLabResult.fromOpenMrsLabResults(results, fhirResourceMapper)
                .bundleLabResults(organizationContext, fhirResourceMapper);

        CareContext careContext = careContextService.careContextFor(
                results.getEncounter(),
                organizationContext.careContextType());

        return DiagnosticReportBundle.builder()
                .bundle(diagnosticReportBundle)
                .careContext(careContext)
                .build();
    }
}
