package org.bahmni.module.hip.web.service;

import org.apache.commons.lang.StringUtils;
import org.bahmni.module.hip.api.dao.PatientDao;
import org.bahmni.module.hip.web.model.ExistingPatient;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Service
public class ExistingPatientService {
    private final PatientService patientService;
    private final PatientDao patientDao;
    static final int MATCHING_CRITERIA_CONSTANT = 2;

    @Autowired
    public ExistingPatientService(PatientService patientService, PatientDao patientDao) {
        this.patientService = patientService;
        this.patientDao = patientDao;
    }

    public List<Patient> getMatchingPatients(String patientName, int patientYearOfBirth,
                                             String patientGender, String phoneNumber) {
        List<Patient> existingPatients = getPatientsByPhoneNumber(phoneNumber);
        if (existingPatients.size() == 1) {
            return existingPatients;
        }
        return getPatients(patientName, patientYearOfBirth, patientGender);
    }

    private List<Patient> getPatientsByPhoneNumber(String phoneNumber) {
        List<Patient> existingPatients = new ArrayList<>();
        List<Patient> patients = patientService.getAllPatients();
        for (Patient patient : patients) {
            Integer patientId = patient.getId();
            String patientPhoneNumber = patientDao.getPhoneNumber(patientId);
            if (patientPhoneNumber != null) {
                patientPhoneNumber = patientPhoneNumber.replace("+91-", "");
                if (phoneNumber.equals(patientPhoneNumber)) {
                    existingPatients.add(patient);
                }
            }
        }
        return existingPatients;
    }

    private List<Patient> getPatients(String patientName, int patientYearOfBirth, String patientGender) {
        List<Patient> patientsMatchedWithName = filterPatientsByName(patientName);
        if (patientsMatchedWithName.size() != 1) {
            List<Patient> patientsMatchedWithNameAndAge = filterPatientsByAge(patientYearOfBirth, patientsMatchedWithName);
            if (patientsMatchedWithNameAndAge.size() != 1)
                return filterPatientsByGender(patientGender, patientsMatchedWithNameAndAge);
            return patientsMatchedWithNameAndAge;
        }
        return patientsMatchedWithName;
    }

    private List<Patient> filterPatientsByName(String patientName) {
        List<Patient> existingPatients = patientService.getAllPatients();
        List<Patient> patients = new ArrayList<>();
        for (Patient patient : existingPatients) {
            String givenName = patientName.split(" ")[0].toLowerCase();
            String familyName = patientName.split(" ")[1].toLowerCase();
            int distanceOfGivenName = StringUtils.getLevenshteinDistance(givenName, patient.getGivenName().toLowerCase());
            int distanceOfFamilyName = StringUtils.getLevenshteinDistance(familyName, patient.getFamilyName().toLowerCase());
            if (distanceOfGivenName <= MATCHING_CRITERIA_CONSTANT
                    && (distanceOfFamilyName <= MATCHING_CRITERIA_CONSTANT
                    || patient.getFamilyName().charAt(0) == patientName.split(" ")[1].charAt(0)))
                patients.add(patient);
        }
        return patients;
    }

    private List<Patient> filterPatientsByAge(int patientYearOfBirth, List<Patient> patientsMatchedWithNameAndGender) {
        List<Patient> patients = new ArrayList<>();
        for (Patient patient : patientsMatchedWithNameAndGender) {
            if (verifyYearOfBirth(getYearOfBirth(patient.getAge()), patientYearOfBirth)) {
                patients.add(patient);
            }
        }
        return patients;
    }

    private Integer getYearOfBirth(int age) {
        return Calendar.getInstance().get(Calendar.YEAR) - age;
    }

    private boolean verifyYearOfBirth(int yearOfBirth, int patientYearOfBirth) {
        return yearOfBirth == patientYearOfBirth || Math.abs(yearOfBirth - patientYearOfBirth) <= MATCHING_CRITERIA_CONSTANT;
    }

    private List<Patient> filterPatientsByGender(String patientGender, List<Patient> patientMatchedWithName) {
        List<Patient> patients = new ArrayList<>();
        for (Patient patient : patientMatchedWithName) {
            if (patient.getGender().equals(patientGender))
                patients.add(patient);
        }
        return patients;
    }

    public ExistingPatient getMatchingPatientDetails(List<Patient> matchingPatients) {
        Patient patient = matchingPatients.get(0);
        ExistingPatient existingPatient = new ExistingPatient(patient.getGivenName() + " " + patient.getFamilyName(),
                getYearOfBirth(patient.getAge()).toString(),
                patient.getPersonAddress().getAddress1() +
                        "," + patient.getPersonAddress().getCountyDistrict() +
                        "," + patient.getPersonAddress().getStateProvince(),
                patient.getGender());

        return existingPatient;
    }
}
