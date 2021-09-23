package org.bahmni.module.hip.web.model;

import lombok.Getter;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.EncounterProvider;
import org.openmrs.DrugOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class OpenMrsDischargeSummary {

    private final Encounter encounter;
    private final List<Obs> carePlanObs;
    private final Patient patient;
    private final Set<EncounterProvider> encounterProviders;
    private final List<DrugOrder> drugOrders;
    private final List<OpenMrsCondition> chiefComplaints;
    private final List<OpenMrsCondition> medicalHistory;
    private final List<Obs> physicalExaminationObs;

    public OpenMrsDischargeSummary(Encounter encounter,
                                   List<Obs> carePlanObs,
                                   List<DrugOrder> drugOrders,
                                   Patient patient,
                                   Set<EncounterProvider> encounterProviders,
                                   List<OpenMrsCondition> chiefComplaints,
                                   List<OpenMrsCondition> medicalHistory,
                                   List<Obs> physicalExaminationObs){
        this.encounter = encounter;
        this.carePlanObs = carePlanObs;
        this.drugOrders = drugOrders;
        this.patient = patient;
        this.encounterProviders = encounterProviders;
        this.chiefComplaints = chiefComplaints;
        this.medicalHistory = medicalHistory;
        this.physicalExaminationObs = physicalExaminationObs;
    }
    public static List<OpenMrsDischargeSummary> getOpenMrsDischargeSummaryList(Map<Encounter, List<Obs>> encounterCarePlanMap,
                                                                               Map<Encounter, DrugOrders> encounterDrugOrdersMap,
                                                                               Map<Encounter, List<OpenMrsCondition>> encounterChiefComplaintsMap,
                                                                               Map<Encounter, List<OpenMrsCondition>> encounterMedicalHistoryMap,
                                                                               Map<Encounter, List<Obs>> encounterPhysicalExaminationMap,
                                                                               Patient patient){
        List<OpenMrsDischargeSummary> openMrsDischargeSummaryList = new ArrayList<>();

        for(Map.Entry<Encounter, List<Obs>> encounterListEntry : encounterCarePlanMap.entrySet()){
            List<Obs> carePlanList = encounterCarePlanMap.get(encounterListEntry.getKey());
            List<DrugOrder> drugOrdersList = new ArrayList<>();
            List<OpenMrsCondition> chiefComplaintList = new ArrayList<>();
            List<OpenMrsCondition> medicalHistoryList = new ArrayList<>();
            List<Obs> physicalExaminationList = new ArrayList<>();
            if (encounterDrugOrdersMap.get(encounterListEntry.getKey()) != null){
                drugOrdersList = encounterDrugOrdersMap.get(encounterListEntry.getKey()).getOpenMRSDrugOrders();
                encounterDrugOrdersMap.remove(encounterListEntry.getKey());
            }
            if (encounterChiefComplaintsMap.get(encounterListEntry.getKey()) != null) {
                chiefComplaintList = getEncounterConditions(encounterChiefComplaintsMap, encounterListEntry.getKey());
                encounterChiefComplaintsMap.remove(encounterListEntry.getKey());
            }
            if(encounterMedicalHistoryMap.get(encounterListEntry.getKey()) != null) {
                medicalHistoryList = getEncounterConditions(encounterMedicalHistoryMap, encounterListEntry.getKey());
                encounterMedicalHistoryMap.remove(encounterListEntry.getKey());
            }
            if(encounterPhysicalExaminationMap.get(encounterListEntry.getKey()) != null){
                physicalExaminationList = getEncounterObs(encounterPhysicalExaminationMap, encounterListEntry.getKey());
                encounterPhysicalExaminationMap.remove(encounterListEntry.getKey());
            }
            openMrsDischargeSummaryList.add(new OpenMrsDischargeSummary(encounterListEntry.getKey(), carePlanList, drugOrdersList, patient, encounterListEntry.getKey().getEncounterProviders(), chiefComplaintList, medicalHistoryList, physicalExaminationList));
        }

        for(Map.Entry<Encounter, DrugOrders> encounterListEntry : encounterDrugOrdersMap.entrySet()){
            List<DrugOrder> drugOrdersList = encounterDrugOrdersMap.get(encounterListEntry.getKey()).getOpenMRSDrugOrders();
            List<OpenMrsCondition> chiefComplaintList = new ArrayList<>();
            List<OpenMrsCondition> medicalHistoryList = new ArrayList<>();
            List<Obs> physicalExaminationList = new ArrayList<>();
            if (encounterChiefComplaintsMap.get(encounterListEntry.getKey()) != null) {
                chiefComplaintList = getEncounterConditions(encounterChiefComplaintsMap, encounterListEntry.getKey());
                encounterChiefComplaintsMap.remove(encounterListEntry.getKey());
            }
            if(encounterMedicalHistoryMap.get(encounterListEntry.getKey()) != null) {
                medicalHistoryList = getEncounterConditions(encounterMedicalHistoryMap, encounterListEntry.getKey());
                encounterMedicalHistoryMap.remove(encounterListEntry.getKey());
            }
            if(encounterPhysicalExaminationMap.get(encounterListEntry.getKey()) != null){
                physicalExaminationList = getEncounterObs(encounterPhysicalExaminationMap, encounterListEntry.getKey());
                encounterPhysicalExaminationMap.remove(encounterListEntry.getKey());
            }
            openMrsDischargeSummaryList.add(new OpenMrsDischargeSummary(encounterListEntry.getKey(), new ArrayList<>(), drugOrdersList, patient, encounterListEntry.getKey().getEncounterProviders(), chiefComplaintList, medicalHistoryList, physicalExaminationList));
        }

        for(Map.Entry<Encounter, List<OpenMrsCondition>> encounterListEntry : encounterChiefComplaintsMap.entrySet()){
            List<OpenMrsCondition> medicalHistoryList = new ArrayList<>();
            List<Obs> physicalExaminationList = new ArrayList<>();
            List<OpenMrsCondition> chiefComplaintList = getEncounterConditions(encounterChiefComplaintsMap, encounterListEntry.getKey());
            if(encounterMedicalHistoryMap.get(encounterListEntry.getKey()) != null) {
                medicalHistoryList = getEncounterConditions(encounterMedicalHistoryMap, encounterListEntry.getKey());
                encounterMedicalHistoryMap.remove(encounterListEntry.getKey());
            }
            if(encounterPhysicalExaminationMap.get(encounterListEntry.getKey()) != null){
                physicalExaminationList = getEncounterObs(encounterPhysicalExaminationMap, encounterListEntry.getKey());
                encounterPhysicalExaminationMap.remove(encounterListEntry.getKey());
            }
            openMrsDischargeSummaryList.add(new OpenMrsDischargeSummary(encounterListEntry.getKey(), new ArrayList<>(), new ArrayList<>(), patient, encounterListEntry.getKey().getEncounterProviders(), chiefComplaintList, medicalHistoryList, physicalExaminationList));
        }

        for(Map.Entry<Encounter, List<OpenMrsCondition>> medicalHistoryEntry : encounterMedicalHistoryMap.entrySet()){
            List<OpenMrsCondition> medicalHistoryList = getEncounterConditions(encounterChiefComplaintsMap, medicalHistoryEntry.getKey());
            List<Obs> physicalExaminationList = new ArrayList<>();
            if(encounterPhysicalExaminationMap.get(medicalHistoryEntry.getKey()) != null){
                physicalExaminationList = getEncounterObs(encounterPhysicalExaminationMap, medicalHistoryEntry.getKey());
                encounterPhysicalExaminationMap.remove(medicalHistoryEntry.getKey());
            }
            openMrsDischargeSummaryList.add(new OpenMrsDischargeSummary(medicalHistoryEntry.getKey(), new ArrayList<>(), new ArrayList<>(), patient, medicalHistoryEntry.getKey().getEncounterProviders(), new ArrayList<>(), medicalHistoryList, physicalExaminationList));
        }

        for(Map.Entry<Encounter, List<Obs>> physicalExaminationEntry : encounterPhysicalExaminationMap.entrySet()){
            List<Obs> physicalExaminationList = getEncounterObs(encounterPhysicalExaminationMap, physicalExaminationEntry.getKey());
            openMrsDischargeSummaryList.add(new OpenMrsDischargeSummary(physicalExaminationEntry.getKey(), new ArrayList<>(), new ArrayList<>(), patient, physicalExaminationEntry.getKey().getEncounterProviders(), new ArrayList<>(), new ArrayList<>(), physicalExaminationList));
        }

        return openMrsDischargeSummaryList;
    }

    public static List<OpenMrsCondition> getEncounterConditions(Map<Encounter, List<OpenMrsCondition>> map, Encounter encounter) {
        if (map.containsKey(encounter)) {
            List<OpenMrsCondition> conditionList = map.get(encounter);
            map.remove(encounter);
            return conditionList;
        }
        return null;
    }

    public static List<Obs> getEncounterObs(Map<Encounter, List<Obs>> map, Encounter encounter) {
        if (map.containsKey(encounter)) {
            List<Obs> obsList = map.get(encounter);
            map.remove(encounter);
            return obsList;
        }
        return null;
    }
}
