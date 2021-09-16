package org.bahmni.module.hip.web.model;

import lombok.Getter;
import org.openmrs.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class OpenMrsDischargeSummary {

    private final Encounter encounter;
    private final List<Obs> observations;
    private final Patient patient;
    private final Set<EncounterProvider> encounterProviders;
    private final List<DrugOrder> drugOrders;

    public OpenMrsDischargeSummary(Encounter encounter,
                                   List<Obs> observations,
                                   List<DrugOrder> drugOrders,
                                   Patient patient,
                                   Set<EncounterProvider> encounterProviders){
        this.encounter = encounter;
        this.observations = observations;
        this.drugOrders = drugOrders;
        this.patient = patient;
        this.encounterProviders = encounterProviders;
    }
    public static List<OpenMrsDischargeSummary> getOpenMrsDischargeSummaryList(Map<Encounter, List<Obs>> encounterCarePlanMap,
                                                                               Map<Encounter, DrugOrders> encounterDrugOrdersMap,
                                                                               Patient patient){
        List<OpenMrsDischargeSummary> openMrsDischargeSummaryList = new ArrayList<>();

        for(Map.Entry<Encounter, List<Obs>> encounterListEntry : encounterCarePlanMap.entrySet()){
            List<Obs> carePlanList = encounterCarePlanMap.get(encounterListEntry.getKey());
            List<DrugOrder> drugOrdersList = new ArrayList<>();
            if (encounterDrugOrdersMap.get(encounterListEntry.getKey()) != null){
                drugOrdersList = encounterDrugOrdersMap.get(encounterListEntry.getKey()).getOpenMRSDrugOrders();
                encounterDrugOrdersMap.remove(encounterListEntry.getKey());
                drugOrdersList = drugOrdersList == null ? new ArrayList<>() : drugOrdersList;
            }
            openMrsDischargeSummaryList.add(new OpenMrsDischargeSummary(encounterListEntry.getKey(), carePlanList, drugOrdersList, patient, encounterListEntry.getKey().getEncounterProviders()));
        }

        for(Map.Entry<Encounter, DrugOrders> encounterListEntry : encounterDrugOrdersMap.entrySet()){
            List<DrugOrder> drugOrdersList = encounterDrugOrdersMap.get(encounterListEntry.getKey()).getOpenMRSDrugOrders();
            openMrsDischargeSummaryList.add(new OpenMrsDischargeSummary(encounterListEntry.getKey(), new ArrayList<>(), drugOrdersList, patient, encounterListEntry.getKey().getEncounterProviders()));
        }

        return openMrsDischargeSummaryList;
    }
}
