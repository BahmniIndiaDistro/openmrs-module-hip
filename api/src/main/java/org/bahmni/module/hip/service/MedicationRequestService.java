package org.bahmni.module.hip.service;

import org.hl7.fhir.r4.model.MedicationRequest;

import java.util.List;

public interface MedicationRequestService {
    List<MedicationRequest> medicationRequestFor(String patientId, String byVisitType);
}
