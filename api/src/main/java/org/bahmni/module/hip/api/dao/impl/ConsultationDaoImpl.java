package org.bahmni.module.hip.api.dao.impl;

import org.bahmni.module.hip.Config;
import org.bahmni.module.hip.api.dao.ConsultationDao;
import org.bahmni.module.hip.api.dao.EncounterDao;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.Visit;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.module.episodes.Episode;
import org.openmrs.module.episodes.service.EpisodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.bahmni.module.hip.api.dao.Constants.ORDER_ACTION;

@Repository
public class ConsultationDaoImpl implements ConsultationDao {

    public static final ArrayList<String> ORDER_TYPES = new ArrayList<String>() {{
        add(Config.LAB_ORDER.getValue());
        add(Config.RADIOLOGY_ORDER.getValue());
    }};
    private final ProgramWorkflowService programWorkflowService;
    private final EpisodeService episodeService;
    private final EncounterDao encounterDao;

    @Autowired
    public ConsultationDaoImpl(ProgramWorkflowService programWorkflowService, EpisodeService episodeService, EncounterDao encounterDao) {
        this.programWorkflowService = programWorkflowService;
        this.episodeService = episodeService;
        this.encounterDao = encounterDao;
    }

    public List<Obs> getAllObsForProgram(String programName, Date fromDate, Date toDate, Patient patient) {
        List<PatientProgram> patientPrograms = programWorkflowService.getPatientPrograms(patient, programWorkflowService.getProgramByName(programName), fromDate, toDate, null, null, false);
        Set<PatientProgram> patientProgramSet = new HashSet<>(patientPrograms);
        List<Obs> obs = new ArrayList<>();
        for (PatientProgram patientProgram : patientProgramSet) {
            Episode episode = episodeService.getEpisodeForPatientProgram(patientProgram);
            Set<Encounter> encounterSet = episode.getEncounters();
            for (Encounter encounter : encounterSet) {
                obs.addAll(encounter.getAllObs());
            }
        }
        return obs;
    }

    @Override
    public Map<Encounter, List<Order>> getOrders(Visit visit, Date fromDate, Date toDate) {
        List<Encounter> encounters = encounterDao.getEncountersForVisit(visit,null,fromDate,toDate);
        Map<Encounter, List<Order>> encounterOrdersMap = new HashMap<>();
        for (Encounter encounter: encounters) {
            List<Order> orderList = encounter.getOrders()
                    .stream().filter(order -> order.getDateStopped() == null && !Objects.equals(order.getAction().toString(), ORDER_ACTION))
                    .filter(order -> ORDER_TYPES.contains(order.getOrderType().getName()))
                    .collect(Collectors.toList());
            if(orderList.size() > 0)
                encounterOrdersMap.put(encounter, orderList);
        }
        return encounterOrdersMap;
    }

    @Override
    public Map<Encounter, List<Order>> getOrdersForProgram(String programName, Date fromDate, Date toDate, Patient patient) {
        List<PatientProgram> patientPrograms = programWorkflowService.getPatientPrograms(patient, programWorkflowService.getProgramByName(programName), fromDate, toDate, null, null, false);
        Set<PatientProgram> patientProgramSet = new HashSet<>(patientPrograms);
        Map<Encounter, List<Order>> encounterOrdersMap = new HashMap<>();
        for (PatientProgram patientProgram : patientProgramSet) {
            Episode episode = episodeService.getEpisodeForPatientProgram(patientProgram);
            Set<Encounter> encounterSet = episode.getEncounters();
            for (Encounter encounter : encounterSet) {
                List<Order> orderList = encounter.getOrders()
                        .stream().filter(order -> order.getDateStopped() == null && !Objects.equals(order.getAction().toString(), ORDER_ACTION))
                        .filter(order -> ORDER_TYPES.contains(order.getOrderType().getName()))
                        .collect(Collectors.toList());
                if(orderList.size() > 0)
                    encounterOrdersMap.put(encounter, orderList);
            }
        }
        return encounterOrdersMap;
    }
}

