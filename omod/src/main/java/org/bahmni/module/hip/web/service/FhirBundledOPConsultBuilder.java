package org.bahmni.module.hip.web.service;

import org.bahmni.module.hip.web.model.CareContext;
import org.bahmni.module.hip.web.model.FhirOPConsult;
import org.bahmni.module.hip.web.model.OPConsultBundle;
import org.bahmni.module.hip.web.model.OpenMrsOPConsult;
import org.bahmni.module.hip.web.model.OrganizationContext;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FhirBundledOPConsultBuilder {
    private final CareContextService careContextService;
    private final OrganizationContextService organizationContextService;
    private final FHIRResourceMapper fhirResourceMapper;

    @Autowired
    public FhirBundledOPConsultBuilder(CareContextService careContextService, OrganizationContextService organizationContextService, FHIRResourceMapper fhirResourceMapper) {
        this.careContextService = careContextService;
        this.organizationContextService = organizationContextService;
        this.fhirResourceMapper = fhirResourceMapper;
    }

    public OPConsultBundle fhirBundleResponseFor (OpenMrsOPConsult openMrsOPConsult) {
        OrganizationContext organizationContext = organizationContextService.buildContext(Optional.ofNullable(openMrsOPConsult.getEncounter().getVisit().getLocation()));
        Bundle opConsultBundle = FhirOPConsult.fromOpenMrsOPConsult(openMrsOPConsult, fhirResourceMapper).
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
