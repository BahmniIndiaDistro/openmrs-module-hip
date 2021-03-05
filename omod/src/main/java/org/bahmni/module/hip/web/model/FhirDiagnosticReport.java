package org.bahmni.module.hip.web.model;

import org.bahmni.module.hip.web.service.FHIRResourceMapper;
import org.bahmni.module.hip.web.service.FHIRUtils;
import org.openmrs.Patient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.StringType;
import org.openmrs.Encounter;
import org.openmrs.module.bahmniemrapi.laborder.contract.LabOrderResult;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

public class FhirDiagnosticReport {
    private final List<Observation> observations;
    private final Date encounterTimestamp;
    private final Encounter encounter;
    private String panelName;
    private final Patient patient;


    public void setPanelName(String panelName) {
        this.panelName = panelName;
    }
    public String getPanelName() {
        return panelName;
    }

    private FhirDiagnosticReport(Date encounterDatetime, List<Observation> observations, Encounter encounter, Patient patient ) {
        this.encounterTimestamp = encounterDatetime;
        this.observations = observations;
        this.encounter = encounter;
        this.patient = patient;
    }
    public Bundle bundleDiagnosticReport(String webUrl) {
        String bundleID = String.format("LR-%d", encounter.getEncounterId());
        Bundle bundle = FHIRUtils.createBundle(encounterTimestamp, bundleID, webUrl);

        FHIRUtils.addToBundleEntry(bundle, observations, false);
        return bundle;
    }
    public Bundle bundleLabResults (String webUrl, FHIRResourceMapper fhirResourceMapper) {
        String bundleID = String.format("LR-%d", encounter.getEncounterId());

        Bundle bundle = FHIRUtils.createBundle(this.encounterTimestamp, bundleID, webUrl);
        addToBundleEntry(bundle, "", fhirResourceMapper);
        return bundle;
    }
    private void addToBundleEntry(Bundle bundle, String webURL,  FHIRResourceMapper fhirResourceMapper) {
        Composition composition = initializeComposition(encounterTimestamp, webURL);
        Composition.SectionComponent compositionSection = composition.addSection();
        org.hl7.fhir.r4.model.Patient patient = fhirResourceMapper.mapToPatient(this.patient);
        org.hl7.fhir.r4.model.Encounter encounterFHIR = fhirResourceMapper.mapToEncounter(encounter);
        List<Practitioner> practitioners = encounter.getEncounterProviders()
                                                    .stream()
                                                    .map( fhirResourceMapper::mapToPractitioner)
                                                    .collect(Collectors.toList());
        Reference subjectReference = FHIRUtils.getReferenceToResource(patient);
        Reference encounterReference = FHIRUtils.getReferenceToResource(encounterFHIR);

        practitioners.forEach(practitioner -> composition
                                        .addAuthor()
                                        .setResource(practitioner)
                                        .setDisplay(FHIRUtils.getDisplay(practitioner)));

        composition
                .setTitle("Diagnostic Report Document")
                .setEncounter(encounterReference)
                .setSubject(subjectReference);

        DiagnosticReport report = new DiagnosticReport();
        report.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);
        report.setCode(new CodeableConcept().setText(panelName));
        report.setSubject(subjectReference);

        observations
                .stream()
                .map(FHIRUtils::getReferenceToResource)
                .forEach(report::addResult);

        compositionSection
                .setTitle("# Diagnostic Report")
                .setCode(FHIRUtils.getDiagnosticType())
                .addEntry(FHIRUtils.getReferenceToResource(report));

        FHIRUtils.addToBundleEntry(bundle, composition, false);
        FHIRUtils.addToBundleEntry(bundle, practitioners, false);
        FHIRUtils.addToBundleEntry(bundle, patient, false);
        FHIRUtils.addToBundleEntry(bundle, encounterFHIR, false);
        FHIRUtils.addToBundleEntry(bundle, report, false);
        FHIRUtils.addToBundleEntry(bundle, observations, false);
    }
    private Composition initializeComposition(Date encounterTimestamp, String webURL) {
        Composition composition = new Composition();

        composition.setId(UUID.randomUUID().toString());
        composition.setDate(encounterTimestamp);
        composition.setIdentifier(FHIRUtils.getIdentifier(composition.getId(), webURL, "document"));
        composition.setStatus(Composition.CompositionStatus.FINAL);
        composition.setType(FHIRUtils.getDiagnosticType());
        composition.setTitle("Diagnostic Report");
        return composition;
    }
    public static FhirDiagnosticReport fromOpenMrsDiagnosticReport(OpenMrsDiagnosticReport openMrsDiagnosticReport,
                                                                   FHIRResourceMapper fhirResourceMapper) {
        Date encounterDatetime = openMrsDiagnosticReport.getEncounter().getEncounterDatetime();
        Encounter encounter = openMrsDiagnosticReport.getEncounter();
        List<Observation> observations = openMrsDiagnosticReport.getEncounter().getAllObs().stream()
                .map(fhirResourceMapper::mapToObs).collect(Collectors.toList());
        for (Observation o : observations) {
            String valueText = o.getValueStringType().getValueAsString();
            o.getValueStringType().setValueAsString("/document_images/" + valueText);
        }
        return new FhirDiagnosticReport(encounterDatetime, observations, encounter, encounter.getPatient());
    }

    public static FhirDiagnosticReport fromOpenMrsLabResults(OpenMrsLabResults labresult) {
        FhirDiagnosticReport report = new FhirDiagnosticReport(labresult.getEncounter().getEncounterDatetime(),
                labresult.getLabOrderResults().stream().map(FhirDiagnosticReport::mapToObsFromLabResult).collect(Collectors.toList()),
                labresult.getEncounter(),
                labresult.getPatient());
        report.setPanelName(labresult.getLabOrderResults().get(0).getPanelName());
        return report;
    }
    private static Observation mapToObsFromLabResult(  LabOrderResult result) {
        Observation obs = new Observation();
        obs.setCode(new CodeableConcept().setText( result.getTestName( )));
        try {
            Float f = Float.parseFloat(result.getResult());
            obs.setValue(new Quantity().setValue(f).setUnit(result.getTestUnitOfMeasurement()));
        } catch (NumberFormatException ex ) {
            obs.setValue(new StringType().setValue(result.getResult()));
        }
        obs.setStatus(Observation.ObservationStatus.FINAL);
        return obs;
    }
    public static List<FhirDiagnosticReport> fromLabResults( List<OpenMrsLabResults> labResults) {
        return labResults.stream().map(FhirDiagnosticReport::fromOpenMrsLabResults).collect(Collectors.toList());
    }
}
