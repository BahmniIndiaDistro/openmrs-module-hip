package org.bahmni.module.hip.web.model;

import lombok.Getter;
import org.bahmni.module.hip.Config;
import org.bahmni.module.hip.web.service.FHIRResourceMapper;
import org.bahmni.module.hip.web.service.FHIRUtils;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.Concept;
import org.openmrs.EncounterProvider;
import org.openmrs.Obs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class FhirPrescription {
    private final Date visitTimeStamp;
    private final Integer encounterID;
    private final Encounter encounter;
    private final List<Practitioner> practitioners;
    private final Patient patient;
    private final Reference patientReference;
    private final List<Medication> medications;
    private final List<MedicationRequest> medicationRequests;
    private final List<Binary> documentObs;

    private FhirPrescription(Date visitTimeStamp, Integer encounterID, Encounter encounter,
                             List<Practitioner> practitioners, Patient patient,
                             Reference patientReference, List<Medication> medications,
                             List<MedicationRequest> medicationRequests, List<Binary> documentObs) {
        this.visitTimeStamp = visitTimeStamp;
        this.encounterID = encounterID;
        this.encounter = encounter;
        this.practitioners = practitioners;
        this.patient = patient;
        this.patientReference = patientReference;
        this.medications = medications;
        this.medicationRequests = medicationRequests;
        this.documentObs = documentObs;
    }

    public static FhirPrescription from(OpenMrsPrescription openMrsPrescription, FHIRResourceMapper fhirResourceMapper, Concept prescriptionDocumentConcept) {

        Date encounterDatetime = openMrsPrescription.getEncounter().getVisit().getStartDatetime();
        Integer encounterId = openMrsPrescription.getEncounter().getId();
        Patient patient = fhirResourceMapper.mapToPatient(openMrsPrescription.getPatient());
        Reference patientReference = FHIRUtils.getReferenceToResource(patient);
        Encounter encounter = fhirResourceMapper.mapToEncounter(openMrsPrescription.getEncounter());
        List<Practitioner> practitioners = getPractitionersFrom(fhirResourceMapper, openMrsPrescription.getEncounterProviders());
        List<MedicationRequest> medicationRequests = medicationRequestsFor(fhirResourceMapper, openMrsPrescription.getDrugOrders());
        List<Medication> medications = medicationsFor(fhirResourceMapper, openMrsPrescription.getDrugOrders());
        List<Binary> prescriptionDocs = (prescriptionDocumentConcept == null)
                ? Collections.emptyList()
                : openMrsPrescription.getEncounter()
                    .getAllFlattenedObs(false)
                    .stream()
                    .filter(obs -> obs.getConcept().getUuid().equals(prescriptionDocumentConcept.getUuid()))
                    .map(FhirPrescription::getPrescriptionDocument)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());


        return new FhirPrescription(encounterDatetime, encounterId, encounter, practitioners, patient, patientReference, medications, medicationRequests, prescriptionDocs);
    }

    private static Binary getPrescriptionDocument(Obs obs) {
        byte[] fileContent;
        try {
            fileContent = Files.readAllBytes(new File(Config.PATIENT_DOCUMENTS_PATH.getValue() + obs.getValueComplex()).toPath());
        } catch (IOException e) {
            System.out.println(e);
            return null;
        }
        Binary binary = new Binary();
        binary
           .setContentType(FHIRUtils.getTypeOfTheObsDocument(obs.getValueText()))
           .setData(fileContent)
           .setId(UUID.randomUUID().toString());
        return binary;
    }

    public Bundle bundle(OrganizationContext orgContext) {
        String bundleID = String.format("PR-%d", encounterID);
        Bundle bundle = FHIRUtils.createBundle(visitTimeStamp, bundleID, orgContext.getWebUrl());
        FHIRUtils.addToBundleEntry(bundle, compositionFrom(orgContext), false);
        FHIRUtils.addToBundleEntry(bundle, practitioners, false);
        FHIRUtils.addToBundleEntry(bundle, patient, false);
        FHIRUtils.addToBundleEntry(bundle, encounter, false);
        FHIRUtils.addToBundleEntry(bundle, medications, false);
        FHIRUtils.addToBundleEntry(bundle, medicationRequests, false);
        if (!documentObs.isEmpty()) {
            FHIRUtils.addToBundleEntry(bundle, documentObs, false);
        }
        return bundle;
    }

    private Composition compositionFrom(OrganizationContext orgContext) {
        Composition composition = initializeComposition(visitTimeStamp, orgContext.getWebUrl());
        Composition.SectionComponent compositionSection = composition.addSection();

        practitioners
                .forEach(practitioner -> composition
                        .addAuthor().setResource(practitioner).setDisplay(FHIRUtils.getDisplay(practitioner)));

        composition
                .setEncounter(FHIRUtils.getReferenceToResource(encounter))
                .setSubject(patientReference)
                .setAuthor(Collections.singletonList(FHIRUtils.getReferenceToResource(orgContext.getOrganization(), "Organization")));

        compositionSection
                .setTitle("OPD Prescription")
                .setCode(FHIRUtils.getPrescriptionType());

        medicationRequests
                .stream()
                .map(FHIRUtils::getReferenceToResource)
                .forEach(compositionSection::addEntry);
        documentObs
                .stream()
                .map(FHIRUtils::getReferenceToResource)
                .forEach(compositionSection::addEntry);

        return composition;
    }

    private Composition initializeComposition(Date visitTimeStamp, String webURL) {
        Composition composition = new Composition();

        composition.setId(UUID.randomUUID().toString());
        composition.setDate(visitTimeStamp);
        composition.setIdentifier(FHIRUtils.getIdentifier(composition.getId(), webURL, "document"));
        composition.setStatus(Composition.CompositionStatus.FINAL);
        composition.setType(FHIRUtils.getPrescriptionType());
        composition.setTitle("Prescription");
        return composition;
    }

    private static List<MedicationRequest> medicationRequestsFor(FHIRResourceMapper fhirResourceMapper, DrugOrders drugOrders) {
        return drugOrders
                .stream()
                .map(fhirResourceMapper::mapToMedicationRequest)
                .collect(Collectors.toList());
    }

    private static List<Medication> medicationsFor(FHIRResourceMapper fhirResourceMapper, DrugOrders drugOrders) {
        return drugOrders
                .stream()
                .map(fhirResourceMapper::mapToMedication)
                .filter(medication -> !Objects.isNull(medication))
                .collect(Collectors.toList());
    }

    private static List<Practitioner> getPractitionersFrom(FHIRResourceMapper fhirResourceMapper, Set<EncounterProvider> encounterProviders) {
        return encounterProviders
                .stream()
                .map(fhirResourceMapper::mapToPractitioner)
                .collect(Collectors.toList());
    }
}
