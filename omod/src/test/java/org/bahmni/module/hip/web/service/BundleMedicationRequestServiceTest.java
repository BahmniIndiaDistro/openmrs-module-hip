package org.bahmni.module.hip.web.service;


import org.hl7.fhir.r4.model.MedicationRequest;
import org.junit.Test;
import org.openmrs.VisitType;
import org.openmrs.api.VisitService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class BundleMedicationRequestServiceTest {

    private MedicationRequestService medicationRequestService = mock(MedicationRequestService.class);
    private BundleService bundleService = mock(BundleService.class);
    private VisitService visitService = mock(VisitService.class);
    private BundleMedicationRequestService bundledMedicationRequestService =
            new BundleMedicationRequestService(medicationRequestService, bundleService, visitService);

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

    @Test
    public void shouldReturnTrueForValidVisitType() {
        String visitType = "OPD";
        when(visitService.getAllVisitTypes()).thenReturn(Collections.singletonList(new VisitType("OPD", "OPD")));
        boolean actual = bundledMedicationRequestService.isValidVisit(visitType);

        assertTrue(actual);
    }
}

