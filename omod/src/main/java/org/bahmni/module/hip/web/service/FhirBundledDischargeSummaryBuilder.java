package org.bahmni.module.hip.web.service;

import org.bahmni.module.hip.web.model.*;
import org.hl7.fhir.r4.model.Bundle;
import org.openmrs.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FhirBundledDischargeSummaryBuilder {
    private final CareContextService careContextService;
    private final OrganizationContextService organizationContextService;
    private final FHIRResourceMapper fhirResourceMapper;
    private final AbdmConfig abdmConfig;
    private final OmrsObsDocumentTransformer omrsObsDocumentTransformer;

    @Autowired
    public FhirBundledDischargeSummaryBuilder(CareContextService careContextService, OrganizationContextService organizationContextService, FHIRResourceMapper fhirResourceMapper, AbdmConfig abdmConfig, OmrsObsDocumentTransformer omrsObsDocumentTransformer) {
        this.careContextService = careContextService;
        this.organizationContextService = organizationContextService;
        this.fhirResourceMapper = fhirResourceMapper;
        this.abdmConfig = abdmConfig;
        this.omrsObsDocumentTransformer = omrsObsDocumentTransformer;
    }

    public DischargeSummaryBundle fhirBundleResponseFor (OpenMrsDischargeSummary openMrsDischargeSummary) {
        Optional<Location> location = OrganizationContextService.findOrganization(openMrsDischargeSummary.getEncounter().getVisit().getLocation());
        OrganizationContext organizationContext = organizationContextService.buildContext(location);

        Bundle dischargeSummaryBundle = FhirDischargeSummary.fromOpenMrsDischargeSummary(openMrsDischargeSummary, fhirResourceMapper, abdmConfig, omrsObsDocumentTransformer).
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
