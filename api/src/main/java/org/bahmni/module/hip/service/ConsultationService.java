package org.bahmni.module.hip.service;

import org.bahmni.module.hip.config.AbdmConfig;
import org.bahmni.module.hip.model.OpenMrsCondition;
import org.openmrs.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public interface ConsultationService {
    ConcurrentHashMap<Encounter, List<OpenMrsCondition>> getEncounterChiefComplaintsMap(Visit visit, Date fromDate, Date toDate);

    ConcurrentHashMap<Encounter, List<OpenMrsCondition>> getEncounterChiefComplaintsMapForProgram(String programName, Date fromDate, Date toDate, Patient patient);

    Map<Encounter, List<Obs>> getEncounterPhysicalExaminationMap(Visit visit, Date fromDate, Date toDate);

    Map<Encounter, List<Obs>> getEncounterPhysicalExaminationMapForProgram(String programName, Date fromDate, Date toDate, Patient patient);

    Map<Encounter, List<OpenMrsCondition>> getEncounterMedicalHistoryConditionsMap(Visit visit, Date fromDate, Date toDate);

    Map<Encounter, List<OpenMrsCondition>> getEncounterMedicalHistoryConditionsMapForProgram(String programName, Date fromDate, Date toDate, Patient patient);

    Map<Encounter, List<Obs>> getEncounterPatientDocumentsMap(Visit visit, Date fromDate, Date toDate, AbdmConfig.HiTypeDocumentKind type);

    Map<Encounter, List<Obs>> getEncounterOtherObsMap(Visit visit, Date fromDate, Date toDate, AbdmConfig.OpConsultAttribute type);

    Map<Encounter, List<Obs>> getEncounterPatientDocumentsMapForProgram(String programName, Date fromDate, Date toDate, Patient patient, String programEnrollmentId);

    Map<Encounter, List<Order>> getEncounterOrdersMap(Visit visit, Date fromDate, Date toDate);

    Map<Encounter, List<Order>> getEncounterOrdersMapForProgram(String programName, Date fromDate, Date toDate, Patient patient);

    String getCustomDisplayStringForChiefComplaint(Set<Obs> groupMembers);

    Map<Encounter, List<Obs>> getEncounterProcedureMap(Visit visit, Date startDate, Date ToDate);

    Map<Encounter, List<Obs>> getEncounterProcedureMapForProgram(String programName, Date fromDate, Date toDate, Patient patient);

    Map<Encounter, List<Obs>> getEncounterOtherObsMapForProgram(String programName, Date fromDate, Date toDate, Patient patient, AbdmConfig.OpConsultAttribute type);
}
