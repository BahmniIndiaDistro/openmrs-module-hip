package org.bahmni.module.hip.web.service;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Service
public class ExistingPatientService {

    private List<Patient> existingPatients;
    private final PatientService patientService;

    @Autowired
    public ExistingPatientService(PatientService patientService) {
        this.patientService = patientService;
    }

    public List<Patient> filterMatchingPatients(String patientName, int patientYearOfBirth,
                                                String patientGender) {

        existingPatients = patientService.getAllPatients();
        return filter(patientName, patientYearOfBirth, patientGender);
    }

    private List<Patient> filter(String patientName, int patientYearOfBirth, String patientGender) {
        List<Patient> patientsMatchedWithName = filterPatientsByName(patientName);
        if(patientsMatchedWithName.size() != 1) {
            List<Patient> patientsMatchedWithNameAndAge = filterPatientsByAge(patientYearOfBirth, patientsMatchedWithName);
            if(patientsMatchedWithNameAndAge.size() != 1)
                return filterPatientsByGender(patientGender, patientsMatchedWithNameAndAge);
            return patientsMatchedWithNameAndAge;
        }
        return patientsMatchedWithName;
    }

    private List<Patient> filterPatientsByAge(int patientYearOfBirth, List<Patient> patientsMatchedWithNameAndGender) {
        List<Patient> patients = new ArrayList<>();
        for (Patient patient : patientsMatchedWithNameAndGender) {
            int yearOfBirth = Calendar.getInstance().get(Calendar.YEAR) - patient.getAge();
            if (verifyYearOfBirth(yearOfBirth, patientYearOfBirth)) {
                patients.add(patient);
            }
        }
        return patients;
    }

    private boolean verifyYearOfBirth(int yearOfBirth, int patientYearOfBirth) {
        return yearOfBirth == patientYearOfBirth || Math.abs(yearOfBirth - patientYearOfBirth) <= 2;
    }

    private List<Patient> filterPatientsByName(String patientName) {
        List<Patient> patients = new ArrayList<>();
        for (Patient patient : existingPatients) {
            int distance = StringUtils.getLevenshteinDistance(patientName.toLowerCase(), patient.getPersonName().
                    toString().toLowerCase());
            if (distance <= 2)
                patients.add(patient);
        }
        return patients;
    }

    private List<Patient> filterPatientsByGender(String patientGender, List<Patient> patientMatchedWithName) {
        List<Patient> patients = new ArrayList<>();
        for (Patient patient : patientMatchedWithName) {
            if (patient.getGender().equals(patientGender))
                patients.add(patient);
        }
        return patients;
    }

    public JSONObject getMatchingPatientDetails(List<Patient> matchingPatients) {
        JSONObject existingPatientsListObject = new JSONObject();
        existingPatientsListObject.put("name", matchingPatients.get(0).getGivenName() + " " +
                matchingPatients.get(0).getFamilyName());
        existingPatientsListObject.put("age", matchingPatients.get(0).getAge());
        existingPatientsListObject.put("gender", matchingPatients.get(0).getGender());
        existingPatientsListObject.put("address", matchingPatients.get(0).getPersonAddress().getCountyDistrict() +
                "," + matchingPatients.get(0).getPersonAddress().getStateProvince());

        return existingPatientsListObject;
    }
}
