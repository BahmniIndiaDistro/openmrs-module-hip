package org.bahmni.module.hip.web.service;

import org.bahmni.module.hip.api.dao.DischargeSummaryDao;
import org.bahmni.module.hip.web.model.DateRange;
import org.bahmni.module.hip.web.model.DischargeSummaryBundle;
import org.bahmni.module.hip.web.model.OpenMrsDischargeSummary;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Date;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DischargeSummaryService {

    private final PatientService patientService;
    private final DischargeSummaryDao dischargeSummaryDao;
    public static Set<String> conceptNames = new HashSet<>(Arrays.asList("Follow up Date", "Additional Advice on Discharge", "Discharge Summary, Plan for follow up"));
    private final FhirBundledDischargeSummaryBuilder fhirBundledDischargeSummaryBuilder;

    @Autowired
    public DischargeSummaryService(PatientService patientService, DischargeSummaryDao dischargeSummaryDao, FhirBundledDischargeSummaryBuilder fhirBundledDischargeSummaryBuilder) {
        this.patientService = patientService;
        this.dischargeSummaryDao = dischargeSummaryDao;
        this.fhirBundledDischargeSummaryBuilder = fhirBundledDischargeSummaryBuilder;
    }

    public List<DischargeSummaryBundle> getDischargeSummaryForVisit(String patientUuid, DateRange dateRange, String visitType) {
        Date fromDate = dateRange.getFrom();
        Date toDate = dateRange.getTo();
        Patient patient = patientService.getPatientByUuid(patientUuid);
        Map<Encounter, List<Obs>> encounterDischargeSummaryMap = getEncounterDischargeSummaryMap(patient, visitType, fromDate, toDate);
        List<OpenMrsDischargeSummary> openMrsDischargeSummaryList = OpenMrsDischargeSummary.getOpenMrsDischargeSummaryList(encounterDischargeSummaryMap, patient);
        return openMrsDischargeSummaryList.stream().map(fhirBundledDischargeSummaryBuilder::fhirBundleResponseFor).collect(Collectors.toList());
    }


    private Map<Encounter, List<Obs>> getEncounterDischargeSummaryMap(Patient patient, String visitType, Date fromDate, Date toDate) {
        List<Obs> carePlanObs = dischargeSummaryDao.getCarePlan(patient, visitType, fromDate, toDate);
        Map<Encounter, List<Obs>> encounterCarePlanMap = new HashMap<>();
        for(Obs obs : carePlanObs){
            Encounter encounter = obs.getEncounter();
            if(!encounterCarePlanMap.containsKey(encounter)){
                encounterCarePlanMap.put(encounter, new ArrayList<>());
            }
            encounterCarePlanMap.get(encounter).add(obs);
        }
        return encounterCarePlanMap;
    }
}
