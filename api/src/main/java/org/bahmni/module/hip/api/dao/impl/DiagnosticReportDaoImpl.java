package org.bahmni.module.hip.api.dao.impl;

import org.bahmni.module.hip.api.dao.DiagnosticReportDao;
import org.bahmni.module.hip.api.dao.EncounterDao;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.openmrs.*;
import org.openmrs.api.EncounterService;
import org.openmrs.parameter.EncounterSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository
public class DiagnosticReportDaoImpl implements DiagnosticReportDao {

    @Autowired
    EncounterDao encounterDao;

    @Autowired
    EncounterService encounterService;

    @Override
    public List<Obs> getObsForPrograms(Patient patient, Date fromDate, Date toDate, OrderType orderType, String program, String programEnrollmentId) {
        Integer[] encounterIds = encounterDao.GetEncounterIdsForProgram(patient.getUuid(), program, programEnrollmentId, fromDate, toDate).toArray(new Integer[0]);

        List<Obs> obss = new ArrayList<>();

        for (Integer eId: encounterIds) {
            Encounter encounter = encounterService.getEncounter(eId);
            obss.addAll(encounter.getAllObs());
        }

        return obss;
    }
}
