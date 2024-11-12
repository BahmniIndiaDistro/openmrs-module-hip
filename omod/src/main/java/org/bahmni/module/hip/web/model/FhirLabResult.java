package org.bahmni.module.hip.web.model;

import org.bahmni.module.bahmnicore.service.OrderService;
import org.bahmni.module.hip.Config;
import org.bahmni.module.hip.web.service.FHIRResourceMapper;
import org.bahmni.module.hip.web.service.FHIRUtils;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.openmrs.Obs;
import org.openmrs.module.bahmniemrapi.laborder.contract.LabOrderResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;


public class FhirLabResult {

    private final Patient patient;
    private final Encounter encounter;
    private final Date visitTime;
    private final List<DiagnosticReport>  report;
    private final List<Observation> results;
    private final List<Practitioner> practitioners;

    public FhirLabResult(Patient patient, Encounter encounter, Date visitTime, List<DiagnosticReport> report, List<Observation> results, List<Practitioner> practitioners) {
        this.patient = patient;
        this.encounter = encounter;
        this.visitTime = visitTime;
        this.report = report;
        this.results = results;
        this.practitioners = practitioners;
    }

    public Bundle bundleLabResults (OrganizationContext organizationContext, FHIRResourceMapper fhirResourceMapper) {
        //There maybe several diagnosticReport associated with an encounter
        String bundleId = createReportId();
        Bundle bundle = FHIRUtils.createBundle(visitTime, bundleId, organizationContext.getWebUrl());
        FHIRUtils.addToBundleEntry(bundle, compositionFrom(organizationContext), false);
        FHIRUtils.addToBundleEntry(bundle, organizationContext.getOrganization(), false);
        FHIRUtils.addToBundleEntry(bundle, encounter, false);
        FHIRUtils.addToBundleEntry(bundle, practitioners, false);
        FHIRUtils.addToBundleEntry(bundle, patient, false);
        //Any reasons of packing multiple DiagnosticReport together?
        FHIRUtils.addToBundleEntry(bundle, report, false);
        FHIRUtils.addToBundleEntry(bundle, results, false);
        return bundle;
    }

    private String createReportId() {
        if (report != null && !report.isEmpty()) {
            return String.format("DR-%s-%s", encounter.getId(), report.get(0).getId());
        }
        return String.format("DR-%s-%d-%d", encounter.getId(), System.currentTimeMillis(), (new Random().nextInt()));
    }

    public static FhirLabResult fromOpenMrsLabResults(OpenMrsLabResults labresult, FHIRResourceMapper fhirResourceMapper) {
        List<DiagnosticReport> reportList = new ArrayList<>();
        List<Practitioner> practitioners = labresult.getEncounterProviders().stream().map(fhirResourceMapper::mapToPractitioner).collect(Collectors.toList());
        Patient patient = fhirResourceMapper.mapToPatient(labresult.getPatient());
        List<Observation> results = new ArrayList<>();

        if(labresult.getObservation() != null) {
            for (Obs obs : labresult.getObservation()) {
                String testName = obs.getObsGroup().getConcept().getName().getName();
                DiagnosticReport reports = map(UUID.randomUUID().toString(), obs,testName, patient,practitioners);
                reportList.add(reports);
            }
        }
        if(labresult.getLabResults() != null) {
            Map<String, List<Map.Entry<LabOrderResult, Obs>>> labOrderResultObsOrdersMap =
                    labresult.getLabResults().entrySet().stream()
                            .collect(Collectors.groupingBy(entry -> entry.getKey().getOrderUuid()));

            for(Map.Entry<String, List<Map.Entry<LabOrderResult, Obs>>> labOrder: labOrderResultObsOrdersMap.entrySet()){
                    String testName = labOrder.getValue().get(0).getKey().getPanelName();
                    if (testName == null) {
                        testName = labOrder.getValue().get(0).getKey().getTestName();
                    }
                    DiagnosticReport reports = map(labOrder.getKey(),null,testName,patient,practitioners);
                    labOrder.getValue().forEach(entry -> {
                        FhirLabResult.mapToObsFromLabResult(entry.getKey(), patient, reports, results);
                    });
                    reportList.add(reports);
            }

        }
        Encounter encounter = fhirResourceMapper.mapToEncounter(labresult.getEncounter());
        encounter.getClass_().setDisplay("ambulatory");

        return new FhirLabResult(fhirResourceMapper.mapToPatient(labresult.getPatient()),
                   encounter, labresult.getEncounter().getEncounterDatetime(), reportList, results, practitioners);
    }

