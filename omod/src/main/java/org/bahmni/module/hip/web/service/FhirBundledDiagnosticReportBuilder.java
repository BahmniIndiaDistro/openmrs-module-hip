package org.bahmni.module.hip.web.service;

import org.bahmni.module.hip.web.model.CareContext;
import org.bahmni.module.hip.web.model.DiagnosticReportBundle;
import org.bahmni.module.hip.web.model.FhirDiagnosticReport;
import org.bahmni.module.hip.web.model.FhirLabResult;
import org.bahmni.module.hip.web.model.OpenMrsDiagnosticReport;
import org.bahmni.module.hip.web.model.OpenMrsLabResults;
import org.bahmni.module.hip.web.model.OrganizationContext;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
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
        OrganizationContext organizationContext = organizationContextService.buildContext(Optional.ofNullable(openMrsDiagnosticReport.getEncounter().getVisit().getLocation()));

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
        OrganizationContext organizationContext = organizationContextService.buildContext(Optional.ofNullable(results.getEncounter().getVisit().getLocation()));

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
