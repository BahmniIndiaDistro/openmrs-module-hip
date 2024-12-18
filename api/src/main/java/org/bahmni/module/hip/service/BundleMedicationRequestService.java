package org.bahmni.module.hip.service;

import org.hl7.fhir.r4.model.Bundle;

public interface BundleMedicationRequestService {
    Bundle bundleMedicationRequestsFor(String patientId, String byVisitType);
}
