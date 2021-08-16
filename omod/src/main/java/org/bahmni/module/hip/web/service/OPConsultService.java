package org.bahmni.module.hip.web.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bahmni.module.hip.api.dao.OPConsultDao;
import org.bahmni.module.hip.web.model.*;
import org.openmrs.*;
import org.openmrs.api.*;
import org.openmrs.parameter.EncounterSearchCriteria;
import org.openmrs.parameter.EncounterSearchCriteriaBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OPConsultService {
    protected static final Log log = LogFactory.getLog(OPConsultService.class);
    public static Set<String> conceptNames = new HashSet<>(Arrays.asList("Treatment Plan","Next Followup Visit","Plan for next visit","Patient Category","Current Followup Visit After",
            "Plan for next visit","Parents name","Death Date","Contact number","Vitamin A Capsules Provided","Albendazole Given","Referred out",
            "Vitamin A Capsules Provided","Albendazole Given","Bal Vita provided","Bal Vita Provided by FCHV","Condoms given","Marital Status","Contact Number",
            "Transferred out (Complete Section)"));

    private final FhirBundledOPConsultBuilder fhirBundledOPConsultBuilder;
    private final OPConsultDao opConsultDao;
    private final PatientService patientService;
    private final EncounterService encounterService;
    private final ObsService obsService;
    private final ConceptService conceptService;
    private final OpenMRSDrugOrderClient openMRSDrugOrderClient;
    private final DiagnosticReportService diagnosticReportService;

    @Autowired
    public OPConsultService(FhirBundledOPConsultBuilder fhirBundledOPConsultBuilder, OPConsultDao opConsultDao,
                            PatientService patientService, EncounterService encounterService, ObsService obsService,
                            ConceptService conceptService, OpenMRSDrugOrderClient openMRSDrugOrderClient,
                            DiagnosticReportService diagnosticReportService) {
        this.fhirBundledOPConsultBuilder = fhirBundledOPConsultBuilder;
        this.opConsultDao = opConsultDao;
        this.patientService = patientService;
        this.encounterService = encounterService;
        this.obsService = obsService;
        this.conceptService = conceptService;
        this.openMRSDrugOrderClient = openMRSDrugOrderClient;
        this.diagnosticReportService = diagnosticReportService;
    }

    public List<OPConsultBundle> getOpConsultsForVisit(String patientUuid, DateRange dateRange, String visitType) throws ParseException {
        Date fromDate = dateRange.getFrom();
        Date toDate = dateRange.getTo();
        Patient patient = patientService.getPatientByUuid(patientUuid);

        Map<Encounter, List<OpenMrsCondition>> encounterChiefComplaintsMap = getEncounterChiefComplaintsMap(patient, visitType, fromDate, toDate);
        Map<Encounter, List<OpenMrsCondition>> encounterMedicalHistoryMap = getEncounterMedicalHistoryMap(patient, visitType, fromDate, toDate);
        Map<Encounter, List<Obs>> encounterPhysicalExaminationMap = getEncounterPhysicalExaminationMap(patient, visitType, fromDate, toDate);
        DrugOrders drugOrders = new DrugOrders(openMRSDrugOrderClient.getDrugOrdersByDateAndVisitTypeFor(patientUuid, dateRange, visitType));
        Map<Encounter, DrugOrders> encounteredDrugOrdersMap = drugOrders.groupByEncounter();
        Map<Encounter, Obs> encounterProcedureMap = getEncounterProcedureMap(patient, visitType, fromDate, toDate);
        Map<Encounter, List<Obs>> encounterPatientDocumentsMap = getEncounterPatientDocumentsMap(visitType, fromDate, toDate, patient);

        List<OpenMrsOPConsult> openMrsOPConsultList = OpenMrsOPConsult.getOpenMrsOPConsultList(encounterChiefComplaintsMap,
                encounterMedicalHistoryMap, encounterPhysicalExaminationMap, encounteredDrugOrdersMap, encounterProcedureMap,
                encounterPatientDocumentsMap, patient);
        List<OPConsultBundle> opConsultBundles = openMrsOPConsultList.stream().
                map(fhirBundledOPConsultBuilder::fhirBundleResponseFor).collect(Collectors.toList());

        return opConsultBundles;
    }

    private Map<Encounter, List<Obs>> getEncounterPatientDocumentsMap(String visitType, Date fromDate, Date toDate, Patient patient) {
        final int patientDocumentEncounterType = 9;
        Map<Encounter, List<Obs>> encounterDiagnosticReportsMap = diagnosticReportService.getAllObservationsForVisits(fromDate, toDate, patient, visitType);
        Map<Encounter, List<Obs>> encounterPatientDocumentsMap = new HashMap<>();
        for (Encounter e : encounterDiagnosticReportsMap.keySet()) {
            List<Obs> patientDocuments = e.getAllObs().stream().
                    filter(o -> (o.getEncounter().getEncounterType().getEncounterTypeId() == patientDocumentEncounterType && o.getValueText() == null))
                    .collect(Collectors.toList());
            if (patientDocuments.size() > 0) {
                encounterPatientDocumentsMap.put(e, patientDocuments);
            }
        }
        return encounterPatientDocumentsMap;
    }

    private Map<Encounter, Obs> getEncounterProcedureMap(Patient patient, String visitType, Date fromDate, Date toDate) {
        List<Obs> obsProcedures = opConsultDao.getProcedures(patient, visitType, fromDate, toDate);
        Map<Encounter, Obs> encounterProcedureMap = new HashMap<>();
        for(Obs o: obsProcedures){
            encounterProcedureMap.put(o.getEncounter(), o);
        }
        return encounterProcedureMap;
    }

    private Map<Encounter, List<Obs>> getEncounterPhysicalExaminationMap(Patient patient, String visitType, Date fromDate, Date toDate) {
        List<Obs> physicalExaminations = opConsultDao.getPhysicalExamination(patient, visitType, fromDate, toDate);
        Map<Encounter, List<Obs>> encounterPhysicalExaminationMap = new HashMap<>();
        for (Obs physicalExamination : physicalExaminations) {
            Encounter encounter = physicalExamination.getEncounter();
            List<Obs> groupMembers = new ArrayList<>();
            getGroupMembersOfObs(physicalExamination, groupMembers);
            if (!encounterPhysicalExaminationMap.containsKey(encounter)) {
                encounterPhysicalExaminationMap.put(encounter, new ArrayList<>());
            }
            encounterPhysicalExaminationMap.get(encounter).addAll(groupMembers);
        }
        return encounterPhysicalExaminationMap;
    }

    private void getGroupMembersOfObs(Obs physicalExamination, List<Obs> groupMembers) {
        if (physicalExamination.getGroupMembers().size() > 0) {
            for (Obs groupMember : physicalExamination.getGroupMembers()) {
                if (conceptNames.contains(groupMember.getConcept().getDisplayString())) continue;
                getGroupMembersOfObs(groupMember, groupMembers);
            }
        } else {
            groupMembers.add(physicalExamination);
        }
    }

    private Map<Encounter, List<OpenMrsCondition>> getEncounterChiefComplaintsMap(Patient patient, String visitType, Date fromDate, Date toDate) {
        List<Obs> chiefComplaints = opConsultDao.getChiefComplaints(patient, visitType, fromDate, toDate);

        HashMap<Encounter, List<OpenMrsCondition>> encounterChiefComplaintsMap = new HashMap<>();

        for (Obs o : chiefComplaints) {
            if(!encounterChiefComplaintsMap.containsKey(o.getEncounter())){
                encounterChiefComplaintsMap.put(o.getEncounter(), new ArrayList<>());
            }
            encounterChiefComplaintsMap.get(o.getEncounter()).add(new OpenMrsCondition(o.getUuid(), o.getValueCoded().getDisplayString(), o.getDateCreated()));
        }

        return encounterChiefComplaintsMap;
    }

    private Map<Encounter, List<OpenMrsCondition>> getEncounterMedicalHistoryMap(Patient patient, String visit, Date fromDate, Date toDate) throws ParseException {
        Map<String, Integer> visitMap = new HashMap<>();
        visitMap.put("OPD", 4);
        visitMap.put("IPD", 3);
        Map<Encounter, List<OpenMrsCondition>> encounterMedicalHistoryMap = new HashMap<>();
        EncounterSearchCriteriaBuilder encounterSearchCriteriaBuilder = new EncounterSearchCriteriaBuilder();
        encounterSearchCriteriaBuilder.setPatient(patient);
        encounterSearchCriteriaBuilder.setFromDate(fromDate);
        encounterSearchCriteriaBuilder.setToDate(toDate);
        VisitType visitType = new VisitType(visitMap.get(visit));
        encounterSearchCriteriaBuilder.setVisitTypes(Collections.singleton(visitType));
        EncounterType encounterType = new EncounterType(1);
        encounterSearchCriteriaBuilder.setEncounterTypes(Collections.singleton(encounterType));
        EncounterSearchCriteria encounterSearchCriteria = encounterSearchCriteriaBuilder.createEncounterSearchCriteria();

        List<Encounter> encounters = encounterService.getEncounters(encounterSearchCriteria);
        for (Encounter e : encounters) {
            log.warn(e.toString());
        }
        return encounterMedicalHistoryMap;
    }
}
