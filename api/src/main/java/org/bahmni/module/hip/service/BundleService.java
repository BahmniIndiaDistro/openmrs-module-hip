package org.bahmni.module.hip.service;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MedicationRequest;

import java.util.List;

public interface BundleService {
    Bundle bundleMedicationRequests(List<MedicationRequest> medicationRequests);
}
