package org.bahmni.module.hip.web.model;

import org.bahmni.module.hip.web.service.FHIRResourceMapper;
import org.bahmni.module.hip.web.service.FHIRUtils;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Condition;
import org.openmrs.EncounterProvider;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Objects;
import java.util.stream.Collectors;

public class FhirDischargeSummary {

    private final Date encounterTimestamp;
    private final Integer encounterID;
    private final Encounter encounter;
    private final List<Practitioner> practitioners;
    private final List<Condition> chiefComplaints;
    private final Patient patient;
    private final List<MedicationRequest> medicationRequests;
    private final List<Medication> medications;
    private final Reference patientReference;
    private final List<CarePlan> carePlan;

    public FhirDischargeSummary(Integer encounterID,
                                Encounter encounter,
                                Date encounterTimestamp,
                                List<Practitioner> practitioners,
                                Reference patientReference,
                                List<Condition> chiefComplaints,
                                List<MedicationRequest> medicationRequests,
                                List<Medication> medications,
                                Patient patient,
                                List<CarePlan> carePlan){
        this.encounterTimestamp = encounterTimestamp;
        this.encounterID = encounterID;
        this.encounter = encounter;
        this.chiefComplaints = chiefComplaints;
        this.practitioners = practitioners;
        this.medicationRequests = medicationRequests;
        this.medications = medications;
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
        List<Condition> chiefComplaints = openMrsDischargeSummary.getChiefComplaints().stream().
                map(fhirResourceMapper::mapToCondition).collect(Collectors.toList());
        List<MedicationRequest> medicationRequestsList = openMrsDischargeSummary.getDrugOrders().stream().
                map(fhirResourceMapper::mapToMedicationRequest).collect(Collectors.toList());
        List<Medication> medications = openMrsDischargeSummary.getDrugOrders().stream().map(fhirResourceMapper::mapToMedication).
                filter(medication -> !Objects.isNull(medication)).collect(Collectors.toList());

        return new FhirDischargeSummary(encounterId, encounter, encounterDatetime, practitioners, patientReference, chiefComplaints, medicationRequestsList, medications, patient, carePlans);
    }

    public Bundle bundleDischargeSummary(String webUrl){
        String bundleID = String.format("PR-%d", encounterID);
        Bundle bundle = FHIRUtils.createBundle(encounterTimestamp, bundleID, webUrl);
        FHIRUtils.addToBundleEntry(bundle, compositionFrom(webUrl), false);
        FHIRUtils.addToBundleEntry(bundle, practitioners, false);
        FHIRUtils.addToBundleEntry(bundle, medicationRequests, false);
        FHIRUtils.addToBundleEntry(bundle, chiefComplaints, false);
        FHIRUtils.addToBundleEntry(bundle, medications, false);
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

        if(medicationRequests.size() > 0){
            Composition.SectionComponent medicationRequestsCompositionSection = composition.addSection();
            medicationRequestsCompositionSection
                    .setTitle("Medication request")
                    .setCode(FHIRUtils.getPrescriptionType());
            medicationRequests
                    .stream()
                    .map(FHIRUtils::getReferenceToResource)
                    .forEach(medicationRequestsCompositionSection::addEntry);
        }

        if (chiefComplaints.size() > 0){
            Composition.SectionComponent chiefComplaintsCompositionSection = composition.addSection();
            chiefComplaintsCompositionSection
                    .setTitle("Chief complaint")
                    .setCode(FHIRUtils.getChiefComplaintType());
            chiefComplaints
                    .stream()
                    .map(FHIRUtils::getReferenceToResource)
                    .forEach(chiefComplaintsCompositionSection::addEntry);
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
