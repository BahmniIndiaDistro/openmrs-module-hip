package org.bahmni.module.hip.service;

import org.openmrs.DrugOrder;
import org.openmrs.Patient;

import java.util.List;

public interface DrugOrderService {
    List<DrugOrder> getAllDrugOrderFor(Patient patient, String visitType);
}
