package org.bahmni.module.hip.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.bahmni.module.bahmnicommons.api.contract.patient.PatientSearchParameters;
import org.bahmni.module.bahmnicommons.api.contract.patient.response.PatientResponse;
import org.bahmni.module.bahmnicommons.api.dao.PatientDao;
import org.bahmni.module.bahmnicommons.api.visitlocation.BahmniVisitLocationServiceImpl;
import org.bahmni.module.hip.Config;
import org.bahmni.module.hip.api.dao.ExistingPatientDao;
import org.bahmni.module.hip.model.Status;
import org.bahmni.module.hip.model.ExistingPatient;
import org.bahmni.module.hip.model.PatientAbhaInfo;
import org.bahmni.module.hip.service.ExistingPatientService;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;


@Service
public class ExistingPatientServiceImpl implements ExistingPatientService {
    private final ExistingPatientDao existingPatientDao;
    static final int MATCHING_CRITERIA_CONSTANT = 2;
    private final PatientDao patientDao;
    private final PatientService patientService;
    private final LocationService locationService;
    static final int PHONE_NUMBER_LENGTH = 10;

    @Autowired
    public ExistingPatientServiceImpl(PatientDao patientDao, PatientService patientService, ExistingPatientDao existingPatientDao, LocationService locationService) {
        this.patientDao = patientDao;
        this.patientService = patientService;
        this.existingPatientDao = existingPatientDao;
        this.locationService = locationService;
    }

    @Override
    public Set<Patient> getMatchingPatients(String locationUuid, String phoneNumber, String patientName, int patientYearOfBirth, String patientGender) {
        Set<Patient> matchingPatients = new HashSet<>();
        matchingPatients.addAll(getMatchingPatients(phoneNumber));
        matchingPatients.addAll(getMatchingPatients(locationUuid, patientName, patientYearOfBirth, patientGender));
        matchingPatients.removeIf(patient -> !getHealthId(patient).equals(""));
        return matchingPatients;
    }

    @Override
    public String getHealthId(Patient patient) {
        String healthId = "";
        try {
            healthId = patient.getPatientIdentifier(Config.ABHA_ADDRESS.getValue()).getIdentifier();
        } catch (NullPointerException ignored) {

        }
        return healthId;
    }

    @Override
    public void checkAndAddPatientIdentifier(String patientUuid, PatientAbhaInfo abhaInfo) {
        Patient patient = patientService.getPatientByUuid(patientUuid);
        try {
            if (patient.getPatientIdentifier(Config.ABHA_ADDRESS.getValue()) == null && patient.getPatientIdentifier(Config.ABHA_NUMBER.getValue()) == null) {
                setIdentifier(patient, abhaInfo.getAbhaAddress(), Config.ABHA_ADDRESS.getValue());
                setIdentifier(patient, abhaInfo.getAbhaNumber(), Config.ABHA_NUMBER.getValue());
            }
        } catch (NullPointerException ignored) {
        }
    }

    private void setIdentifier(Patient patient, String identifierValue, String identifierType) {
        PatientIdentifier identifier = new PatientIdentifier();

        identifier.setPatient(patient);
        identifier.setIdentifier(identifierValue);
        identifier.setIdentifierType(patientService.getPatientIdentifierTypeByName(identifierType));

        patientService.savePatientIdentifier(identifier);
    }


    @Override
    public void perform(String healthId, String action) {
        Patient patient = patientService.getPatientByUuid(getPatientWithHealthId(healthId));
        PatientIdentifier patientIdentifierPhr = patient.getPatientIdentifier(Config.ABHA_ADDRESS.getValue());
        PatientIdentifier patientIdentifierHealthId = patient.getPatientIdentifier(Config.ABHA_NUMBER.getValue());
        if (action.equals(Status.DELETED.toString())) {
            removeHealthId(patient, patientIdentifierPhr, patientIdentifierHealthId);
        }
        if (action.equals(Status.DEACTIVATED.toString())) {
            voidHealthId(patientIdentifierPhr, patientIdentifierHealthId);
        }
        if (action.equals(Status.REACTIVATED.toString())) {
            unVoidHealthId(patient);
        }
    }

