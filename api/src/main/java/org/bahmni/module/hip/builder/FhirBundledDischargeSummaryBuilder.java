package org.bahmni.module.hip.builder;

import org.bahmni.module.hip.model.CareContext;
import org.bahmni.module.hip.model.DischargeSummaryBundle;
import org.bahmni.module.hip.model.FhirDischargeSummary;
import org.bahmni.module.hip.model.OpenMrsDischargeSummary;
import org.bahmni.module.hip.model.*;
import org.bahmni.module.hip.config.AbdmConfig;
import org.bahmni.module.hip.mapper.FHIRResourceMapper;
import org.bahmni.module.hip.service.OrganizationContextService;
import org.hl7.fhir.r4.model.Bundle;
import org.openmrs.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FhirBundledDischargeSummaryBuilder {
    private final OrganizationContextService organizationContextService;
    private final FHIRResourceMapper fhirResourceMapper;
    private final AbdmConfig abdmConfig;
    private final OmrsObsDocumentTransformer omrsObsDocumentTransformer;

    @Autowired
    public FhirBundledDischargeSummaryBuilder(OrganizationContextService organizationContextService, FHIRResourceMapper fhirResourceMapper, AbdmConfig abdmConfig, OmrsObsDocumentTransformer omrsObsDocumentTransformer) {
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

        CareContext careContext = CareContext.builder().careContextReference(openMrsDischargeSummary.getEncounter().getVisit().getUuid()).careContextType("Visit").build();

        return DischargeSummaryBundle.builder()
                .bundle(dischargeSummaryBundle)
                .careContext(careContext)
                .build();
    }
}
