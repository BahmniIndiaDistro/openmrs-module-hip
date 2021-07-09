package org.bahmni.module.hip.web.model;

import org.bahmni.module.hip.web.service.FHIRResourceMapper;
import org.bahmni.module.hip.web.service.FHIRUtils;
import org.hl7.fhir.r4.model.*;
import org.openmrs.EncounterProvider;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class FhirOPConsult {
    private final Condition condition;
    private final Date encounterTimestamp;
    private final Integer encounterID;
    private final Encounter encounter;
    private final List<Practitioner> practitioners;
    private final Patient patient;
    private final Reference patientReference;

    public FhirOPConsult(Condition condition,
                         Date encounterTimestamp,
                         Integer encounterID,
                         Encounter encounter,
                         List<Practitioner> practitioners,
                         Patient patient,
                         Reference patientReference) {
        this.condition = condition;
        this.encounterTimestamp = encounterTimestamp;
        this.encounterID = encounterID;
        this.encounter = encounter;
        this.practitioners = practitioners;
        this.patient = patient;
        this.patientReference = patientReference;
    }

    public Bundle bundleOPConsult(String webUrl) {
        String bundleID = String.format("PR-%d", encounterID);
        Bundle bundle = FHIRUtils.createBundle(encounterTimestamp, bundleID, webUrl);
        FHIRUtils.addToBundleEntry(bundle, compositionFrom(webUrl), false);
        FHIRUtils.addToBundleEntry(bundle, practitioners, false);
        FHIRUtils.addToBundleEntry(bundle, patient, false);
        FHIRUtils.addToBundleEntry(bundle, encounter, false);
        FHIRUtils.addToBundleEntry(bundle, condition, false);
        return bundle;
    }

    public static FhirOPConsult fromOpenMrsOpConsult(OpenMrsCondition openMrsCondition, FHIRResourceMapper fhirResourceMapper) {
        Patient patient = fhirResourceMapper.mapToPatient(openMrsCondition.getPatient());
        Reference patientReference = FHIRUtils.getReferenceToResource(patient);
        Encounter encounter = fhirResourceMapper.mapToEncounter(openMrsCondition.getEncounter());
        Date encounterDatetime = openMrsCondition.getEncounter().getEncounterDatetime();
        Integer encounterId = openMrsCondition.getEncounter().getId();
        List<Practitioner> practitioners = getPractitionersFrom(fhirResourceMapper, openMrsCondition.getEncounterProviders());
        Condition condition = fhirResourceMapper.mapToCondition(openMrsCondition);
        return new FhirOPConsult(condition, encounterDatetime, encounterId, encounter, practitioners, patient, patientReference);
    }

    private static List<Practitioner> getPractitionersFrom(FHIRResourceMapper fhirResourceMapper, Set<EncounterProvider> encounterProviders) {
        return encounterProviders
                .stream()
                .map(fhirResourceMapper::mapToPractitioner)
                .collect(Collectors.toList());
    }

    private Composition compositionFrom(String webURL) {
        Composition composition = initializeComposition(encounterTimestamp, webURL);
        Composition.SectionComponent compositionSection = composition.addSection();

        practitioners
                .forEach(practitioner -> composition
                        .addAuthor().setResource(practitioner).setDisplay(FHIRUtils.getDisplay(practitioner)));

        composition
                .setEncounter(FHIRUtils.getReferenceToResource(encounter))
                .setSubject(patientReference);

        compositionSection
                .setTitle("Chief Complaint")
                .setCode(FHIRUtils.getOPConsultType());

        Reference reference = FHIRUtils.getReferenceToResource(condition);
        compositionSection.addEntry(reference);

        return composition;
    }
    private Composition initializeComposition(Date encounterTimestamp, String webURL) {
        Composition composition = new Composition();

        composition.setId(UUID.randomUUID().toString());
        composition.setDate(encounterTimestamp);
        composition.setIdentifier(FHIRUtils.getIdentifier(composition.getId(), webURL, "document"));
        composition.setStatus(Composition.CompositionStatus.FINAL);
        composition.setType(FHIRUtils.getOPConsultType());
        composition.setTitle("Chief Complaint");
        return composition;
    }
}
