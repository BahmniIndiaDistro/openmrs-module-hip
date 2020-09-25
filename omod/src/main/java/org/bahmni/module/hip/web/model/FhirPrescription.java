package org.bahmni.module.hip.web.model;

import lombok.Getter;
import org.bahmni.module.hip.web.service.FHIRResourceMapper;
import org.bahmni.module.hip.web.service.FHIRUtils;
import org.hl7.fhir.r4.model.*;
import org.openmrs.DrugOrder;
import org.openmrs.EncounterProvider;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public class FhirPrescription {

    private Date encounterTimestamp;
    private Integer encounterID;
    private Encounter encounter;
    private List<Practitioner> practitioners;
    private Patient patient;
    private Reference patientReference;
    private List<Medication> medications;
    private List<MedicationRequest> medicationRequests;

    private FhirPrescription(Date encounterTimestamp, Integer encounterID, Encounter encounter, List<Practitioner> practitioners, Patient patient, Reference patientReference, List<Medication> medications, List<MedicationRequest> medicationRequests) {
        this.encounterTimestamp = encounterTimestamp;
        this.encounterID = encounterID;
        this.encounter = encounter;
        this.practitioners = practitioners;
        this.patient = patient;
        this.patientReference = patientReference;
        this.medications = medications;
        this.medicationRequests = medicationRequests;
    }

    public static FhirPrescription from(OpenMrsPrescription openMrsPrescription) {

        Date encounterDatetime = openMrsPrescription.getEncounter().getEncounterDatetime();
        Integer encounterId = openMrsPrescription.getEncounter().getId();
        Patient patient = FHIRResourceMapper.mapToPatient(openMrsPrescription.getPatient());
        Reference patientReference = FHIRUtils.getReferenceToResource(patient);
        Encounter encounter = encounterFrom(openMrsPrescription.getEncounter(), patientReference);
        List<Practitioner> practitioners = getPractitionersFrom(openMrsPrescription.getEncounterProviders());
        List<MedicationRequest> medicationRequests = medicationRequestsFor(openMrsPrescription.getDrugOrders(), patientReference, practitioners.get(0));
        List<Medication> medications = medicationsFor(openMrsPrescription.getDrugOrders());

        return new FhirPrescription(encounterDatetime, encounterId, encounter, practitioners, patient, patientReference, medications, medicationRequests);
    }

    public Bundle bundle(String webUrl){
        String bundleID = String.format("PR-%d", encounterID);
        Bundle bundle = FHIRUtils.createBundle(encounterTimestamp, bundleID, webUrl);

        FHIRUtils.addToBundleEntry(bundle, compositionFrom(webUrl), false);
        FHIRUtils.addToBundleEntry(bundle, practitioners, false);
        FHIRUtils.addToBundleEntry(bundle, patient, false);
        FHIRUtils.addToBundleEntry(bundle, encounter, false);
        FHIRUtils.addToBundleEntry(bundle, medications, false);
        FHIRUtils.addToBundleEntry(bundle, medicationRequests, false);
        return bundle;
    }

    private Composition compositionFrom(String webURL){
        Composition composition = initializeComposition(encounterTimestamp, webURL);
        Composition.SectionComponent compositionSection = composition.addSection();

        practitioners
                .forEach(practitioner -> composition
                        .addAuthor().setResource(practitioner).setDisplay(FHIRUtils.getDisplay(practitioner)));

        composition
                .setEncounter(FHIRUtils.getReferenceToResource(encounter))
                .setSubject(patientReference);

        compositionSection
                .setTitle("OPD Prescription")
                .setCode(FHIRUtils.getPrescriptionType());

        medicationRequests
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
        composition.setType(FHIRUtils.getPrescriptionType());
        composition.setTitle("Prescription");
        return composition;
    }

    private static Encounter encounterFrom(org.openmrs.Encounter openMRSEncounter, Reference patientReference) {
        return FHIRResourceMapper
                .mapToEncounter(openMRSEncounter)
                .setSubject(patientReference);
    }

    private static List<MedicationRequest> medicationRequestsFor(
            DrugOrders drugOrders,
            Reference patientReference,
            Practitioner practitioner) {
        return drugOrders
                .stream()
                .map(drugOrder -> {
                    Medication medication = FHIRResourceMapper.mapToMedication(drugOrder);
                    return FHIRResourceMapper.mapToMedicationRequest(
                            drugOrder,
                            patientReference,
                            practitioner,
                            medication
                    );
                })
                .collect(Collectors.toList());
    }

    private static List<Medication> medicationsFor(DrugOrders drugOrders) {
        return drugOrders
                .stream()
                .map(FHIRResourceMapper::mapToMedication)
                .filter(medication -> !Objects.isNull(medication))
                .collect(Collectors.toList());
    }

    private static List<Practitioner> getPractitionersFrom(Set<EncounterProvider> encounterProviders) {
        return encounterProviders
                .stream()
                .map(FHIRResourceMapper::mapToPractitioner)
                .collect(Collectors.toList());
    }
}