package org.bahmni.module.hip.builder;

import org.bahmni.module.hip.model.CareContext;
import org.bahmni.module.hip.model.FhirOPConsult;
import org.bahmni.module.hip.model.OPConsultBundle;
import org.bahmni.module.hip.model.OpenMrsOPConsult;
import org.bahmni.module.hip.model.OrganizationContext;
import org.bahmni.module.hip.config.AbdmConfig;
import org.bahmni.module.hip.mapper.FHIRResourceMapper;
import org.bahmni.module.hip.service.OrganizationContextService;
import org.hl7.fhir.r4.model.Bundle;
import org.openmrs.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FhirBundledOPConsultBuilder {
    private final OrganizationContextService organizationContextService;
    private final FHIRResourceMapper fhirResourceMapper;
    private final AbdmConfig abdmConfig;
    private final OmrsObsDocumentTransformer omrsObsDocumentTransformer;

    @Autowired
    public FhirBundledOPConsultBuilder(OrganizationContextService organizationContextService, FHIRResourceMapper fhirResourceMapper, AbdmConfig abdmConfig, OmrsObsDocumentTransformer omrsObsDocumentTransformer) {
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
        CareContext careContext = CareContext.builder().careContextReference(openMrsOPConsult.getEncounter().getVisit().getUuid()).careContextType("Visit").build();
        return OPConsultBundle.builder()
                .bundle(opConsultBundle)
                .careContext(careContext)
                .build();
    }
}
