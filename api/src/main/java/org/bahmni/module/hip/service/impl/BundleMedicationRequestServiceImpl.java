package org.bahmni.module.hip.service.impl;

import org.bahmni.module.hip.service.BundleMedicationRequestService;
import org.bahmni.module.hip.service.BundleService;
import org.bahmni.module.hip.service.MedicationRequestService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BundleMedicationRequestServiceImpl implements BundleMedicationRequestService {

    private final MedicationRequestService medicationRequestService;
    private final BundleService bundleService;

    @Autowired
    public BundleMedicationRequestServiceImpl(MedicationRequestService medicationRequestService, BundleService bundleService) {
        this.medicationRequestService = medicationRequestService;
        this.bundleService = bundleService;
    }

    @Override
    public Bundle bundleMedicationRequestsFor(String patientId, String byVisitType) {

        List<MedicationRequest> medicationRequests = medicationRequestService.medicationRequestFor(patientId, byVisitType);

        return bundleService.bundleMedicationRequests(medicationRequests);
    }
}
