package org.bahmni.module.hip.service.impl;

import org.bahmni.module.hip.Config;
import org.bahmni.module.hip.api.dao.ConsultationDao;
import org.bahmni.module.hip.api.dao.OPConsultDao;
import org.bahmni.module.hip.builder.OmrsObsDocumentTransformer;
import org.bahmni.module.hip.config.AbdmConfig;
import org.bahmni.module.hip.model.OpenMrsCondition;
import org.bahmni.module.hip.service.ConsultationService;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.module.emrapi.conditionslist.Condition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationDao consultationDao;
    private final OPConsultDao opConsultDao;
    private final OmrsObsDocumentTransformer documentTransformer;
    private final AbdmConfig abdmConfig;

    public static Set<String> conceptNames = new HashSet<>(Arrays.asList("Image", "Tuberculosis, Treatment Plan", "Tuberculosis, Next Followup Visit", "Tuberculosis, Plan for next visit", "Tuberculosis, Patient Category", "Current Followup Visit After",
            "Tuberculosis, Plan for next visit", "Malaria, Parents Name", "Malaria, Death Date", "Childhood Illness, Vitamin A Capsules Provided", "Childhood Illness, Albendazole Given", "Childhood Illness, Referred out",
            "Childhood Illness, Vitamin A Capsules Provided", "Childhood Illness, Albendazole Given", "Nutrition, Bal Vita Provided by FCHV", "Bal Vita Provided by FCHV", "ART, Condoms given", "HIVTC, Marital Status", "Malaria, Contact number",
            "HIVTC, Transferred out", "HIVTC, Regimen when transferred out", "HIVTC, Date of transferred out", "HIVTC, Transferred out to", "HIVTC, Chief Complaint"));

    @Autowired
    public ConsultationServiceImpl(ConsultationDao consultationDao, OPConsultDao opConsultDao, OmrsObsDocumentTransformer documentTransformer, AbdmConfig abdmConfig) {
        this.consultationDao = consultationDao;
        this.opConsultDao = opConsultDao;
        this.documentTransformer = documentTransformer;
        this.abdmConfig = abdmConfig;
    }

    @Override
    public ConcurrentHashMap<Encounter, List<OpenMrsCondition>> getEncounterChiefComplaintsMap(Visit visit, Date fromDate, Date toDate) {
        Stream<Obs> obsStream = visit.getEncounters().stream()
                .filter(e -> fromDate == null || e.getEncounterDatetime().after(fromDate))
                .filter(e -> toDate == null || e.getEncounterDatetime().before(toDate))
                .map(Encounter::getAllObs)
                .flatMap(Collection::stream);
        List<Obs> chiefComplaintObs = getObservationsForChiefComplaint(obsStream);
        return getEncounterListConcurrentHashMapForChiefComplaint(chiefComplaintObs);
    }

    @Override
    public ConcurrentHashMap<Encounter, List<OpenMrsCondition>> getEncounterChiefComplaintsMapForProgram(String programName, Date fromDate, Date toDate, Patient patient) {
        Stream<Obs> obs = consultationDao.getAllObsForProgram(programName, fromDate, toDate, patient).stream();
        List<Obs> chiefComplaintObs = getObservationsForChiefComplaint(obs);
        return getEncounterListConcurrentHashMapForChiefComplaint(chiefComplaintObs);
    }

    @Override
    public Map<Encounter, List<Obs>> getEncounterPhysicalExaminationMap(Visit visit, Date fromDate, Date toDate) {
        List<Concept> conceptList = abdmConfig.getOPConsultAttributeConcept(AbdmConfig.OpConsultAttribute.PHYSICAl_EXAMINATION);
        Map<Encounter, List<Obs>> encounterObsMap = visit.getEncounters().stream()
                .filter(e -> fromDate == null || e.getEncounterDatetime().after(fromDate))
                .filter(e -> toDate == null || e.getEncounterDatetime().before(toDate))
                .map(Encounter::getAllObs)
                .flatMap(Collection::stream)
                .filter(obs -> conceptList.contains(obs.getConcept()))
                .collect(Collectors.groupingBy(obs -> obs.getEncounter()));
        return encounterObsMap;
    }

    @Override
    public Map<Encounter, List<Obs>> getEncounterPhysicalExaminationMapForProgram(String programName, Date fromDate, Date toDate, Patient patient) {
        List<Concept> conceptList = abdmConfig.getOPConsultAttributeConcept(AbdmConfig.OpConsultAttribute.PHYSICAl_EXAMINATION);
        List<Obs> physicalExaminations = consultationDao.getAllObsForProgram(programName, fromDate, toDate, patient)
                .stream().filter(obs -> conceptList.contains(obs.getConcept())).collect(Collectors.toList());
        return groupObsByEncounter(physicalExaminations);
    }

    @Override
    public Map<Encounter, List<OpenMrsCondition>> getEncounterMedicalHistoryConditionsMap(Visit visit, Date fromDate, Date toDate) {
        Map<Encounter, List<Condition>> medicalHistoryConditionsMap = opConsultDao.getMedicalHistoryConditions(visit, fromDate, toDate);
        List<Obs> medicalHistoryDiagnosisMap = opConsultDao.getMedicalHistoryDiagnosis(visit, fromDate, toDate);
        return getEncounterListMapForMedicalHistory(medicalHistoryConditionsMap, medicalHistoryDiagnosisMap);
    }

    @Override
    public Map<Encounter, List<OpenMrsCondition>> getEncounterMedicalHistoryConditionsMapForProgram(String programName, Date fromDate, Date toDate, Patient patient) {
        Map<Encounter, List<Condition>> medicalHistoryConditionsMap = opConsultDao.getMedicalHistoryConditionsForProgram(programName, fromDate, toDate, patient);
        List<Obs> medicalHistoryDiagnosisMap = opConsultDao.getMedicalHistoryDiagnosisForProgram(programName, fromDate, toDate, patient);
        return getEncounterListMapForMedicalHistory(medicalHistoryConditionsMap, medicalHistoryDiagnosisMap);
    }

    @Override
    public Map<Encounter, List<Obs>> getEncounterPatientDocumentsMap(Visit visit, Date fromDate, Date toDate, AbdmConfig.HiTypeDocumentKind type) {
        return visit.getEncounters()
                .stream()
                .filter(e -> fromDate == null || e.getEncounterDatetime().after(fromDate))
                .filter(e -> toDate == null || e.getEncounterDatetime().before(toDate))
                .map(e -> e.getObsAtTopLevel(false))
                .flatMap(Collection::stream)
                .filter(obs -> isHiTypeSupportedObs(obs, type))
                .collect(Collectors.groupingBy(Obs::getEncounter));
    }

    @Override
    public Map<Encounter, List<Obs>> getEncounterOtherObsMap(Visit visit, Date fromDate, Date toDate, AbdmConfig.OpConsultAttribute type) {
        List<Concept> supportedConcepts = abdmConfig.getOPConsultAttributeConcept(type);
        return visit.getEncounters()
                .stream()
                .filter(e -> fromDate == null || e.getEncounterDatetime().after(fromDate))
                .filter(e -> toDate == null || e.getEncounterDatetime().before(toDate))
                .map(Encounter::getAllObs)
                .flatMap(Collection::stream)
                .filter(obs -> supportedConcepts.contains(obs.getConcept()))
                .collect(Collectors.groupingBy(obs -> obs.getEncounter()));
    }

    private boolean isHiTypeSupportedObs(Obs obs, AbdmConfig.HiTypeDocumentKind type) {
        if (documentTransformer.isSupportedHiTypeDocument(obs.getConcept(), type)) {
            System.out.println("Concept is supported");
            return true;
        }
        if (documentTransformer.isSupportedDocument(obs, AbdmConfig.DocTemplateAttribute.TEMPLATE)) {
            if (!isExternalOriginDoc(obs)) {
                Concept docTypeField = abdmConfig.getDocTemplateAtributeConcept(AbdmConfig.DocTemplateAttribute.DOC_TYPE);
                if (docTypeField == null) return false;
                Optional<Concept> identifiedMember = obs.getGroupMembers().stream()
                        .filter(member -> member.getConcept().getUuid().equals(docTypeField.getUuid()))
                        .map(Obs::getValueCoded)
                        .findFirst();
                if (identifiedMember.isPresent()) {
                    return documentTransformer.isSupportedHiTypeDocument(identifiedMember.get(), type);
                }
            }
        }
        return false;
    }

    private boolean isExternalOriginDoc(Obs obs) {
        if (obs.isObsGrouping()) {
            Concept externalOriginDocConcept = abdmConfig.getDocTemplateAtributeConcept(AbdmConfig.DocTemplateAttribute.EXTERNAL_ORIGIN);
            if (externalOriginDocConcept == null) {
                return false;
            }
            Optional<Obs> externalOriginObs = obs.getGroupMembers().stream().filter(o -> o.getConcept().getUuid().equals(externalOriginDocConcept.getUuid())).findFirst();
            if (externalOriginObs.isPresent()) {
                return !StringUtils.isEmpty(externalOriginObs.get().getValueText());
            }
        }
        return false;
    }

    @Override
    public Map<Encounter, List<Obs>> getEncounterPatientDocumentsMapForProgram(String programName, Date fromDate, Date toDate, Patient patient, String programEnrollmentId) {
        final int patientDocumentEncounterType = 9;
//        Map<Encounter, List<Obs>> encounterDiagnosticReportsMap = diagnosticReportService.getAllObservationsForPrograms(fromDate,toDate,patient, programName, programEnrollmentId) ;
//        return getEncounterListMapForPatientDocument(patientDocumentEncounterType, encounterDiagnosticReportsMap);
        return null;
    }

    @Override
    public Map<Encounter, List<Order>> getEncounterOrdersMap(Visit visit, Date fromDate, Date toDate) {
        return consultationDao.getOrders(visit, fromDate, toDate);
    }

    @Override
    public Map<Encounter, List<Order>> getEncounterOrdersMapForProgram(String programName, Date fromDate, Date toDate, Patient patient) {
        return consultationDao.getOrdersForProgram(programName, fromDate, toDate, patient);
    }

    private ConcurrentHashMap<Encounter, List<OpenMrsCondition>> getEncounterListConcurrentHashMapForChiefComplaint(List<Obs> chiefComplaints) {
        ConcurrentHashMap<Encounter, List<OpenMrsCondition>> encounterChiefComplaintsMap = new ConcurrentHashMap<>();

        for (Obs obs : chiefComplaints) {
            String value;
            if (obs.getGroupMembers().size() > 0 && Config.CONCEPT_DETAILS_CONCEPT_CLASS.getValue().equals(obs.getConcept().getConceptClass().getName()) && obs.getFormFieldNamespace() != null) {
                value = getCustomDisplayStringForChiefComplaint(obs.getGroupMembers());
            } else
                value = getObsValue(obs);
            if (!encounterChiefComplaintsMap.containsKey(obs.getEncounter())) {
                encounterChiefComplaintsMap.put(obs.getEncounter(), new ArrayList<>());
            }
            encounterChiefComplaintsMap.get(obs.getEncounter()).add(new OpenMrsCondition(obs.getUuid(), value, obs.getDateCreated()));
        }

        return encounterChiefComplaintsMap;
    }

    private String getObsValue(Obs obs) {
        if (obs.getValueCoded() != null)
            return obs.getValueCoded().getDisplayString();
        else if (obs.getValueNumeric() != null)
            return String.valueOf(Math.round(obs.getValueNumeric()));
        return obs.getValueText();
    }

    private List<Obs> getObservationsForChiefComplaint(Stream<Obs> obsStream) {
        Concept chiefComplaintObsRootConcept = abdmConfig.getChiefComplaintObsRootConcept();
        if (abdmConfig.getChiefComplaintObsRootConcept() != null) {
            return obsStream.filter(obs -> obs.getConcept().equals(chiefComplaintObsRootConcept)).collect(Collectors.toList());
        }
        List<Concept> conceptList = abdmConfig.getHistoryExaminationConcepts();
        return obsStream.filter(obs -> conceptList.contains(obs.getConcept())).collect(Collectors.toList());
    }

    private Map<Encounter, List<OpenMrsCondition>> getEncounterListMapForMedicalHistory(Map<Encounter, List<Condition>> medicalHistoryConditionsMap, List<Obs> medicalHistoryDiagnosisMap) {
        Map<Encounter, List<OpenMrsCondition>> encounterMedicalHistoryMap = new HashMap<>();

        for (Map.Entry<Encounter, List<Condition>> medicalHistory : medicalHistoryConditionsMap.entrySet()) {
            if (!encounterMedicalHistoryMap.containsKey(medicalHistory.getKey())) {
                encounterMedicalHistoryMap.put(medicalHistory.getKey(), new ArrayList<>());
            }
            for (Condition condition : medicalHistory.getValue()) {
                encounterMedicalHistoryMap.get(medicalHistory.getKey()).add(new OpenMrsCondition(condition.getUuid(), condition.getConditionNonCoded() != null ? condition.getConditionNonCoded() : condition.getConcept().getDisplayString(), condition.getDateCreated()));
            }
        }
        for (Obs obs : medicalHistoryDiagnosisMap) {
            if (!encounterMedicalHistoryMap.containsKey(obs.getEncounter())) {
                encounterMedicalHistoryMap.put(obs.getEncounter(), new ArrayList<>());
            }
            encounterMedicalHistoryMap.get(obs.getEncounter()).add(new OpenMrsCondition(obs.getUuid(), obs.getValueText() != null ? obs.getValueText() : obs.getValueCoded().getDisplayString(), obs.getDateCreated()));
        }
        return encounterMedicalHistoryMap;
    }

    private Map<Encounter, List<Obs>> getEncounterListMapForPatientDocument(int patientDocumentEncounterType, Map<Encounter, List<Obs>> encounterDiagnosticReportsMap) {
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

    private Map<Encounter, List<Order>> getEncounterListMapForOrders(List<Order> orders) {
        Map<Encounter, List<Order>> encounterOrdersMap = new HashMap<>();
        for (Order order : orders) {
            if (!encounterOrdersMap.containsKey(order.getEncounter())) {
                encounterOrdersMap.put(order.getEncounter(), new ArrayList<>());
            }
            encounterOrdersMap.get(order.getEncounter()).add(order);
        }
        return encounterOrdersMap;
    }

    private void getGroupMembersOfObs(Obs obs, List<Obs> groupMembers) {
        if (obs.getGroupMembers().size() > 0 && !Config.CONCEPT_DETAILS_CONCEPT_CLASS.getValue().equals(obs.getConcept().getConceptClass().getName())) {
            for (Obs groupMember : obs.getGroupMembers()) {
                if (conceptNames.contains(groupMember.getConcept().getDisplayString())) continue;
                getGroupMembersOfObs(groupMember, groupMembers);
            }
        } else {
            groupMembers.add(obs);
        }
    }

    @Override
    public String getCustomDisplayStringForChiefComplaint(Set<Obs> groupMembers) {
        String chiefComplaintCoded = null, signOrSymptomDuration = null, chiefComplaintDuration = null;
        for (Obs childObs : groupMembers) {
            if (childObs.getConcept().equals(abdmConfig.getHnEConcept(AbdmConfig.HistoryAndExamination.CHIEF_COMPLAINT_CODED))
                    || childObs.getConcept().equals(abdmConfig.getHnEConcept(AbdmConfig.HistoryAndExamination.CHIEF_COMPLAINT_NON_CODED)))
                chiefComplaintCoded = getObsValue(childObs);
            if (childObs.getConcept().equals(abdmConfig.getHnEConcept(AbdmConfig.HistoryAndExamination.SIGN_SYMPTOM_DURATION)))
                signOrSymptomDuration = getObsValue(childObs);
            if (childObs.getConcept().equals(abdmConfig.getHnEConcept(AbdmConfig.HistoryAndExamination.CHIEF_COMPLAINT_DURATION)))
                chiefComplaintDuration = getObsValue(childObs);
        }
        return (chiefComplaintCoded + " " + "since" + " " + signOrSymptomDuration + " " + chiefComplaintDuration);
    }

    @Override
    public Map<Encounter, List<Obs>> getEncounterProcedureMap(Visit visit, Date startDate, Date ToDate) {
        Map<Encounter, List<Obs>> encounterProcedureMap = new HashMap<>();
        for (Encounter encounter : visit.getEncounters().stream().filter(e -> startDate == null || e.getEncounterDatetime().after(startDate)).collect(Collectors.toList())) {
            List<Obs> obsList = encounter.getAllObs().stream().filter(obs -> obs.getConcept().equals(abdmConfig.getProcedureObsRootConcept())).collect(Collectors.toList());
            if (obsList.size() > 0)
                encounterProcedureMap.put(encounter, obsList);
        }
        return encounterProcedureMap;
    }

    @Override
    public Map<Encounter, List<Obs>> getEncounterProcedureMapForProgram(String programName, Date fromDate, Date toDate, Patient patient) {
        List<Obs> obsProcedures = opConsultDao.getProceduresForProgram(programName, fromDate, toDate, patient);
        return obsProcedures.stream().collect(Collectors.groupingBy(Obs::getEncounter));
    }

    @Override
    public Map<Encounter, List<Obs>> getEncounterOtherObsMapForProgram(String programName, Date fromDate, Date toDate, Patient patient, AbdmConfig.OpConsultAttribute type) {
        List<Concept> conceptList = abdmConfig.getOPConsultAttributeConcept(type);
        List<Obs> otherObs = consultationDao.getAllObsForProgram(programName, fromDate, toDate, patient)
                .stream().filter(obs -> conceptList.contains(obs.getConcept())).collect(Collectors.toList());
        return groupObsByEncounter(otherObs);
    }

    private Map<Encounter, List<Obs>> groupObsByEncounter(List<Obs> obsList) {
        Map<Encounter, List<Obs>> encounteObsMap = new HashMap<>();
        for (Obs obs : obsList) {
            Encounter encounter = obs.getEncounter();
            List<Obs> groupMembers = new ArrayList<>();
            getGroupMembersOfObs(obs, groupMembers);
            if (!encounteObsMap.containsKey(encounter)) {
                encounteObsMap.put(encounter, new ArrayList<>());
            }
            encounteObsMap.get(encounter).addAll(groupMembers);
        }
        return encounteObsMap;
    }
}
