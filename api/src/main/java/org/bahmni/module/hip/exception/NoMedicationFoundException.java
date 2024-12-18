package org.bahmni.module.hip.exception;

public class NoMedicationFoundException extends RuntimeException {

    public NoMedicationFoundException(String patientId) {
        super("No Medication found for Patient" + patientId);
    }
}
