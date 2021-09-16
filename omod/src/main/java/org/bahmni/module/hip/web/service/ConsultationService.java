package org.bahmni.module.hip.web.service;

import org.bahmni.module.hip.api.dao.ConsultationDao;
import org.bahmni.module.hip.web.model.OpenMrsCondition;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
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

    @Autowired
    public ConsultationService(ConsultationDao consultationDao) {
        this.consultationDao = consultationDao;
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
}