    private static DiagnosticReport map(String orderUuid, Obs obs, String testName, Patient patient,List<Practitioner> practitioners ){
        DiagnosticReport reports = new DiagnosticReport();
        reports.setCode(new CodeableConcept().setText(testName).addCoding(new Coding().setDisplay(testName)));
            try {
                reports.setPresentedForm(getAttachments(obs, testName));
            } catch (IOException e) {
                e.printStackTrace();
            }
        reports.setId(orderUuid);
        reports.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);
        reports.setSubject(FHIRUtils.getReferenceToResource(patient));
        reports.setResultsInterpreter(practitioners.stream().map(FHIRUtils::getReferenceToResource).collect(Collectors.toList()));
        return reports;
    }

    private static void mapToObsFromLabResult(LabOrderResult result, Patient patient, DiagnosticReport report, List<Observation> observations) {
        if(result.getResult() != null) {
            Observation obs = new Observation();
            obs.setId(result.getTestUuid());
            obs.setCode(new CodeableConcept().setText(result.getTestName()));
            try {
                float f = result.getResult() != null ? Float.parseFloat(result.getResult()) : (float) 0;
                obs.setValue(new Quantity().setValue(f).setUnit(result.getTestUnitOfMeasurement()));
            } catch (NumberFormatException | NullPointerException ex) {
                obs.setValue(new StringType().setValue(result.getResult()));
            }
            obs.setStatus(Observation.ObservationStatus.FINAL);
            obs.setEffective(new DateTimeType(result.getResultDateTime()));
            report.addResult(FHIRUtils.getReferenceToResource(obs));
            observations.add(obs);
        }
    }

    private Composition compositionFrom(OrganizationContext orgContext) {
        Composition composition = initializeComposition(visitTime, orgContext.getWebUrl());
        Composition.SectionComponent compositionSection = composition.addSection();
        Reference patientReference = FHIRUtils.getReferenceToResource(patient);

        Meta meta = new Meta();
        CanonicalType profileCanonical = new CanonicalType("https://nrces.in/ndhm/fhir/r4/StructureDefinition/DiagnosticReportRecord");
        List<CanonicalType> profileList = Collections.singletonList(profileCanonical);
        meta.setProfile(profileList);
        composition.setMeta(meta);
        composition
                .setEncounter(FHIRUtils.getReferenceToResource(encounter))
                .setSubject(patientReference)
                .setAuthor(Collections.singletonList(FHIRUtils.getReferenceToResource(orgContext.getOrganization(), "Organization")));

        compositionSection
                .setTitle("Diagnostic Report")
                .setCode(FHIRUtils.getDiagnosticReportType());

        report.stream()
                .map(FHIRUtils::getReferenceToResource)
                .forEach(compositionSection::addEntry);


        return composition;
    }

    private Composition initializeComposition(Date visitTimestamp, String webURL) {
        Composition composition = new Composition();
        composition.setId(UUID.randomUUID().toString());
        composition.setDate(visitTimestamp);
        composition.setIdentifier(FHIRUtils.getIdentifier(composition.getId(), webURL, "document"));
        composition.setStatus(Composition.CompositionStatus.FINAL);
        composition.setType(FHIRUtils.getDiagnosticReportType());
        composition.setTitle("Diagnostic Report");
        return composition;
    }

    private static List<Attachment> getAttachments(Obs obs,String testNmae) throws IOException {
        List<Attachment> attachments = new ArrayList<>();

        if(obs != null) {
            Attachment attachment = new Attachment();
            attachment.setContentType(FHIRUtils.getTypeOfTheObsDocument(obs.getValueText()));
            byte[] fileContent = Files.readAllBytes(new File(Config.LAB_UPLOADS_PATH.getValue() + obs.getValueText()).toPath());
            attachment.setData(fileContent);
            attachment.setTitle("LAB REPORT : " + testNmae);
            attachments.add(attachment);
        }

        return attachments;
    }
}
