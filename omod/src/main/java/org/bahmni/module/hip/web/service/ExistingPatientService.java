package org.bahmni.module.hip.web.service;

import org.bahmni.module.bahmnicore.contract.patient.response.PatientResponse;
import org.bahmni.module.bahmnicore.dao.PatientDao;
import org.bahmni.module.hip.api.dao.ExistingPatientDao;
import org.bahmni.module.hip.web.client.model.Status;
import org.bahmni.module.hip.web.model.ExistingPatient;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ExistingPatientService {
    private final ExistingPatientDao existingPatientDao;
    static final int MATCHING_CRITERIA_CONSTANT = 2;
    private final PatientDao patientDao;
    private final PatientService patientService;
    private final LocationService locationService;
    private static final String REGISTRATION_DESK = "Registration Desk";
    private static final String PRIMARY_CONTACT = "primaryContact";
    private static final String HEALTH_ID = "Health ID";
    private static final String PHR_ADDRESS = "PHR Address";
    static final int PHONE_NUMBER_LENGTH = 10;

    @Autowired
    public ExistingPatientService(PatientDao patientDao, PatientService patientService, ExistingPatientDao existingPatientDao, LocationService locationService) {
        this.patientDao = patientDao;
        this.patientService = patientService;
        this.existingPatientDao = existingPatientDao;
        this.locationService = locationService;
    }

    public Set<Patient> getMatchingPatients(String phoneNumber, String patientName, int patientYearOfBirth, String patientGender) {
        Set<Patient> matchingPatients = new HashSet<>();
        matchingPatients.addAll(getMatchingPatients(phoneNumber));
        matchingPatients.addAll(getMatchingPatients(patientName, patientYearOfBirth, patientGender));
        matchingPatients.removeIf(patient -> !getHealthId(patient).equals(""));
        return matchingPatients;
    }

    public String getHealthId(Patient patient) {
        String healthId = "";
        try {
            healthId = patient.getPatientIdentifier(HEALTH_ID).getIdentifier();
        } catch (NullPointerException ignored) {

        }
        return healthId;
    }

    public void perform(String healthId, String action) {
        Patient patient = patientService.getPatientByUuid(getPatientWithHealthId(healthId));
        PatientIdentifier patientIdentifierPhr = patient.getPatientIdentifier(PHR_ADDRESS);
        PatientIdentifier patientIdentifierHealthId = patient.getPatientIdentifier(HEALTH_ID);
        if (action.equals(Status.DELETED.toString())) {
            removeHealthId(patient,patientIdentifierPhr,patientIdentifierHealthId);
        }
        if (action.equals(Status.DEACTIVATED.toString())) {
            voidHealthId(patientIdentifierPhr,patientIdentifierHealthId);
        }
        if (action.equals(Status.REACTIVATED.toString())) {
            unVoidHealthId(patient,healthId);
        }
    }

    private void voidHealthId(PatientIdentifier patientIdentifierPHR,PatientIdentifier patientIdentifierHealthId) {
        try {
            if (!patientIdentifierPHR.getVoided() && !patientIdentifierHealthId.getVoided()) {
                patientService.voidPatientIdentifier(patientIdentifierPHR, Status.DEACTIVATED.toString());
                patientService.voidPatientIdentifier(patientIdentifierHealthId, Status.DEACTIVATED.toString());
            }
        } catch (NullPointerException ignored) {
        }
    }

    private void unVoidHealthId(Patient patient, String phrAddress) {
        Set<PatientIdentifier> patientIdentifiers = patient.getIdentifiers();
        try {
            for (PatientIdentifier patientIdentifier : patientIdentifiers) {
                if (patientIdentifier.getIdentifier().equals(phrAddress) || patientIdentifier.getIdentifierType().getName().equals(HEALTH_ID)) {
                    patientIdentifier.setVoided(false);
                    patientService.savePatientIdentifier(patientIdentifier);
                }
            }
        } catch (NullPointerException ignored) {
        }
    }

    private void removeHealthId(Patient patient,PatientIdentifier patientIdentifierPHR,PatientIdentifier patientIdentifierHealthId) {
        try {
            if (patientIdentifierHealthId != null && patientIdentifierPHR != null) {
                patient.removeIdentifier(patientIdentifierPHR);
                patient.removeIdentifier(patientIdentifierHealthId);
                patientService.savePatient(patient);
            }
        } catch (NullPointerException ignored) {
        }
    }

    public List<Patient> getMatchingPatients(String phoneNumber) {
        return existingPatientDao.getPatientsWithPhoneNumber(phoneNumber.substring(phoneNumber.length() - PHONE_NUMBER_LENGTH));
    }

    public List<Patient> getMatchingPatients(String patientName, int patientYearOfBirth, String patientGender) {
        List<PatientResponse> patients = getPatients(patientName, patientYearOfBirth, patientGender);
        List<Patient> existingPatients = new ArrayList<>();
        for (PatientResponse patient : patients) {
            existingPatients.add(patientService.getPatientByUuid(patient.getUuid()));
        }
        return existingPatients;
    }

    private List<PatientResponse> getPatients(String patientName, int patientYearOfBirth, String patientGender) {
        List<PatientResponse> patientsMatchedWithName = filterPatientsByName(patientName);
        if (patientsMatchedWithName.size() != 1) {
            List<PatientResponse> patientsMatchedWithNameAndAge = filterPatientsByAge(patientYearOfBirth, patientsMatchedWithName);
            if (patientsMatchedWithNameAndAge.size() != 1)
                return filterPatientsByGender(patientGender, patientsMatchedWithNameAndAge);
            return patientsMatchedWithNameAndAge;
        }
        return patientsMatchedWithName;
    }

    private List<PatientResponse> filterPatientsByName(String patientName) {
        return patientDao.getPatients("", patientName, null, null, "", 100, 0,
                null, "", null, null, null,
                locationService.getLocation(REGISTRATION_DESK).getUuid(), false, false);
    }


    private List<PatientResponse> filterPatientsByAge(int patientYearOfBirth, List<PatientResponse> patientsMatchedWithNameAndGender) {
        List<PatientResponse> patients = new ArrayList<>();
        for (PatientResponse patient : patientsMatchedWithNameAndGender) {
            if (verifyYearOfBirth(getYearOfBirth(patient.getBirthDate()), patientYearOfBirth)) {
                patients.add(patient);
            }
        }
        return patients;
    }

    private Integer getYearOfBirth(Date birthDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(birthDate);
        return calendar.get(Calendar.YEAR);
    }

    private boolean verifyYearOfBirth(int yearOfBirth, int patientYearOfBirth) {
        return yearOfBirth == patientYearOfBirth || Math.abs(yearOfBirth - patientYearOfBirth) <= MATCHING_CRITERIA_CONSTANT;
    }

    private List<PatientResponse> filterPatientsByGender(String patientGender, List<PatientResponse> patientMatchedWithName) {
        List<PatientResponse> patients = new ArrayList<>();
        for (PatientResponse patient : patientMatchedWithName) {
            if (patient.getGender().equals(patientGender))
                patients.add(patient);
        }
        return patients;
    }

    public List<ExistingPatient> getMatchingPatientDetails(Set<Patient> matchingPatients) {
        List<ExistingPatient> existingPatients = new ArrayList<>();
        for (Patient patient : matchingPatients) {
            if (!isHealthIdVoided(patient.getUuid())) {
                existingPatients.add(
                        new ExistingPatient(
                                patient.getGivenName() + " " + patient.getMiddleName() + " " + patient.getFamilyName(),
                                getYearOfBirth(patient.getBirthdate()).toString(),
                                getAddress(patient),
                                patient.getGender(),
                                patient.getUuid(),
                                getPhoneNumber(patient))
                );
            }
        }
        return existingPatients;
    }

    private String getPhoneNumber(Patient patient) {
        String phoneNumber = " ";
        try {
            phoneNumber = patient.getAttribute(PRIMARY_CONTACT).getValue();
        } catch (NullPointerException ignored) {

        }
        return phoneNumber;
    }

    private String getAddress(Patient patient) {
        if (patient.getPersonAddress() != null) {
            return patient.getPersonAddress().getAddress1() +
                    "," + patient.getPersonAddress().getCountyDistrict() +
                    "," + patient.getPersonAddress().getStateProvince();
        }
        return "";
    }

    public String getPatientWithHealthId(String healthId) {
        return existingPatientDao.getPatientUuidWithHealthId(healthId);
    }

    public boolean isHealthIdVoided(String uuid){
        Patient patient = patientService.getPatientByUuid(uuid);
        Set<PatientIdentifier> patientIdentifiers = patient.getIdentifiers();
        try {
            for (PatientIdentifier patientIdentifier:patientIdentifiers) {
                if(patientIdentifier.getIdentifierType().getName().equals(HEALTH_ID) || patientIdentifier.getIdentifierType().getName().equals(PHR_ADDRESS)){
                   return patientIdentifier.getVoided();
                }
            }
        } catch (NullPointerException ignored) {
        }
        return false;
    }
}
