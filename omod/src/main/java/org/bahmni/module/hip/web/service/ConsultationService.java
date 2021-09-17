package org.bahmni.module.hip.web.service;

import org.bahmni.module.hip.api.dao.ConsultationDao;
import org.bahmni.module.hip.api.dao.OPConsultDao;
import org.bahmni.module.hip.web.model.OpenMrsCondition;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.module.emrapi.conditionslist.Condition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Date;

@Service
public class ConsultationService {

    private final ConsultationDao consultationDao;
    private final OPConsultDao opConsultDao;

    @Autowired
    public ConsultationService(ConsultationDao consultationDao, OPConsultDao opConsultDao) {
        this.consultationDao = consultationDao;
        this.opConsultDao = opConsultDao;
    }

    public Map<Encounter, List<OpenMrsCondition>> getEncounterChiefComplaintsMap(Patient patient, String visitType, Date fromDate, Date toDate) {
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

    public Map<Encounter, List<OpenMrsCondition>> getEncounterMedicalHistoryConditionsMap(Patient patient, String visit, Date fromDate, Date toDate) {
        Map<Encounter, List<Condition>> medicalHistoryConditionsMap =  opConsultDao.getMedicalHistoryConditions(patient, visit, fromDate, toDate);
        List<Obs> medicalHistoryDiagnosisMap =  opConsultDao.getMedicalHistoryDiagnosis(patient, visit, fromDate, toDate);
        Map<Encounter, List<OpenMrsCondition>> encounterMedicalHistoryMap = new HashMap<>();

        for(Map.Entry<Encounter, List<Condition>> medicalHistory : medicalHistoryConditionsMap.entrySet()){
            if (!encounterMedicalHistoryMap.containsKey(medicalHistory.getKey())){
                encounterMedicalHistoryMap.put(medicalHistory.getKey(), new ArrayList<>());
            }
            for(Condition condition : medicalHistory.getValue()){
                encounterMedicalHistoryMap.get(medicalHistory.getKey()).add(new OpenMrsCondition(condition.getUuid(), condition.getConcept().getDisplayString(), condition.getDateCreated()));
            }
        }
        for(Obs obs : medicalHistoryDiagnosisMap){
            if (!encounterMedicalHistoryMap.containsKey(obs.getEncounter())){
                encounterMedicalHistoryMap.put(obs.getEncounter(), new ArrayList<>());
            }
            encounterMedicalHistoryMap.get(obs.getEncounter()).add(new OpenMrsCondition(obs.getUuid(), obs.getValueCoded().getDisplayString(), obs.getDateCreated()));
        }
        return encounterMedicalHistoryMap;
    }
}
