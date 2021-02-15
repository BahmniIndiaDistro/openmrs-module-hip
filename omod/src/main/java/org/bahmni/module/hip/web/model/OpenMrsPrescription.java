package org.bahmni.module.hip.web.model;

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
    private Encounter encounter;
    private Set<EncounterProvider> encounterProviders;
    private DrugOrders drugOrders;
    private Patient patient;
    private Obs obs;

    private OpenMrsPrescription(@NotEmpty Encounter encounter, DrugOrders drugOrders) {
        this.encounter = encounter;
        this.encounterProviders = encounter.getEncounterProviders();
        this.patient = encounter.getPatient();
        this.drugOrders = drugOrders;
    }

    private OpenMrsPrescription(@NotEmpty Encounter encounter, Obs obs) {
        this.encounter = encounter;
        this.encounterProviders = encounter.getEncounterProviders();
        this.patient = encounter.getPatient();
        this.obs = obs;
    }


    public static List<OpenMrsPrescription> from(Map<Encounter, DrugOrders> encounterDrugOrdersMap) {
        return encounterDrugOrdersMap
                .entrySet()
                .stream()
                .map(entry -> new OpenMrsPrescription(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public static List<OpenMrsPrescription> fromDiagnosticReport(Map<Encounter, Obs> encounterDrugOrdersMap) {
        return encounterDrugOrdersMap
                .entrySet()
                .stream()
                .map(entry -> new OpenMrsPrescription(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
}
