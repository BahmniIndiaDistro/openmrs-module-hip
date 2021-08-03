package org.bahmni.module.hip.web.model;

import lombok.Getter;
import org.openmrs.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class OpenMrsOPConsult {
    private final Encounter encounter;
    private final List<OpenMrsCondition> chiefComplaintConditions;
    private final List<OpenMrsCondition> medicalHistoryConditions;
    private final List<Obs> observations;
    private final Patient patient;
    private final Set<EncounterProvider> encounterProviders;
    private final List<DrugOrder> drugOrders;

    public OpenMrsOPConsult(Encounter encounter,
                            List<OpenMrsCondition> chiefComplaintConditions,
                            List<OpenMrsCondition> medicalHistoryConditions,
                            List<Obs> observations,
                            Patient patient,
                            Set<EncounterProvider> encounterProviders,
                            List<DrugOrder> drugOrders) {
        this.encounter = encounter;
        this.chiefComplaintConditions = chiefComplaintConditions;
        this.medicalHistoryConditions = medicalHistoryConditions;
        this.observations = observations;
        this.patient = patient;
        this.encounterProviders = encounterProviders;
        this.drugOrders = drugOrders;
    }

    public static List<OpenMrsOPConsult> getOpenMrsOPConsultList(Map<Encounter, List<OpenMrsCondition>> encounterChiefComplaintsMap,
                                                                 Map<Encounter, List<OpenMrsCondition>> encounterMedicalHistoryMap,
                                                                 Map<Encounter, List<Obs>> encounterPhysicalExaminationMap,
                                                                 Map<Encounter, DrugOrders> encounteredDrugOrdersMap,
                                                                 Patient patient) {
        List<OpenMrsOPConsult> openMrsOPConsultList = new ArrayList<>();

        for(Map.Entry<Encounter, DrugOrders> entry : encounteredDrugOrdersMap.entrySet()){
            List<DrugOrder> drugOrdersList = encounteredDrugOrdersMap.get(entry.getKey()).getOpenMRSDrugOrders();
            List<OpenMrsCondition> chiefComplaintList = new ArrayList<>();
            List<OpenMrsCondition> medicalHistoryList = new ArrayList<>();
            List<Obs> physicalExaminationList = new ArrayList<>();

            if (encounterMedicalHistoryMap.containsKey(entry.getKey())) {
                medicalHistoryList = encounterMedicalHistoryMap.get(entry.getKey());
                encounterMedicalHistoryMap.remove(entry.getKey());
            }
            if (encounterPhysicalExaminationMap.containsKey(entry.getKey())) {
                physicalExaminationList = encounterPhysicalExaminationMap.get(entry.getKey());
                encounterPhysicalExaminationMap.remove(entry.getKey());
            }
            if (encounterChiefComplaintsMap.containsKey(entry.getKey())) {
                chiefComplaintList = encounterChiefComplaintsMap.get(entry.getKey());
                encounterChiefComplaintsMap.remove(entry.getKey());
            }
            openMrsOPConsultList.add(new OpenMrsOPConsult(entry.getKey(), chiefComplaintList, medicalHistoryList, physicalExaminationList, patient, entry.getKey().getEncounterProviders(), drugOrdersList));
        }

        for (Map.Entry<Encounter, List<OpenMrsCondition>> entry : encounterChiefComplaintsMap.entrySet()) {
            List<OpenMrsCondition> chiefComplaintList = encounterChiefComplaintsMap.get(entry.getKey());
            List<OpenMrsCondition> medicalHistoryList = new ArrayList<>();
            List<Obs> physicalExaminationList = new ArrayList<>();
            List<DrugOrder> drugOrdersList = new ArrayList<>();

            if (encounterMedicalHistoryMap.containsKey(entry.getKey())) {
                medicalHistoryList = encounterMedicalHistoryMap.get(entry.getKey());
                encounterMedicalHistoryMap.remove(entry.getKey());
            }
            if (encounterPhysicalExaminationMap.containsKey(entry.getKey())) {
                physicalExaminationList = encounterPhysicalExaminationMap.get(entry.getKey());
                encounterPhysicalExaminationMap.remove(entry.getKey());
            }
            if (encounteredDrugOrdersMap.containsKey(entry.getKey())) {
                drugOrdersList = encounteredDrugOrdersMap.get(entry.getKey()).getOpenMRSDrugOrders();
                encounteredDrugOrdersMap.remove(entry.getKey());
            }

            openMrsOPConsultList.add(new OpenMrsOPConsult(entry.getKey(), chiefComplaintList, medicalHistoryList, physicalExaminationList, patient, entry.getKey().getEncounterProviders(), drugOrdersList));
        }

        for (Map.Entry<Encounter, List<OpenMrsCondition>> entry : encounterMedicalHistoryMap.entrySet()) {
            List<OpenMrsCondition> chiefComplaintList = new ArrayList<>();
            List<OpenMrsCondition> medicalHistoryList = encounterMedicalHistoryMap.get(entry.getKey());
            List<Obs> physicalExaminationList = new ArrayList<>();
            List<DrugOrder> drugOrdersList = new ArrayList<>();

            if (encounterChiefComplaintsMap.containsKey(entry.getKey())) {
                chiefComplaintList = encounterChiefComplaintsMap.get(entry.getKey());
                encounterChiefComplaintsMap.remove(entry.getKey());
            }
            if (encounterPhysicalExaminationMap.containsKey(entry.getKey())) {
                physicalExaminationList = encounterPhysicalExaminationMap.get(entry.getKey());
                encounterPhysicalExaminationMap.remove(entry.getKey());
            }
            if (encounteredDrugOrdersMap.containsKey(entry.getKey())) {
                drugOrdersList = encounteredDrugOrdersMap.get(entry.getKey()).getOpenMRSDrugOrders();
                encounteredDrugOrdersMap.remove(entry.getKey());
            }
            openMrsOPConsultList.add(new OpenMrsOPConsult(entry.getKey(), chiefComplaintList, medicalHistoryList, physicalExaminationList, patient, entry.getKey().getEncounterProviders(), drugOrdersList));
        }

        for (Map.Entry<Encounter, List<Obs>> entry : encounterPhysicalExaminationMap.entrySet()) {
            List<OpenMrsCondition> chiefComplaintList = new ArrayList<>();
            List<OpenMrsCondition> medicalHistoryList = new ArrayList<>();
            List<Obs> physicalExaminationList = encounterPhysicalExaminationMap.get(entry.getKey());
            List<DrugOrder> drugOrdersList = new ArrayList<>();

            if (encounterChiefComplaintsMap.containsKey(entry.getKey())) {
                chiefComplaintList = encounterChiefComplaintsMap.get(entry.getKey());
                encounterChiefComplaintsMap.remove(entry.getKey());
            }
            if (encounterMedicalHistoryMap.containsKey(entry.getKey())) {
                medicalHistoryList = encounterMedicalHistoryMap.get(entry.getKey());
                encounterMedicalHistoryMap.remove(entry.getKey());
            }
            if (encounteredDrugOrdersMap.containsKey(entry.getKey())) {
                drugOrdersList = encounteredDrugOrdersMap.get(entry.getKey()).getOpenMRSDrugOrders();
                encounteredDrugOrdersMap.remove(entry.getKey());
            }
            openMrsOPConsultList.add(new OpenMrsOPConsult(entry.getKey(), chiefComplaintList, medicalHistoryList, physicalExaminationList, patient, entry.getKey().getEncounterProviders(), drugOrdersList));
        }
        return openMrsOPConsultList;
    }

    @Override
    public String toString() {
        return "OpenMrsOPConsult{" +
                "encounter=" + encounter +
                ", chiefComplaintConditions=" + chiefComplaintConditions +
                ", medicalHistoryConditions=" + medicalHistoryConditions +
                ", observations=" + observations +
                ", patient=" + patient +
                ", encounterProviders=" + encounterProviders +
                '}';
    }
}
