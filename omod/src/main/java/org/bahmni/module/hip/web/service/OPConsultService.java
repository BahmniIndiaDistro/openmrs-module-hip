package org.bahmni.module.hip.web.service;

import org.bahmni.module.hip.api.dao.OPConsultDao;
import org.bahmni.module.hip.web.model.DateRange;
import org.bahmni.module.hip.web.model.OPConsultBundle;
import org.bahmni.module.hip.web.model.OpenMrsCondition;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.openmrs.logic.op.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OPConsultService {
    private final FhirBundledOPConsultBuilder fhirBundledOPConsultBuilder;
    private final OPConsultDao opConsultDao;
    private final PatientService patientService;
    private final EncounterService encounterService;
    private final ObsService obsService;
    private final ConceptService conceptService;

    @Autowired
    public OPConsultService(FhirBundledOPConsultBuilder fhirBundledOPConsultBuilder, OPConsultDao opConsultDao,
                            PatientService patientService, EncounterService encounterService, ObsService obsService,
                            ConceptService conceptService) {
        this.fhirBundledOPConsultBuilder = fhirBundledOPConsultBuilder;
        this.opConsultDao = opConsultDao;
        this.patientService = patientService;
        this.encounterService = encounterService;
        this.obsService = obsService;
        this.conceptService = conceptService;
    }

    public List<OPConsultBundle> getOpConsultsForVisit(String patientUuid, DateRange dateRange, String visitType) {
        Date fromDate = dateRange.getFrom();
        Date toDate = dateRange.getTo();
        Patient patient = patientService.getPatientByUuid(patientUuid);
        List<OPConsultBundle> opConsultBundles = new ArrayList<>();

        List<Integer> obsIdsOfChiefComplaints = opConsultDao.getChiefComplaints(patientUuid, visitType, fromDate, toDate);
        List<Obs> obsOfChiefComplaints = obsIdsOfChiefComplaints.stream().map(obsService::getObs).collect(Collectors.toList());

        List<OpenMrsCondition> openMrsConditionsForChiefComplaints = obsOfChiefComplaints.stream().map(o -> new OpenMrsCondition(o.getEncounter(), o.getUuid(),
                o.getValueCoded().getDisplayString(), patient, o.getEncounter().getEncounterProviders())).collect(Collectors.toList());
        opConsultBundles.addAll(openMrsConditionsForChiefComplaints.stream().
                map(fhirBundledOPConsultBuilder::fhirBundleResponseFor).
                collect(Collectors.toList()));

        List<String[]> medicalHistoryIds =  opConsultDao.getMedicalHistory(patientUuid, visitType, fromDate, toDate);
        List<OpenMrsCondition> openMrsConditionsForMedicalHistory = new ArrayList<>();
        for (Object[] id : medicalHistoryIds) {
            Encounter encounter = encounterService.getEncounter(Integer.parseInt(String.valueOf(id[3])));
            openMrsConditionsForMedicalHistory.add(new OpenMrsCondition(encounter, String.valueOf(id[2]), conceptService.getConcept(Integer.parseInt(String.valueOf(id[1]))).getDisplayString(),
                    patient, encounter.getEncounterProviders()));
        }
        opConsultBundles.addAll(openMrsConditionsForMedicalHistory.stream().
                map(fhirBundledOPConsultBuilder::fhirBundleResponseFor).collect(Collectors.toList()));

        List<Integer> physicalExaminationObsIds = opConsultDao.getPhysicalExamination(patientUuid, visitType, fromDate, toDate);
        List<Obs> physicalExaminationObs = physicalExaminationObsIds.stream().map(obsService::getObs).collect(Collectors.toList());

        opConsultBundles.addAll(physicalExaminationObs.stream().map(fhirBundledOPConsultBuilder::fhirBundleResponseFor).collect(Collectors.toList()));
        return opConsultBundles;
    }

}
