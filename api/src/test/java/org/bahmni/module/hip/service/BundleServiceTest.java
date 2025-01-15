package org.bahmni.module.hip.service;

import org.bahmni.module.hip.service.impl.BundleServiceImpl;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BundleServiceTest {

    private BundleService bundleService = new BundleServiceImpl();

    @Test
    public void shouldWrapTheMedicationRequestsInAFhirBundle() {
        List<MedicationRequest> medicationRequests = new ArrayList<>();
        medicationRequests.add(new MedicationRequest());

        Bundle bundle = bundleService.bundleMedicationRequests(medicationRequests);

        assertEquals(ResourceType.MedicationRequest, bundle.getEntry().get(0).getResource().getResourceType());
    }
}
