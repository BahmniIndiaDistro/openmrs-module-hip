package org.bahmni.module.hip.web.service;

import org.bahmni.module.hip.web.model.CareContext;
import org.bahmni.module.hip.web.model.FhirPrescription;
import org.bahmni.module.hip.web.model.OpenMrsPrescription;
import org.bahmni.module.hip.web.model.OrganizationContext;
import org.bahmni.module.hip.web.model.PrescriptionBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.openmrs.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FhirBundledPrescriptionBuilder {
    private final CareContextService careContextService;
    private final OrganizationContextService organizationContextService;
    private final FHIRResourceMapper fhirResourceMapper;
    private final OmrsObsDocumentTransformer documentTransformer;

    @Autowired
    public FhirBundledPrescriptionBuilder(CareContextService careContextService, OrganizationContextService organizationContextService, FHIRResourceMapper fhirResourceMapper, OmrsObsDocumentTransformer documentTransformer) {
        this.careContextService = careContextService;
        this.organizationContextService = organizationContextService;
        this.fhirResourceMapper = fhirResourceMapper;
        this.documentTransformer = documentTransformer;
    }

    PrescriptionBundle fhirBundleResponseFor(OpenMrsPrescription openMrsPrescription) {

        Optional<Location> location = OrganizationContextService.findOrganization(openMrsPrescription.getEncounter().getVisit().getLocation());
        OrganizationContext organizationContext = organizationContextService.buildContext(location);

        Bundle prescriptionBundle = FhirPrescription
                .from(openMrsPrescription, fhirResourceMapper, documentTransformer)
                .bundle(organizationContext);

        CareContext careContext = careContextService.careContextFor(
                openMrsPrescription.getEncounter(),
                organizationContext.careContextType());

        return PrescriptionBundle.builder()
                .bundle(prescriptionBundle)
                .careContext(careContext)
                .build();
    }
}
