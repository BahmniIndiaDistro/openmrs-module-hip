package org.bahmni.module.hip.web.service;

import org.bahmni.module.hip.api.dao.ConsultationDao;
import org.bahmni.module.hip.api.dao.DischargeSummaryDao;
import org.bahmni.module.hip.web.model.*;
import org.bahmni.module.hip.web.model.DateRange;
import org.bahmni.module.hip.web.model.DischargeSummaryBundle;
import org.bahmni.module.hip.web.model.DrugOrders;
import org.bahmni.module.hip.web.model.OpenMrsDischargeSummary;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private final FhirBundledDischargeSummaryBuilder fhirBundledDischargeSummaryBuilder;
    private final OpenMRSDrugOrderClient openMRSDrugOrderClient;
    private final ConsultationDao consultationDao;

    @Autowired
    public DischargeSummaryService(PatientService patientService, DischargeSummaryDao dischargeSummaryDao, FhirBundledDischargeSummaryBuilder fhirBundledDischargeSummaryBuilder, OpenMRSDrugOrderClient openMRSDrugOrderClient, ConsultationDao consultationDao) {
        this.patientService = patientService;
        this.dischargeSummaryDao = dischargeSummaryDao;
        this.fhirBundledDischargeSummaryBuilder = fhirBundledDischargeSummaryBuilder;
        this.openMRSDrugOrderClient = openMRSDrugOrderClient;
        this.consultationDao = consultationDao;
    }

    public List<DischargeSummaryBundle> getDischargeSummaryForVisit(String patientUuid, DateRange dateRange, String visitType) {
        Date fromDate = dateRange.getFrom();
        Date toDate = dateRange.getTo();
        Patient patient = patientService.getPatientByUuid(patientUuid);
        Map<Encounter, List<Obs>> encounterDischargeSummaryMap = getEncounterDischargeSummaryMap(patient, visitType, fromDate, toDate);
        DrugOrders drugOrders = new DrugOrders(openMRSDrugOrderClient.getDrugOrdersByDateAndVisitTypeFor(patientUuid, dateRange, visitType));
        Map<Encounter, DrugOrders> encounteredDrugOrdersMap = drugOrders.groupByEncounter();
        Map<Encounter, List<OpenMrsCondition>> encounterChiefComplaintsMap = getEncounterChiefComplaintsMap(patient, visitType, fromDate, toDate);

        List<OpenMrsDischargeSummary> openMrsDischargeSummaryList = OpenMrsDischargeSummary.getOpenMrsDischargeSummaryList(encounterDischargeSummaryMap, encounteredDrugOrdersMap, encounterChiefComplaintsMap, patient);
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

    private Map<Encounter, List<OpenMrsCondition>> getEncounterChiefComplaintsMap(Patient patient, String visitType, Date fromDate, Date toDate) {
        List<Obs> chiefComplaints = consultationDao.getChiefComplaints(patient, visitType, fromDate, toDate);
        HashMap<Encounter, List<OpenMrsCondition>> encounterChiefComplaintsMap = new HashMap<>();

        for (Obs o : chiefComplaints) {
            if(!encounterChiefComplaintsMap.containsKey(o.getEncounter())){
                encounterChiefComplaintsMap.put(o.getEncounter(), new ArrayList<>());
            }
            encounterChiefComplaintsMap.get(o.getEncounter()).add(new OpenMrsCondition(o.getUuid(), o.getValueCoded().getDisplayString(), o.getDateCreated()));
        }
        return encounterChiefComplaintsMap;
    }
}
