package org.bahmni.module.hip.web.service;

import org.apache.log4j.Logger;
import org.bahmni.module.hip.api.dao.OPConsultDao;
import org.bahmni.module.hip.web.controller.HipControllerAdvice;
import org.bahmni.module.hip.web.model.*;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OPConsultService {
    private static final Logger log = Logger.getLogger(OPConsultService.class);
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
        Map<Encounter, List<OpenMrsCondition>> encounterMedicalHistoryMap = getEncounterMedicalHistoryMap(patientUuid, visitType, fromDate, toDate);
        Map<Encounter, List<Obs>> encounterPhysicalExaminationMap = getEncounterPhysicalExaminationMap(patientUuid, visitType, fromDate, toDate);
        DrugOrders drugOrders = new DrugOrders(openMRSDrugOrderClient.getDrugOrdersByDateAndVisitTypeFor(patientUuid, dateRange, visitType));
        Map<Encounter, DrugOrders> encounteredDrugOrdersMap = drugOrders.groupByEncounter();
        Map<Encounter, Obs> encounterProcedureMap = getEncounterProcedureMap(patientUuid, visitType, fromDate, toDate);
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

    private Map<Encounter, Obs> getEncounterProcedureMap(String patientUuid, String visitType, Date fromDate, Date toDate) {
        List<Integer> obsIds = opConsultDao.getProcedures(patientUuid, visitType, fromDate, toDate);
        Map<Encounter, Obs> encounterProcedureMap = new HashMap<>();
        for (int obsId:obsIds) {
            Obs obs = obsService.getObs(obsId);
            Encounter encounter = obs.getEncounter();
            encounterProcedureMap.put(encounter, obs);
        }
        return encounterProcedureMap;
    }

    private Map<Encounter, List<Obs>> getEncounterPhysicalExaminationMap(String patientUuid, String visitType, Date fromDate, Date toDate) {
        List<Integer> physicalExaminationObsIds = opConsultDao.getPhysicalExamination(patientUuid, visitType, fromDate, toDate);
        List<Obs> physicalExaminationObs = physicalExaminationObsIds.stream().map(obsService::getObs).collect(Collectors.toList());
        Map<Encounter, List<Obs>> encounterPhysicalExaminationMap = new HashMap<>();
        for (Obs o : physicalExaminationObs) {
            Encounter encounter = o.getEncounter();
            if(!encounterPhysicalExaminationMap.containsKey(encounter)) {
                encounterPhysicalExaminationMap.put(encounter, new ArrayList<>());
            }
            encounterPhysicalExaminationMap.get(encounter).add(o);
        }
        return encounterPhysicalExaminationMap;
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

    private Map<Encounter, List<OpenMrsCondition>> getEncounterMedicalHistoryMap(String patientUuid, String visitType, Date fromDate, Date toDate) throws ParseException {
        List<String[]> medicalHistoryIds =  opConsultDao.getMedicalHistory(patientUuid, visitType, fromDate, toDate);
        Map<Encounter, List<OpenMrsCondition>> encounterMedicalHistoryMap = new HashMap<>();
        for (Object[] id : medicalHistoryIds) {
            Encounter encounter = encounterService.getEncounter(Integer.parseInt(String.valueOf(id[2])));
            if (!encounterMedicalHistoryMap.containsKey(encounter))
                encounterMedicalHistoryMap.put(encounter, new ArrayList<>());
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = dateFormat.parse(String.valueOf(id[3]));
            encounterMedicalHistoryMap.get(encounter).add(new OpenMrsCondition(String.valueOf(id[1]), conceptService.getConcept(Integer.parseInt(String.valueOf(id[0]))).getDisplayString(), date));
        }
        return encounterMedicalHistoryMap;
    }

}
