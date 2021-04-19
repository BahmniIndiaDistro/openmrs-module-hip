package org.bahmni.module.hip.web.service;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExistingPatientService {

    private List<Patient> existingPatients;
    private final PatientService patientService;
    private Map<Patient, Boolean> patientMap;

    @Autowired
    public ExistingPatientService(PatientService patientService) {
        this.patientService = patientService;
    }

    public List<Patient> filterMatchingPatients(String patientName, int patientYearOfBirth,
                                                String patientGender) {

        existingPatients = patientService.getAllPatients();
        patientMap = new HashMap<>();
        filter(patientName, patientYearOfBirth, patientGender);
        List<Patient> matchingPatients = new ArrayList<>();
        for (Map.Entry<Patient, Boolean> entry : patientMap.entrySet()) {
            if (entry.getValue()) {
                matchingPatients.add(entry.getKey());
            }
        }
        return matchingPatients;
    }

    private void filter(String patientName, int patientYearOfBirth, String patientGender) {
        for (Patient patient : existingPatients) {
            patientMap.put(patient, true);
        }

        filterPatientsByGender(patientGender);
        filterPatientsByName(patientName);
        filterPatientsByAge(patientYearOfBirth);
    }

    private void filterPatientsByAge(int patientYearOfBirth) {
        for (Patient patient : existingPatients) {
            int yearOfBirth = Calendar.getInstance().get(Calendar.YEAR) - patient.getAge();
            if (!verifyYearOfBirth(yearOfBirth, patientYearOfBirth)) {
                patientMap.put(patient, false);
            }
        }
    }

    private boolean verifyYearOfBirth(int yearOfBirth, int patientYearOfBirth) {
        return yearOfBirth == patientYearOfBirth || Math.abs(yearOfBirth - patientYearOfBirth) <= 2;
    }

    private void filterPatientsByName(String patientName) {
        for (Patient patient : existingPatients) {
            int distance = StringUtils.getLevenshteinDistance(patientName.toLowerCase(), patient.getPersonName().
                    toString().toLowerCase());
            if (distance > 2)
                patientMap.put(patient, false);
        }
    }

    private void filterPatientsByGender(String patientGender) {
        for (Patient patient : existingPatients) {
            if (!patient.getGender().equals(patientGender))
                patientMap.put(patient, false);
        }
    }

    public JSONObject getMatchingPatientDetails(List<Patient> matchingPatients) {
        JSONObject existingPatientsListObject = new JSONObject();
        existingPatientsListObject.put("PatientName:", matchingPatients.get(0).getGivenName() + " " +
                matchingPatients.get(0).getFamilyName());
        existingPatientsListObject.put("PatientAge:", matchingPatients.get(0).getAge());
        existingPatientsListObject.put("PatientGender:", matchingPatients.get(0).getGender());
        existingPatientsListObject.put("PatientAddress:", matchingPatients.get(0).getPersonAddress().getCountyDistrict() +
                "," + matchingPatients.get(0).getPersonAddress().getStateProvince());

        return existingPatientsListObject;
    }
}
