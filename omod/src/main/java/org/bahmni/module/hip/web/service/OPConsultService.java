package org.bahmni.module.hip.web.service;
import org.bahmni.module.hip.api.dao.HipVisitDao;
import org.bahmni.module.hip.api.dao.OPConsultDao;
import org.bahmni.module.hip.web.model.OPConsultBundle;
import org.bahmni.module.hip.web.model.DateRange;
import org.bahmni.module.hip.web.model.OpenMrsCondition;
import org.bahmni.module.hip.web.model.DrugOrders;
import org.bahmni.module.hip.web.model.OpenMrsOPConsult;
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
import java.util.stream.Collectors;

@Service
public class OPConsultService {

    private final FhirBundledOPConsultBuilder fhirBundledOPConsultBuilder;
    private final OPConsultDao opConsultDao;
    private final PatientService patientService;
    private final OpenMRSDrugOrderClient openMRSDrugOrderClient;
    private final ConsultationService consultationService;
    private final VisitService visitService;
    private final AbdmConfig abdmConfig;

    @Autowired
    public OPConsultService(FhirBundledOPConsultBuilder fhirBundledOPConsultBuilder,
                            OPConsultDao opConsultDao,
                            PatientService patientService,
                            OpenMRSDrugOrderClient openMRSDrugOrderClient,
                            ConsultationService consultationService, VisitService visitService, AbdmConfig abdmConfig) {
        this.fhirBundledOPConsultBuilder = fhirBundledOPConsultBuilder;
        this.opConsultDao = opConsultDao;
        this.patientService = patientService;
        this.openMRSDrugOrderClient = openMRSDrugOrderClient;
        this.consultationService = consultationService;
        this.visitService = visitService;
        this.abdmConfig = abdmConfig;
    }

    public List<OPConsultBundle> getOpConsultsForVisit(String patientUuid, String visitUuid, Date fromDate, Date toDate) throws ParseException {
        Visit visit = visitService.getVisitByUuid(visitUuid);


        Patient patient = patientService.getPatientByUuid(patientUuid);
        DrugOrders drugOrders = new DrugOrders(openMRSDrugOrderClient.getDrugOrdersByDateAndVisitTypeFor(visit, fromDate, toDate));
        Map<Encounter, DrugOrders> encounteredDrugOrdersMap = drugOrders.groupByEncounter();
        Map<Encounter, List<OpenMrsCondition>> encounterChiefComplaintsMap = consultationService.getEncounterChiefComplaintsMap(visit, fromDate, toDate);
        Map<Encounter, List<OpenMrsCondition>> encounterMedicalHistoryMap = consultationService.getEncounterMedicalHistoryConditionsMap(visit, fromDate, toDate);
        Map<Encounter, List<Obs>> encounterPhysicalExaminationMap = consultationService.getEncounterPhysicalExaminationMap(visit, fromDate, toDate);
        Map<Encounter, Obs> encounterProcedureMap = getEncounterProcedureMap(visit, fromDate, toDate);
        Map<Encounter, List<Obs>> encounterPatientDocumentsMap = consultationService.getEncounterPatientDocumentsMap(visit, fromDate, toDate);
        Map<Encounter, List<Order>> encounterOrdersMap = consultationService.getEncounterOrdersMap(visit, fromDate, toDate);

        List<OpenMrsOPConsult> openMrsOPConsultList = OpenMrsOPConsult.getOpenMrsOPConsultList(encounterChiefComplaintsMap,
                encounterMedicalHistoryMap, encounterPhysicalExaminationMap, encounteredDrugOrdersMap, encounterProcedureMap,
                encounterPatientDocumentsMap, encounterOrdersMap, patient);

        return openMrsOPConsultList.stream().
                map(fhirBundledOPConsultBuilder::fhirBundleResponseFor).collect(Collectors.toList());


    }

    public List<OPConsultBundle> getOpConsultsForProgram(String patientUuid, DateRange dateRange, String programName,String programEnrollmentId) {
        Date fromDate = dateRange.getFrom();
        Date toDate = dateRange.getTo();
        Patient patient = patientService.getPatientByUuid(patientUuid);

        Map<Encounter, List<OpenMrsCondition>> encounterChiefComplaintsMap = consultationService. getEncounterChiefComplaintsMapForProgram(programName,fromDate,toDate,patient);
        Map<Encounter, List<OpenMrsCondition>> encounterMedicalHistoryMap = consultationService.getEncounterMedicalHistoryConditionsMapForProgram(programName,fromDate,toDate,patient);
        Map<Encounter, List<Obs>> encounterPhysicalExaminationMap = consultationService.getEncounterPhysicalExaminationMapForProgram(programName,fromDate,toDate,patient);
        DrugOrders drugOrders = new DrugOrders(openMRSDrugOrderClient.getDrugOrdersByDateAndProgramFor(patientUuid, dateRange,programName,programEnrollmentId));
        Map<Encounter, DrugOrders> encounteredDrugOrdersMap = drugOrders.groupByEncounter();
        Map<Encounter, Obs> encounterProcedureMap = getEncounterProcedureMapForProgram(programName,fromDate,toDate,patient);
        Map<Encounter, List<Obs>> encounterPatientDocumentsMap = consultationService.getEncounterPatientDocumentsMapForProgram(programName,fromDate,toDate,patient,programEnrollmentId);
        Map<Encounter, List<Order>> encounterOrdersMap = consultationService.getEncounterOrdersMapForProgram(programName,fromDate,toDate,patient);

        List<OpenMrsOPConsult> openMrsOPConsultList = OpenMrsOPConsult.getOpenMrsOPConsultList(encounterChiefComplaintsMap,
                encounterMedicalHistoryMap, encounterPhysicalExaminationMap, encounteredDrugOrdersMap, encounterProcedureMap,
                encounterPatientDocumentsMap, encounterOrdersMap, patient);

        return openMrsOPConsultList.stream().
                map(fhirBundledOPConsultBuilder::fhirBundleResponseFor).collect(Collectors.toList());
    }

    private Map<Encounter, Obs> getEncounterProcedureMap(Visit visit, Date fromDate, Date toDate) {
        List<Obs> obsProcedures = opConsultDao.getProcedures(visit,fromDate,toDate);
        Map<Encounter, Obs> encounterProcedureMap = new HashMap<>();
        for(Obs o: obsProcedures){
            encounterProcedureMap.put(o.getEncounter(), o);
        }
        return encounterProcedureMap;
    }

    private Map<Encounter, Obs> getEncounterProcedureMapForProgram(String programName, Date fromDate, Date toDate, Patient patient) {
        List<Obs> obsProcedures = opConsultDao.getProceduresForProgram(programName,fromDate, toDate,patient);
        Map<Encounter, Obs> encounterProcedureMap = new HashMap<>();
        for(Obs o: obsProcedures){
            encounterProcedureMap.put(o.getEncounter(), o);
        }
        return encounterProcedureMap;
    }
}
