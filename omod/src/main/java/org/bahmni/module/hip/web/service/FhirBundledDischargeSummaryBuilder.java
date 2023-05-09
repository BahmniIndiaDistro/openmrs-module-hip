package org.bahmni.module.hip.web.service;

import org.bahmni.module.hip.web.model.*;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FhirBundledDischargeSummaryBuilder {
    private final CareContextService careContextService;
    private final OrganizationContextService organizationContextService;
    private final FHIRResourceMapper fhirResourceMapper;
    private final AbdmConfig abdmConfig;

    @Autowired
    public FhirBundledDischargeSummaryBuilder(CareContextService careContextService, OrganizationContextService organizationContextService, FHIRResourceMapper fhirResourceMapper, AbdmConfig abdmConfig) {
        this.careContextService = careContextService;
        this.organizationContextService = organizationContextService;
        this.fhirResourceMapper = fhirResourceMapper;
        this.abdmConfig = abdmConfig;
    }

    public DischargeSummaryBundle fhirBundleResponseFor (OpenMrsDischargeSummary openMrsDischargeSummary) {
        OrganizationContext organizationContext = organizationContextService.buildContext(
                Optional.ofNullable(openMrsDischargeSummary.getEncounter().getVisit().getLocation()));

        Bundle dischargeSummaryBundle = FhirDischargeSummary.fromOpenMrsDischargeSummary(openMrsDischargeSummary, fhirResourceMapper, abdmConfig).
                bundleDischargeSummary(organizationContext);

        CareContext careContext = careContextService.careContextFor(
                openMrsDischargeSummary.getEncounter(),
                organizationContext.careContextType());

        return DischargeSummaryBundle.builder()
                .bundle(dischargeSummaryBundle)
                .careContext(careContext)
                .build();
    }
}
