package org.bahmni.module.hip.web.model;

import lombok.Getter;
import org.openmrs.Encounter;
import org.openmrs.EncounterProvider;
import org.openmrs.Patient;

import java.util.Set;

@Getter
public class OpenMrsChiefComplaint {
    private final Encounter encounter;
    private final String name;
    private final String uuid;
    private final Patient patient;
    private final Set<EncounterProvider> encounterProviders;

    public OpenMrsChiefComplaint(Encounter encounter, String uuid,  String name, Patient patient, Set<EncounterProvider> encounterProviders) {
        this.encounter = encounter;
        this.uuid = uuid;
        this.name = name;
        this.patient = patient;
        this.encounterProviders = encounterProviders;
    }
}
