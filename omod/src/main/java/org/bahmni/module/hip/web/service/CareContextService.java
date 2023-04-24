package org.bahmni.module.hip.web.service;

import org.bahmni.module.hip.Config;
import org.bahmni.module.hip.api.dao.CareContextRepository;
import org.bahmni.module.hip.api.dao.ExistingPatientDao;
import org.bahmni.module.hip.model.PatientCareContext;
import org.bahmni.module.hip.web.model.CareContext;
import org.bahmni.module.hip.web.model.serializers.NewCareContext;
import org.openmrs.Encounter;
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
public class CareContextService {
    private final CareContextRepository careContextRepository;
    private final PatientService patientService;
    private final ValidationService validationService;
    private final ExistingPatientDao existingPatientDao;

    @Autowired
    public CareContextService(CareContextRepository careContextRepository, PatientService patientService, ValidationService validationService, ExistingPatientDao existingPatientDao) {
        this.careContextRepository = careContextRepository;
        this.patientService = patientService;
        this.validationService = validationService;
        this.existingPatientDao = existingPatientDao;
    }

    CareContext careContextFor(Encounter emrEncounter, Class careContextType) {
        if (careContextType.getName().equals("Visit")) {
            return CareContext.builder()
                    .careContextReference(emrEncounter.getVisit().getUuid())
                    .careContextType("Visit").build();
        } else {
            return CareContext.builder()
                    .careContextReference(emrEncounter.getVisit().getVisitType().getName())
                    .careContextType("VisitType").build();
        }
    }

    public <Type> Type careContextForPatient(String patientUuid) {
        return (Type) careContextRepository.getPatientCareContext(patientUuid);
    }

    public NewCareContext newCareContextsForPatient(String patientUuid) {
        Patient patient = patientService.getPatientByUuid(patientUuid);
        return createNewCareContext(patient,getCareContexts(patient));
    }

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
