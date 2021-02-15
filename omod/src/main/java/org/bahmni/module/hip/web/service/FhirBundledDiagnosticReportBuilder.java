package org.bahmni.module.hip.web.service;

import org.bahmni.module.hip.web.model.*;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public DiagnosticReportBundle fhirBundleResponseFor(OpenMrsPrescription openMrsPrescription) {
        OrganizationContext organizationContext = organizationContextService.buildContext();

        Bundle diagnosticReportBundle = FhirPrescription
                .fromOpenmrsPrescription(openMrsPrescription, fhirResourceMapper)
                .bundle(organizationContext.webUrl());

        CareContext careContext = careContextService.careContextFor(
                openMrsPrescription.getEncounter(),
                organizationContext.careContextType());

        return DiagnosticReportBundle.builder()
                .bundle(diagnosticReportBundle)
                .careContext(careContext)
                .build();
    }
}