    private void voidHealthId(PatientIdentifier patientIdentifierPHR, PatientIdentifier patientIdentifierHealthId) {
        try {
            if (!patientIdentifierPHR.getVoided()) {
                patientService.voidPatientIdentifier(patientIdentifierPHR, Status.DEACTIVATED.toString());
            }
            if (!patientIdentifierHealthId.getVoided()) {
                patientService.voidPatientIdentifier(patientIdentifierHealthId, Status.DEACTIVATED.toString());
            }
        } catch (NullPointerException ignored) {
        }
    }

    private void unVoidHealthId(Patient patient) {
        Set<PatientIdentifier> patientIdentifiers = patient.getIdentifiers();
        try {
            for (PatientIdentifier patientIdentifier : patientIdentifiers) {
                if (patientIdentifier.getIdentifierType().getName().equals(Config.ABHA_ADDRESS.getValue()) ||
                        patientIdentifier.getIdentifierType().getName().equals(Config.ABHA_NUMBER.getValue())) {
                    if (patientIdentifier.getVoided()) {
                        patientIdentifier.setVoided(false);
                        patientService.savePatientIdentifier(patientIdentifier);
                    }
                }
            }
        } catch (NullPointerException ignored) {
        }
    }

    private void removeHealthId(Patient patient, PatientIdentifier patientIdentifierPHR, PatientIdentifier patientIdentifierHealthId) {
        try {
            if (patientIdentifierPHR != null)
                patient.removeIdentifier(patientIdentifierPHR);
            if (patientIdentifierHealthId != null)
                patient.removeIdentifier(patientIdentifierHealthId);
            patientService.savePatient(patient);
        } catch (NullPointerException ignored) {
        }
    }

    @Override
    public List<Patient> getMatchingPatients(String phoneNumber) {
        if (!phoneNumber.equals("undefined"))
            return existingPatientDao.getPatientsWithPhoneNumber(phoneNumber.substring(phoneNumber.length() - PHONE_NUMBER_LENGTH));
        return new ArrayList<>();
    }

    @Override
    public List<Patient> getMatchingPatients(String locationUuid, String patientName, int patientYearOfBirth, String patientGender) {
        List<PatientResponse> patients = getPatients(locationUuid, patientName, patientYearOfBirth, patientGender);
        List<Patient> existingPatients = new ArrayList<>();
        for (PatientResponse patient : patients) {
            existingPatients.add(patientService.getPatientByUuid(patient.getUuid()));
        }
        return existingPatients;
    }

    private List<PatientResponse> getPatients(String locationUuid, String patientName, int patientYearOfBirth, String patientGender) {
        List<PatientResponse> patientsMatchedWithName = filterPatientsByName(locationUuid, patientName);
        if (patientsMatchedWithName.size() != 1) {
            List<PatientResponse> patientsMatchedWithNameAndAge = filterPatientsByAge(patientYearOfBirth, patientsMatchedWithName);
            if (patientsMatchedWithNameAndAge.size() != 1)
                return filterPatientsByGender(patientGender, patientsMatchedWithNameAndAge);
            return patientsMatchedWithNameAndAge;
        }
        return patientsMatchedWithName;
    }

