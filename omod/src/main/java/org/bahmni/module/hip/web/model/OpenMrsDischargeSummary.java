package org.bahmni.module.hip.web.model;

import lombok.Getter;
import org.apache.log4j.Logger;
import org.bahmni.module.hip.web.controller.HipControllerAdvice;
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
    private static final Logger log = Logger.getLogger(HipControllerAdvice.class);

    public OpenMrsDischargeSummary(Encounter encounter,
                                   List<Obs> observations,
                                   Patient patient,
                                   Set<EncounterProvider> encounterProviders){
        this.encounter = encounter;
        this.observations = observations;
        this.patient = patient;
        this.encounterProviders = encounterProviders;
    }
    public static List<OpenMrsDischargeSummary> getOpenMrsDischargeSummaryList(Map<Encounter, List<Obs>> encounterCarePlanMap,
                                                                               Patient patient){
        List<OpenMrsDischargeSummary> openMrsDischargeSummaryList = new ArrayList<>();
        for(Map.Entry<Encounter, List<Obs>> encounterListEntry : encounterCarePlanMap.entrySet()){
            openMrsDischargeSummaryList.add(new OpenMrsDischargeSummary(encounterListEntry.getKey(), encounterListEntry.getValue(), patient, encounterListEntry.getKey().getEncounterProviders()));
        }
        return openMrsDischargeSummaryList;
    }
}
