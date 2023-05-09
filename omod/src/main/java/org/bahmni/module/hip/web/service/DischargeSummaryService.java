package org.bahmni.module.hip.web.service;

import org.bahmni.module.hip.api.dao.DischargeSummaryDao;
import org.bahmni.module.hip.api.dao.HipVisitDao;
import org.bahmni.module.hip.web.model.OpenMrsCondition;
import org.bahmni.module.hip.web.model.DateRange;
import org.bahmni.module.hip.web.model.DischargeSummaryBundle;
import org.bahmni.module.hip.web.model.DrugOrders;
import org.bahmni.module.hip.web.model.OpenMrsDischargeSummary;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.bahmni.module.hip.web.utils.DateUtils.isDateBetweenDateRange;

@Service
public class DischargeSummaryService {

    private final PatientService patientService;
    private final DischargeSummaryDao dischargeSummaryDao;
    private final FhirBundledDischargeSummaryBuilder fhirBundledDischargeSummaryBuilder;
    private final OpenMRSDrugOrderClient openMRSDrugOrderClient;
    private final ConsultationService consultationService;
    private final VisitService visitService;

    @Autowired
    public DischargeSummaryService(PatientService patientService, DischargeSummaryDao dischargeSummaryDao, FhirBundledDischargeSummaryBuilder fhirBundledDischargeSummaryBuilder, OpenMRSDrugOrderClient openMRSDrugOrderClient, ConsultationService consultationService, HipVisitDao hipVisitDao, VisitService visitService) {
        this.patientService = patientService;
        this.dischargeSummaryDao = dischargeSummaryDao;
        this.fhirBundledDischargeSummaryBuilder = fhirBundledDischargeSummaryBuilder;
        this.openMRSDrugOrderClient = openMRSDrugOrderClient;
        this.consultationService = consultationService;
        this.visitService = visitService;
    }

    public List<DischargeSummaryBundle> getDischargeSummaryForVisit(String patientUuid, String visitUuid, Date fromDate, Date toDate) throws ParseException {
        Visit visit = visitService.getVisitByUuid(visitUuid);

        Patient patient = patientService.getPatientByUuid(patientUuid);
        DrugOrders drugOrders = new DrugOrders(openMRSDrugOrderClient.getDrugOrdersByDateAndVisitTypeFor(visit,fromDate,toDate));
        Map<Encounter, DrugOrders> encounteredDrugOrdersMap = drugOrders.groupByEncounter();
        Map<Encounter, List<Obs>> encounterDischargeSummaryMap = getEncounterCarePlanMap(visit,fromDate,toDate);
        ConcurrentHashMap<Encounter, List<OpenMrsCondition>> encounterChiefComplaintsMap = consultationService.getEncounterChiefComplaintsMap(visit,fromDate,toDate);
        Map<Encounter, List<OpenMrsCondition>> encounterMedicalHistoryMap = consultationService.getEncounterMedicalHistoryConditionsMap(visit,fromDate,toDate);
        Map<Encounter, List<Obs>> encounterPhysicalExaminationMap = consultationService.getEncounterPhysicalExaminationMap(visit,fromDate,toDate);
        Map<Encounter, List<Obs>> encounterPatientDocumentsMap = consultationService.getEncounterPatientDocumentsMap(visit,fromDate,toDate);
        Map<Encounter, List<Obs>> encounterProcedureMap = consultationService.getEncounterProcedureMap(visit,fromDate,toDate);
        Map<Encounter, List<Order>> encounterOrdersMap = consultationService.getEncounterOrdersMap(visit,fromDate,toDate);

        List<OpenMrsDischargeSummary> openMrsDischargeSummaryList = OpenMrsDischargeSummary.getOpenMrsDischargeSummaryList(encounterDischargeSummaryMap, encounteredDrugOrdersMap, encounterChiefComplaintsMap, encounterMedicalHistoryMap, encounterPhysicalExaminationMap, encounterPatientDocumentsMap, encounterProcedureMap, encounterOrdersMap, patient);
        return openMrsDischargeSummaryList.stream().map(fhirBundledDischargeSummaryBuilder::fhirBundleResponseFor).collect(Collectors.toList());

    }

    public List<DischargeSummaryBundle> getDischargeSummaryForProgram(String patientUuid, DateRange dateRange, String programName,String programEnrollmentId){
        Date fromDate = dateRange.getFrom();
        Date toDate = dateRange.getTo();
        Patient patient = patientService.getPatientByUuid(patientUuid);
        DrugOrders drugOrders = new DrugOrders(openMRSDrugOrderClient.getDrugOrdersByDateAndProgramFor(patientUuid, dateRange, programName,programEnrollmentId));
        Map<Encounter, DrugOrders> encounteredDrugOrdersMap = drugOrders.groupByEncounter();
        ConcurrentHashMap<Encounter, List<OpenMrsCondition>> encounterChiefComplaintsMap = consultationService.getEncounterChiefComplaintsMapForProgram(programName,fromDate, toDate,patient);
        Map<Encounter, List<Obs>> encounterDischargeSummaryMap = getEncounterCarePlanMapForProgram(programName,fromDate,toDate,patient);
        Map<Encounter, List<OpenMrsCondition>> encounterMedicalHistoryMap = consultationService.getEncounterMedicalHistoryConditionsMapForProgram(programName,fromDate, toDate,patient);
        Map<Encounter, List<Obs>> encounterPhysicalExaminationMap = consultationService.getEncounterPhysicalExaminationMapForProgram(programName,fromDate, toDate,patient);
        Map<Encounter, List<Obs>> encounterPatientDocumentsMap = consultationService.getEncounterPatientDocumentsMapForProgram(programName,fromDate,toDate,patient,programEnrollmentId);
        Map<Encounter, List<Obs>> encounterProcedureMap = consultationService.getEncounterProcedureMapForProgram(programName,fromDate,toDate,patient);
        Map<Encounter, List<Order>> encounterOrdersMap = consultationService.getEncounterOrdersMapForProgram(programName,fromDate,toDate,patient);
        List<OpenMrsDischargeSummary> openMrsDischargeSummaryList = OpenMrsDischargeSummary.getOpenMrsDischargeSummaryList(encounterDischargeSummaryMap, encounteredDrugOrdersMap, encounterChiefComplaintsMap, encounterMedicalHistoryMap, encounterPhysicalExaminationMap, encounterPatientDocumentsMap, encounterProcedureMap, encounterOrdersMap, patient);
        return openMrsDischargeSummaryList.stream().map(fhirBundledDischargeSummaryBuilder::fhirBundleResponseFor).collect(Collectors.toList());
    }


    private Map<Encounter, List<Obs>> getEncounterCarePlanMap(Visit visit, Date fromDate, Date toDate) {
        List<Obs> carePlanObs = dischargeSummaryDao.getCarePlan(visit,fromDate,toDate);
        return getEncounterListMapForCarePlan(carePlanObs);
    }

    private Map<Encounter, List<Obs>> getEncounterListMapForCarePlan(List<Obs> carePlanObs) {
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

    private Map<Encounter, List<Obs>> getEncounterCarePlanMapForProgram(String programName, Date fromDate, Date toDate, Patient patient) {
        List<Obs> carePlanObs = dischargeSummaryDao.getCarePlanForProgram(programName,fromDate, toDate,patient);
        return getEncounterListMapForCarePlan(carePlanObs);
    }


}
