package org.bahmni.module.hip.service;

import org.bahmni.module.hip.model.CareContext;
import org.bahmni.module.hip.model.FhirOPConsult;
import org.bahmni.module.hip.model.OPConsultBundle;
import org.bahmni.module.hip.model.OpenMrsOPConsult;
import org.bahmni.module.hip.model.OrganizationContext;
import org.hl7.fhir.r4.model.Bundle;
import org.openmrs.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FhirBundledOPConsultBuilder {
    private final CareContextService careContextService;
    private final OrganizationContextService organizationContextService;
    private final FHIRResourceMapper fhirResourceMapper;
    private final AbdmConfig abdmConfig;
    private final OmrsObsDocumentTransformer omrsObsDocumentTransformer;

    @Autowired
    public FhirBundledOPConsultBuilder(CareContextService careContextService, OrganizationContextService organizationContextService, FHIRResourceMapper fhirResourceMapper, AbdmConfig abdmConfig, OmrsObsDocumentTransformer omrsObsDocumentTransformer) {
        this.careContextService = careContextService;
        this.organizationContextService = organizationContextService;
        this.fhirResourceMapper = fhirResourceMapper;
        this.abdmConfig = abdmConfig;
        this.omrsObsDocumentTransformer = omrsObsDocumentTransformer;
    }

    public OPConsultBundle fhirBundleResponseFor (OpenMrsOPConsult openMrsOPConsult) {
        Optional<Location> location = OrganizationContextService.findOrganization(openMrsOPConsult.getEncounter().getVisit().getLocation());
        OrganizationContext organizationContext = organizationContextService.buildContext(location);
        Bundle opConsultBundle = FhirOPConsult.fromOpenMrsOPConsult(openMrsOPConsult, fhirResourceMapper, abdmConfig, omrsObsDocumentTransformer).
                bundleOPConsult(organizationContext);
        CareContext careContext = careContextService.careContextFor(
                openMrsOPConsult.getEncounter(),
                organizationContext.careContextType());
        return OPConsultBundle.builder()
                .bundle(opConsultBundle)
                .careContext(careContext)
                .build();
    }
}
