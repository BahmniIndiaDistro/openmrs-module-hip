package org.bahmni.module.hip.builder;

import org.bahmni.module.hip.model.CareContext;
import org.bahmni.module.hip.model.DiagnosticReportBundle;
import org.bahmni.module.hip.model.FhirDiagnosticReport;
import org.bahmni.module.hip.model.FhirLabResult;
import org.bahmni.module.hip.model.OpenMrsDiagnosticReport;
import org.bahmni.module.hip.model.OpenMrsLabResults;
import org.bahmni.module.hip.model.OrganizationContext;
import org.bahmni.module.hip.mapper.FHIRResourceMapper;
import org.bahmni.module.hip.service.OrganizationContextService;
import org.hl7.fhir.r4.model.Bundle;
import org.openmrs.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FhirBundledDiagnosticReportBuilder {
    private final OrganizationContextService organizationContextService;
    private final FHIRResourceMapper fhirResourceMapper;

    @Autowired
    public FhirBundledDiagnosticReportBuilder(OrganizationContextService organizationContextService, FHIRResourceMapper fhirResourceMapper) {
        this.organizationContextService = organizationContextService;
        this.fhirResourceMapper = fhirResourceMapper;
    }

    public DiagnosticReportBundle fhirBundleResponseFor(OpenMrsDiagnosticReport openMrsDiagnosticReport) {
        Optional<Location> location = OrganizationContextService.findOrganization(openMrsDiagnosticReport.getEncounter().getVisit().getLocation());
        OrganizationContext organizationContext = organizationContextService.buildContext(location);

        Bundle diagnosticReportBundle = FhirDiagnosticReport
                .fromOpenMrsDiagnosticReport(openMrsDiagnosticReport, fhirResourceMapper)
                .bundleDiagnosticReport(organizationContext);

        CareContext careContext = CareContext.builder().careContextReference(openMrsDiagnosticReport.getEncounter().getVisit().getUuid()).careContextType("Visit").build();

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

        CareContext careContext = CareContext.builder().careContextReference(results.getEncounter().getVisit().getUuid()).careContextType("Visit").build();

        return DiagnosticReportBundle.builder()
                .bundle(diagnosticReportBundle)
                .careContext(careContext)
                .build();
    }
}
