package org.bahmni.module.hip.service;

import org.bahmni.module.hip.model.ExistingPatient;
import org.bahmni.module.hip.model.PatientAbhaInfo;
import org.openmrs.Patient;

import java.util.List;
import java.util.Set;

public interface ExistingPatientService {
    Set<Patient> getMatchingPatients(String locationUuid, String phoneNumber, String patientName, int patientYearOfBirth, String patientGender);

    String getHealthId(Patient patient);

    void checkAndAddPatientIdentifier(String patientUuid, PatientAbhaInfo abhaInfo);

    void perform(String healthId, String action);

    List<Patient> getMatchingPatients(String phoneNumber);

    List<Patient> getMatchingPatients(String locationUuid, String patientName, int patientYearOfBirth, String patientGender);

    List<ExistingPatient> getMatchingPatientDetails(Set<Patient> matchingPatients);

    String getPatientWithHealthId(String healthId);

    boolean isHealthIdVoided(String uuid);

    boolean isHealthNumberPresent(String patientUuid);

    ExistingPatient getExistingPatientWithUuid(String patientUuid);
}
