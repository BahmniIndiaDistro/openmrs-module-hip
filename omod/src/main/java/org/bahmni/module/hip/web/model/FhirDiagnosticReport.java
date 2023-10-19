package org.bahmni.module.hip.web.model;

import org.bahmni.module.hip.web.service.FHIRResourceMapper;
import org.bahmni.module.hip.web.service.FHIRUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.EncounterProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


public class FhirDiagnosticReport {
    private final List<DiagnosticReport> diagnosticReports;
    private final Date visitTimestamp;
    private final Integer encounterID;
    private final Encounter encounter;
    private final List<Practitioner> practitioners;
    private final Patient patient;
    private final Reference patientReference;
    private final List<Observation> obs;

    private FhirDiagnosticReport(Date visitDateTime,
                                 List<DiagnosticReport> diagnosticReports,
                                 Integer encounterID,
                                 Encounter encounter,
                                 List<Practitioner> practitioners,
                                 Patient patient,
                                 Reference patientReference,
                                 List<Observation> obs) {
        this.visitTimestamp = visitDateTime;
        this.diagnosticReports = diagnosticReports;
        this.encounterID = encounterID;
        this.encounter = encounter;
        this.practitioners = practitioners;
        this.patient = patient;
        this.patientReference = patientReference;
        this.obs = obs;
    }


    public Bundle bundleDiagnosticReport(OrganizationContext orgContext) {
        String bundleID = String.format("PR-%d", encounterID);
        Bundle bundle = FHIRUtils.createBundle(visitTimestamp, bundleID, orgContext.getWebUrl());

        FHIRUtils.addToBundleEntry(bundle, compositionFrom(orgContext), false);
        FHIRUtils.addToBundleEntry(bundle, orgContext.getOrganization(), false);
        FHIRUtils.addToBundleEntry(bundle, practitioners, false);
        FHIRUtils.addToBundleEntry(bundle, patient, false);
        FHIRUtils.addToBundleEntry(bundle, obs, false);
        FHIRUtils.addToBundleEntry(bundle, encounter, false);
        FHIRUtils.addToBundleEntry(bundle, diagnosticReports, false);
        return bundle;
    }

    public static FhirDiagnosticReport fromOpenMrsDiagnosticReport(OpenMrsDiagnosticReport openMrsDiagnosticReport,
                                                                   FHIRResourceMapper fhirResourceMapper) {

        Patient patient = fhirResourceMapper.mapToPatient(openMrsDiagnosticReport.getPatient());
        Reference patientReference = FHIRUtils.getReferenceToResource(patient);
        Encounter encounter = fhirResourceMapper.mapToEncounter(openMrsDiagnosticReport.getEncounter());
        encounter.getClass_().setDisplay("Diagnostic Report");
        Date visitDateTime = openMrsDiagnosticReport.getEncounter().getVisit().getStartDatetime();
        Integer encounterId = openMrsDiagnosticReport.getEncounter().getId();
        List<Practitioner> practitioners = getPractitionersFrom(fhirResourceMapper, openMrsDiagnosticReport.getEncounterProviders());
        List<DiagnosticReport> diagnosticReports = openMrsDiagnosticReport.getObs().stream()
                .map(fhirResourceMapper::mapToDiagnosticReport).collect(Collectors.toList());
        List<DiagnosticReport> diagnosticReportList = diagnosticReports.stream()
                .peek(diagnosticReport -> {
                    diagnosticReport.setResultsInterpreter(practitioners.stream().map(FHIRUtils::getReferenceToResource)
                            .collect(Collectors.toList()));
                    diagnosticReport.setSubject(FHIRUtils.getReferenceToResource(patient));
                }).collect(Collectors.toList());
        return new FhirDiagnosticReport(visitDateTime, diagnosticReportList, encounterId, encounter, practitioners,
                patient, patientReference,new ArrayList<>());
    }

    private Composition compositionFrom(OrganizationContext orgContext) {
        Composition composition = initializeComposition(visitTimestamp, orgContext.getWebUrl());
        Meta meta = new Meta();
        CanonicalType profileCanonical = new CanonicalType("https://nrces.in/ndhm/fhir/r4/StructureDefinition/DiagnosticReportRecord");
        List<CanonicalType> profileList = Collections.singletonList(profileCanonical);
        meta.setProfile(profileList);
        composition.setMeta(meta);
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

        diagnosticReports
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
