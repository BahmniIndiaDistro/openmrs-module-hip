package org.bahmni.module.hip.service;

import org.bahmni.module.hip.service.BundleService;
import org.bahmni.module.hip.service.BundledMedicationRequestService;
import org.bahmni.module.hip.service.MedicationRequestService;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class BundledMedicationRequestServiceTest {

    private MedicationRequestService medicationRequestService = mock(MedicationRequestService.class);
    private BundleService bundleService = mock(BundleService.class);

    private BundledMedicationRequestService bundledMedicationRequestService =
            new BundledMedicationRequestService(medicationRequestService, bundleService);

    @Test
    public void shouldFetchMedicationRequestForPatientBasedOnTheVisitType() {

        bundledMedicationRequestService
                .bundleMedicationRequestsFor("0f90531a-285c-438b-b265-bb3abb4745bd", "OPD");

        verify(medicationRequestService)
                .medicationRequestFor("0f90531a-285c-438b-b265-bb3abb4745bd", "OPD");
    }

    @Test
    public void shouldBundleAllMedicationRequests() {

        List<MedicationRequest> medicationRequests = new ArrayList<>();
        medicationRequests.add(new MedicationRequest());

        when(medicationRequestService.medicationRequestFor(anyString(), anyString()))
                .thenReturn(medicationRequests);

        bundledMedicationRequestService
                .bundleMedicationRequestsFor("", "");

        verify(bundleService).bundleMedicationRequests(medicationRequests);
    }
}
