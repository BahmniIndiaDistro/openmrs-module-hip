package org.bahmni.module.hip.api.dao.impl;

import org.bahmni.module.hip.api.dao.EncounterDao;
import org.bahmni.module.hip.api.dao.PrescriptionOrderDao;
import org.hibernate.SessionFactory;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.Visit;
import org.openmrs.api.OrderService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.module.episodes.Episode;
import org.openmrs.module.episodes.service.EpisodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class PrescriptionOrderDaoImpl implements PrescriptionOrderDao {
    private final ProgramWorkflowService programWorkflowService;
    private final EncounterDao encounterDao;
    private final EpisodeService episodeService;

    @Autowired
    public PrescriptionOrderDaoImpl(ProgramWorkflowService programWorkflowService, SessionFactory sessionFactory, EncounterDao encounterDao, OrderService orderService, EpisodeService episodeService) {
        this.programWorkflowService = programWorkflowService;
        this.encounterDao = encounterDao;
        this.episodeService = episodeService;
    }

    public Map<Encounter, List<DrugOrder>> getDrugOrders(Visit visit, Date fromDate, Date toDate) {
        List<Encounter> encounterList = encounterDao.getEncountersForVisit(visit,null,fromDate,toDate);
        Map<Encounter, List<DrugOrder>> encounterOrdersMap = new HashMap<>();
        for (Encounter encounter: encounterList) {
            List<DrugOrder> orderList = encounter.getOrders().stream()
                    .filter(order -> order.getOrderType().getUuid().equals(OrderType.DRUG_ORDER_TYPE_UUID))
                    .map(order -> (DrugOrder) order)
                    .collect(Collectors.toList());
            if(orderList.size() > 0)
                encounterOrdersMap.put(encounter, orderList);
        }
        return encounterOrdersMap;
    }

    public Map<Encounter, List<DrugOrder>> getDrugOrdersForProgram(Patient patient, Date fromDate, Date toDate, OrderType orderType, String program, String programEnrollmentId) {
        List<PatientProgram> patientPrograms = programWorkflowService.getPatientPrograms(patient, programWorkflowService.getProgramByName(program), fromDate, toDate, null, null, false);
        Set<PatientProgram> patientProgramSet = new HashSet<>(patientPrograms);
        Map<Encounter, List<DrugOrder>> encounterOrdersMap = new HashMap<>();
        for (PatientProgram patientProgram : patientProgramSet) {
            Episode episode = episodeService.getEpisodeForPatientProgram(patientProgram);
            Set<Encounter> encounterSet = episode.getEncounters();
            for (Encounter encounter : encounterSet) {
                List<DrugOrder> orderList =  encounter.getOrders()
                        .stream()
                        .filter(order -> orderType.equals(order.getOrderType().getName()))
                        .map(order -> (DrugOrder) order)
                        .collect(Collectors.toList());
                if(orderList.size() > 0)
                    encounterOrdersMap.put(encounter, orderList);
            }
        }
        return encounterOrdersMap;
    }

}
