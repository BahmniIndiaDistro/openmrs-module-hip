package org.bahmni.module.hip.model;

import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.NotEmpty;
import org.openmrs.Encounter;
import org.openmrs.EncounterProvider;
import org.openmrs.Obs;
import org.openmrs.Patient;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class OpenMrsPrescription {
    private final Encounter encounter;
    private final Set<EncounterProvider> encounterProviders;
    private final DrugOrders drugOrders;
    private List<Obs> docObs;
    private final Patient patient;

    private OpenMrsPrescription(@NotEmpty Encounter encounter, DrugOrders drugOrders, List<Obs> docObs) {
        this.encounter = encounter;
        this.encounterProviders = encounter.getEncounterProviders();
        this.patient = encounter.getPatient();
        this.drugOrders = drugOrders;
        this.docObs = docObs;
    }


    public static List<OpenMrsPrescription> from(Map<Encounter, DrugOrders> encounterDrugOrdersMap, Map<Encounter, List<Obs>> encounterDocObsMap) {
        List<EncounterDrugsAndDocs> encounterDrugsAndDocsList = encounterDrugOrdersMap.entrySet()
                .stream()
                .map(entry -> EncounterDrugsAndDocs.builder().encounter(entry.getKey()).drugOrders(entry.getValue()).build())
                .collect(Collectors.toList());
        encounterDocObsMap.entrySet()
                .stream()
                .forEach(entry -> {
                    EncounterDrugsAndDocs encounterDrugsAndDocs = encounterDrugsAndDocsList.stream()
                            .filter(ed -> ed.encounter.equals(entry.getKey()))
                            .findFirst()
                            .orElseGet(() -> EncounterDrugsAndDocs.builder().encounter(entry.getKey()).build());
                    encounterDrugsAndDocs.docObs = entry.getValue();
                    if (encounterDrugsAndDocs.drugOrders == null) {
                        //this is a newly created one
                        encounterDrugsAndDocsList.add(encounterDrugsAndDocs);
                    }
                });
        return encounterDrugsAndDocsList
                .stream()
                .map(entry -> new OpenMrsPrescription(entry.encounter, entry.drugOrders, entry.docObs))
                .collect(Collectors.toList());
    }

    @Builder
    public static class EncounterDrugsAndDocs {
        private Encounter encounter;
        private DrugOrders drugOrders;
        private List<Obs> docObs;
    }
}
