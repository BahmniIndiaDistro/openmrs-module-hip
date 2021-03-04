package org.bahmni.module.hip.web.model;

import org.bahmni.module.hip.web.service.FHIRResourceMapper;
import org.bahmni.module.hip.web.service.FHIRUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.EncounterProvider;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class FhirDiagnosticReport {
    private final List<Observation> observations;
    private final Date encounterTimestamp;
    private final Integer encounterID;
    private final Encounter encounter;
    private final List<Practitioner> practitioners;
    private final Patient patient;
    private final Reference patientReference;

    private FhirDiagnosticReport(Date encounterDatetime, List<Observation> observations, Integer encounterID,
                                 Encounter encounter, List<Practitioner> practitioners, Patient patient,
                                 Reference patientReference) {
        this.encounterTimestamp = encounterDatetime;
        this.observations = observations;
        this.encounterID = encounterID;
        this.encounter = encounter;
        this.practitioners = practitioners;
        this.patient = patient;
        this.patientReference = patientReference;
    }


    public Bundle bundleDiagnosticReport(String webUrl) {
        String bundleID = String.format("PR-%d", encounterID);
        Bundle bundle = FHIRUtils.createBundle(encounterTimestamp, bundleID, webUrl);

        FHIRUtils.addToBundleEntry(bundle, compositionFrom(webUrl), false);
        FHIRUtils.addToBundleEntry(bundle, practitioners, false);
        FHIRUtils.addToBundleEntry(bundle, patient, false);
        FHIRUtils.addToBundleEntry(bundle, observations, false);
        return bundle;
    }

    public static FhirDiagnosticReport fromOpenMrsDiagnosticReport(OpenMrsDiagnosticReport openMrsDiagnosticReport,
                                                                   FHIRResourceMapper fhirResourceMapper) {

        Patient patient = fhirResourceMapper.mapToPatient(openMrsDiagnosticReport.getPatient());
        Reference patientReference = FHIRUtils.getReferenceToResource(patient);
        Encounter encounter = fhirResourceMapper.mapToEncounter(openMrsDiagnosticReport.getEncounter());
        Date encounterDatetime = openMrsDiagnosticReport.getEncounter().getEncounterDatetime();
        Integer encounterId = openMrsDiagnosticReport.getEncounter().getId();
        List<Practitioner> practitioners = getPractitionersFrom(fhirResourceMapper, openMrsDiagnosticReport.getEncounterProviders());
        List<Observation> observations = openMrsDiagnosticReport.getEncounter().getAllObs().stream()
                .map(fhirResourceMapper::mapToObs).collect(Collectors.toList());
        for (Observation o : observations) {
            String valueText = o.getValueStringType().getValueAsString();
            o.getValueStringType().setValueAsString("/document_images/" + valueText);
        }
        return new FhirDiagnosticReport(encounterDatetime, observations, encounterId, encounter, practitioners, patient, patientReference);
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
                .setTitle("Diagnostic Report")
                .setCode(FHIRUtils.getDiagnosticReportType());

        observations
                .stream()
                .map(FHIRUtils::getReferenceToResource)
                .forEach(compositionSection::addEntry);

        return composition;
    }

    private Composition initializeComposition(Date encounterTimestamp, String webURL) {
        Composition composition = new Composition();

        composition.setId(UUID.randomUUID().toString());
        composition.setDate(encounterTimestamp);
        composition.setIdentifier(FHIRUtils.getIdentifier(composition.getId(), webURL, "document"));
        composition.setStatus(Composition.CompositionStatus.FINAL);
        composition.setType(FHIRUtils.getDiagnosticReportType());
        composition.setTitle("Diagnostic Report");
        return composition;
    }

    private static List<Practitioner> getPractitionersFrom(FHIRResourceMapper fhirResourceMapper, Set<EncounterProvider> encounterProviders) {
        return encounterProviders
                .stream()
                .map(fhirResourceMapper::mapToPractitioner)
                .collect(Collectors.toList());
    }
}
