package org.bahmni.module.hip.web.model;

import lombok.Getter;
import org.hibernate.validator.constraints.NotEmpty;
import org.openmrs.Encounter;
import org.openmrs.EncounterProvider;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.module.bahmniemrapi.laborder.contract.LabOrderResult;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class OpenMrsLabResults {
    private Encounter encounter;
    private Patient patient;
    private final Set<EncounterProvider> encounterProviders;
    private Map<LabOrderResult, Obs> labResults;
    private List<Obs> observation;



    public OpenMrsLabResults(@NotEmpty Encounter encounter, Patient patient, List<Obs> observation) {
        this.encounter = encounter;
        this.patient = patient;
        this.observation = observation;
        this.encounterProviders = encounter.getEncounterProviders();
    }

    public OpenMrsLabResults(@NotEmpty Encounter encounter, Patient patient, Map<LabOrderResult, Obs> labResults) {
        this.encounter = encounter;
        this.patient = patient;
        this.labResults = labResults;
        this.encounterProviders = encounter.getEncounterProviders();
    }
}
