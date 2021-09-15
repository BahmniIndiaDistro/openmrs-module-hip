package org.bahmni.module.hip.web.model;

import org.bahmni.module.hip.web.service.FHIRResourceMapper;
import org.bahmni.module.hip.web.service.FHIRUtils;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.openmrs.EncounterProvider;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class FhirDischargeSummary {

    private final Date encounterTimestamp;
    private final Integer encounterID;
    private final Encounter encounter;
    private final List<Practitioner> practitioners;
    private final Patient patient;
    private final Reference patientReference;
    private final List<CarePlan> carePlan;

    public FhirDischargeSummary(Integer encounterID,
                                Encounter encounter,
                                Date encounterTimestamp,
                                List<Practitioner> practitioners,
                                Reference patientReference,
                                Patient patient,
                                List<CarePlan> carePlan){
        this.encounterTimestamp = encounterTimestamp;
        this.encounterID = encounterID;
        this.encounter = encounter;
        this.practitioners = practitioners;
        this.patient = patient;
        this.patientReference = patientReference;
        this.carePlan = carePlan;
    }
    public static FhirDischargeSummary fromOpenMrsDischargeSummary(OpenMrsDischargeSummary openMrsDischargeSummary, FHIRResourceMapper fhirResourceMapper){
        Patient patient = fhirResourceMapper.mapToPatient(openMrsDischargeSummary.getPatient());
        Reference patientReference = FHIRUtils.getReferenceToResource(patient);
        Encounter encounter = fhirResourceMapper.mapToEncounter(openMrsDischargeSummary.getEncounter());
        Date encounterDatetime = openMrsDischargeSummary.getEncounter().getEncounterDatetime();
        Integer encounterId = openMrsDischargeSummary.getEncounter().getId();
        List<Practitioner> practitioners = getPractitionersFrom(fhirResourceMapper, openMrsDischargeSummary.getEncounter().getEncounterProviders());
        List<CarePlan> carePlans = openMrsDischargeSummary.getObservations().stream().
                map(fhirResourceMapper::mapToCarePlan).collect(Collectors.toList());

        return new FhirDischargeSummary(encounterId, encounter, encounterDatetime, practitioners, patientReference, patient, carePlans);
    }

    public Bundle bundleDischargeSummary(String webUrl){
        String bundleID = String.format("PR-%d", encounterID);
        Bundle bundle = FHIRUtils.createBundle(encounterTimestamp, bundleID, webUrl);
        FHIRUtils.addToBundleEntry(bundle, compositionFrom(webUrl), false);
        FHIRUtils.addToBundleEntry(bundle, practitioners, false);
        FHIRUtils.addToBundleEntry(bundle, patient, false);
        FHIRUtils.addToBundleEntry(bundle, encounter, false);
        FHIRUtils.addToBundleEntry(bundle, carePlan, false);
        return bundle;
    }

    private Composition compositionFrom(String webURL) {
        Composition composition = initializeComposition(encounterTimestamp, webURL);
        composition
                .setEncounter(FHIRUtils.getReferenceToResource(encounter))
                .setSubject(patientReference);

        practitioners
                .forEach(practitioner -> composition
                        .addAuthor().setResource(practitioner).setDisplay(FHIRUtils.getDisplay(practitioner)));

        if (carePlan.size() > 0) {
            Composition.SectionComponent physicalExaminationsCompositionSection = composition.addSection();
            physicalExaminationsCompositionSection
                    .setTitle("Care Plan")
                    .setCode(FHIRUtils.getCarePlanType());
            carePlan
                    .stream()
                    .map(FHIRUtils::getReferenceToResource)
                    .forEach(physicalExaminationsCompositionSection::addEntry);
        }

        return composition;
    }

    private Composition initializeComposition(Date encounterTimestamp, String webURL) {
        Composition composition = new Composition();
        composition.setId(UUID.randomUUID().toString());
        composition.setDate(encounterTimestamp);
        composition.setIdentifier(FHIRUtils.getIdentifier(composition.getId(), webURL, "Composition"));
        composition.setStatus(Composition.CompositionStatus.FINAL);
        composition.setType(FHIRUtils.getOPConsultType());
        composition.setTitle("Discharge Summary Document");
        return composition;
    }

    private static List<Practitioner> getPractitionersFrom(FHIRResourceMapper fhirResourceMapper, Set<EncounterProvider> encounterProviders) {
        return encounterProviders
                .stream()
                .map(fhirResourceMapper::mapToPractitioner)
                .collect(Collectors.toList());
    }
}