    private List<PatientResponse> filterPatientsByName(String locationUuid, String patientName) {
        List<PatientResponse> patientResponseList = new ArrayList<>();
        Supplier<Location> visitLocation = () -> getVisitLocation(locationUuid);
        Supplier<List<String>> configuredAddressFields = () -> patientDao.getConfiguredPatientAddressFields();

        String[] nameParts = patientName.split(" ");

        for (String part : nameParts) {
            patientResponseList.addAll(patientDao.getPatients(getPatientSearchParameters(locationUuid, part), visitLocation, configuredAddressFields));
        }
        return patientResponseList;
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

    @Override
    public List<ExistingPatient> getMatchingPatientDetails(Set<Patient> matchingPatients) {
        List<ExistingPatient> existingPatients = new ArrayList<>();
        for (Patient patient : matchingPatients) {
            if (!isHealthIdVoided(patient.getUuid())) {
                existingPatients.add(
                        new ExistingPatient(
                                patient.getPatientIdentifier().getIdentifier(),
                                patient.getGivenName() + " " + patient.getMiddleName() + " " + patient.getFamilyName(),
                                patient.getBirthdate().toString(),
                                getAddress(patient),
                                patient.getGender(),
                                patient.getUuid(),
                                existingPatientDao.getPhoneNumber(patient))
                );
            }
        }
        return existingPatients;
    }

    private String getAddress(Patient patient) {
        if (patient.getPersonAddress() != null) {
            return patient.getPersonAddress().getAddress1() +
                    "," + patient.getPersonAddress().getCountyDistrict() +
                    "," + patient.getPersonAddress().getStateProvince();
        }
        return "";
    }

    @Override
    public String getPatientWithHealthId(String healthId) {
        return existingPatientDao.getPatientUuidWithHealthId(healthId);
    }

    @Override
    public boolean isHealthIdVoided(String uuid) {
        Patient patient = patientService.getPatientByUuid(uuid);
        Set<PatientIdentifier> patientIdentifiers = patient.getIdentifiers();
        try {
            for (PatientIdentifier patientIdentifier : patientIdentifiers) {
                if (patientIdentifier.getIdentifierType().getName().equals(Config.ABHA_ADDRESS.getValue())) {
                    return patientIdentifier.getVoided();
                }
            }
        } catch (NullPointerException ignored) {
        }
        return false;
    }

    @Override
    public boolean isHealthNumberPresent(String patientUuid) {
        Patient patient = patientService.getPatientByUuid(patientUuid);
        Set<PatientIdentifier> patientIdentifiers = patient.getIdentifiers();
        try {
            for (PatientIdentifier patientIdentifier : patientIdentifiers) {
                if (patientIdentifier.getIdentifierType().getName().equals(Config.ABHA_NUMBER.getValue()) && !patientIdentifier.getVoided()) {
                    return true;
                }
            }
        } catch (NullPointerException ignored) {
        }
        return false;
    }

    @Override
    public ExistingPatient getExistingPatientWithUuid(String patientUuid) {
        Patient patient = patientService.getPatientByUuid(patientUuid);
        if (patient != null && !isHealthIdVoided(patientUuid)) {
            return new ExistingPatient(
                    patient.getPatientIdentifier().getIdentifier(),
                    patient.getGivenName() + " " + patient.getMiddleName() + " " + patient.getFamilyName(),
                    patient.getBirthdate().toString(),
                    getAddress(patient),
                    patient.getGender(),
                    patient.getUuid(),
                    existingPatientDao.getPhoneNumber(patient));
        }
        return null;
    }

    private PatientSearchParameters getPatientSearchParameters(String locationUuid, String patientName) {
        PatientSearchParameters searchParameters = new PatientSearchParameters();
        searchParameters.setIdentifier("");
        searchParameters.setName(patientName);
        searchParameters.setCustomAttribute(null);

        searchParameters.setAddressFieldName(null);
        searchParameters.setAddressFieldValue("");
        searchParameters.setLength(100);
        searchParameters.setStart(0);

        searchParameters.setPatientAttributes(null);
        searchParameters.setProgramAttributeFieldName("");
        searchParameters.setProgramAttributeFieldValue(null);
        searchParameters.setAddressSearchResultFields(null);
        searchParameters.setPatientSearchResultFields(null);

        searchParameters.setLoginLocationUuid(locationUuid);
        searchParameters.setFilterPatientsByLocation(false);
        searchParameters.setFilterOnAllIdentifiers(false);
        return searchParameters;
    }

    private Location getVisitLocation(String loginLocationUuid) {
        if (StringUtils.isBlank(loginLocationUuid)) {
            return null;
        }
        BahmniVisitLocationServiceImpl bahmniVisitLocationService = new BahmniVisitLocationServiceImpl(Context.getLocationService());
        return bahmniVisitLocationService.getVisitLocation(loginLocationUuid);
    }
}
