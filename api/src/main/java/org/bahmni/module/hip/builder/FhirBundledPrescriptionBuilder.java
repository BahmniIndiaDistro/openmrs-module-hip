package org.bahmni.module.hip.builder;

import org.bahmni.module.hip.model.CareContext;
import org.bahmni.module.hip.model.FhirPrescription;
import org.bahmni.module.hip.model.OpenMrsPrescription;
import org.bahmni.module.hip.model.OrganizationContext;
import org.bahmni.module.hip.model.PrescriptionBundle;
import org.bahmni.module.hip.mapper.FHIRResourceMapper;
import org.bahmni.module.hip.service.OrganizationContextService;
import org.hl7.fhir.r4.model.Bundle;
import org.openmrs.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FhirBundledPrescriptionBuilder {
    private final OrganizationContextService organizationContextService;
    private final FHIRResourceMapper fhirResourceMapper;
    private final OmrsObsDocumentTransformer documentTransformer;

    @Autowired
    public FhirBundledPrescriptionBuilder(OrganizationContextService organizationContextService, FHIRResourceMapper fhirResourceMapper, OmrsObsDocumentTransformer documentTransformer) {
        this.organizationContextService = organizationContextService;
        this.fhirResourceMapper = fhirResourceMapper;
        this.documentTransformer = documentTransformer;
    }

    public PrescriptionBundle fhirBundleResponseFor(OpenMrsPrescription openMrsPrescription) {

        Optional<Location> location = OrganizationContextService.findOrganization(openMrsPrescription.getEncounter().getVisit().getLocation());
        OrganizationContext organizationContext = organizationContextService.buildContext(location);

        Bundle prescriptionBundle = FhirPrescription
                .from(openMrsPrescription, fhirResourceMapper, documentTransformer)
                .bundle(organizationContext);

        CareContext careContext = CareContext.builder().careContextReference(openMrsPrescription.getEncounter().getVisit().getUuid()).careContextType("Visit").build();

        return PrescriptionBundle.builder()
                .bundle(prescriptionBundle)
                .careContext(careContext)
                .build();
    }
}
