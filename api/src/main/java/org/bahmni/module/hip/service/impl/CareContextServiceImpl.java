package org.bahmni.module.hip.service.impl;

import org.bahmni.module.hip.api.dao.CareContextRepository;
import org.bahmni.module.hip.api.dao.ExistingPatientDao;
import org.bahmni.module.hip.model.PatientCareContext;
import org.bahmni.module.hip.serializers.NewCareContext;
import org.bahmni.module.hip.service.CareContextService;
import org.bahmni.module.hip.service.ValidationService;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CareContextServiceImpl implements CareContextService {
    private final CareContextRepository careContextRepository;
    private final PatientService patientService;
    private final ValidationService validationService;
    private final ExistingPatientDao existingPatientDao;

    @Autowired
    public CareContextServiceImpl(CareContextRepository careContextRepository, PatientService patientService, ValidationService validationService, ExistingPatientDao existingPatientDao) {
        this.careContextRepository = careContextRepository;
        this.patientService = patientService;
        this.validationService = validationService;
        this.existingPatientDao = existingPatientDao;
    }

    @Override
    public <Type> Type careContextForPatient(String patientUuid) {
        return (Type) careContextRepository.getPatientCareContext(patientUuid);
    }

    @Override
    public NewCareContext newCareContextsForPatient(String patientUuid) {
        Patient patient = patientService.getPatientByUuid(patientUuid);
        return createNewCareContext(patient,getCareContexts(patient));
    }

    @Override
    public NewCareContext newCareContextsForPatientByVisitUuid(String patientUuid, String visitUuid) {
        Patient patient = patientService.getPatientByUuid(patientUuid);
        return createNewCareContext(patient,careContextRepository.getPatientCareContextByVisitUuid(visitUuid));
    }

    private NewCareContext createNewCareContext(Patient patient, List<PatientCareContext> careContexts){
        List<String> name = Arrays.asList(patient.getGivenName(),patient.getMiddleName(),patient.getFamilyName())
                .stream().filter(Objects::nonNull).collect(Collectors.toList());
        return new NewCareContext(String.join(" ", name),
                existingPatientDao.getPatientHealthIdWithPatient(patient),
                patient.getPatientIdentifier("Patient Identifier").getIdentifier(),
                existingPatientDao.getPhoneNumber(patient),
                careContexts);
    }

    private List<PatientCareContext> getCareContexts(Patient patient) {
        List<PatientCareContext> patientCareContexts = careContextRepository.getNewPatientCareContext(patient);
        if (patientCareContexts.size() > 1) {
            List<PatientCareContext> result = new ArrayList<>();
            for (PatientCareContext careContext : patientCareContexts) {
                if (!validationService.isValidVisit(careContext.getCareContextName())) result.add(careContext);
            }
            return result;
        }
        return patientCareContexts;
    }
}
