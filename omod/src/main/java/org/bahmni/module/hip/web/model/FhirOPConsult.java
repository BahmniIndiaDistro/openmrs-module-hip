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
    private final List<Condition> chiefComplaints;
    private final List<Condition> medicalHistory;
    private final Date encounterTimestamp;
    private final Integer encounterID;
    private final Encounter encounter;
    private final List<Practitioner> practitioners;
    private final Patient patient;
    private final Reference patientReference;
    private final List<Observation> observations;
    private final List<MedicationRequest> medicationRequests;
    private final Procedure procedure;

    public FhirOPConsult(List<Condition> chiefComplaints,
                         List<Condition> medicalHistory, Date encounterTimestamp,
                         Integer encounterID,
                         Encounter encounter,
                         List<Practitioner> practitioners,
                         Patient patient,
                         Reference patientReference,
                         List<Observation> observations,
                         List<MedicationRequest> medicationRequests, Procedure procedure) {
        this.chiefComplaints = chiefComplaints;
        this.medicalHistory = medicalHistory;
        this.encounterTimestamp = encounterTimestamp;
        this.encounterID = encounterID;
        this.encounter = encounter;
        this.practitioners = practitioners;
        this.patient = patient;
        this.patientReference = patientReference;
        this.observations = observations;
        this.medicationRequests = medicationRequests;
        this.procedure = procedure;
    }

    public Bundle bundleOPConsult(String webUrl) {
        String bundleID = String.format("PR-%d", encounterID);
        Bundle bundle = FHIRUtils.createBundle(encounterTimestamp, bundleID, webUrl);
        FHIRUtils.addToBundleEntry(bundle, compositionFrom(webUrl), false);
        FHIRUtils.addToBundleEntry(bundle, practitioners, false);
        FHIRUtils.addToBundleEntry(bundle, patient, false);
        FHIRUtils.addToBundleEntry(bundle, encounter, false);
        FHIRUtils.addToBundleEntry(bundle, chiefComplaints, false);
        FHIRUtils.addToBundleEntry(bundle, medicalHistory, false);
        FHIRUtils.addToBundleEntry(bundle, observations, false);
        FHIRUtils.addToBundleEntry(bundle, medicationRequests, false);
        FHIRUtils.addToBundleEntry(bundle, procedure, false);
        return bundle;
    }

    public static FhirOPConsult fromOpenMrsOPConsult(OpenMrsOPConsult openMrsOPConsult, FHIRResourceMapper fhirResourceMapper) {
        Patient patient = fhirResourceMapper.mapToPatient(openMrsOPConsult.getPatient());
        Reference patientReference = FHIRUtils.getReferenceToResource(patient);
        Encounter encounter = fhirResourceMapper.mapToEncounter(openMrsOPConsult.getEncounter());
        Date encounterDatetime = openMrsOPConsult.getEncounter().getEncounterDatetime();
        Integer encounterId = openMrsOPConsult.getEncounter().getId();
        List<MedicationRequest> medicationRequestsList = openMrsOPConsult.getDrugOrders().stream().map(fhirResourceMapper::mapToMedicationRequest).collect(Collectors.toList());
        List<Practitioner> practitioners = getPractitionersFrom(fhirResourceMapper, openMrsOPConsult.getEncounter().getEncounterProviders());
        List<Condition> fhirChiefComplaintConditionList = openMrsOPConsult.getChiefComplaintConditions().stream().
                    map(fhirResourceMapper::mapToCondition).collect(Collectors.toList());
        List<Condition> fhirMedicalHistoryList = openMrsOPConsult.getMedicalHistoryConditions().stream().
                    map(fhirResourceMapper::mapToCondition).collect(Collectors.toList());
        List<Observation> fhirObservationList = openMrsOPConsult.getObservations().stream().
                    map(fhirResourceMapper::mapToObs).collect(Collectors.toList());
        Procedure procedure = openMrsOPConsult.getProcedure() != null ?
                fhirResourceMapper.mapToProcedure(openMrsOPConsult.getProcedure()) : null;
        return new FhirOPConsult(fhirChiefComplaintConditionList, fhirMedicalHistoryList,
                encounterDatetime, encounterId, encounter, practitioners, patient, patientReference, fhirObservationList, medicationRequestsList, procedure);
    }

    private Composition compositionFrom(String webURL) {
        Composition composition = initializeComposition(encounterTimestamp, webURL);
        composition
                .setEncounter(FHIRUtils.getReferenceToResource(encounter))
                .setSubject(patientReference);

        if (procedure != null) {
            Composition.SectionComponent procedureCompositionSection = composition.addSection();
            procedureCompositionSection
                    .setTitle("Procedure")
                    .setCode(FHIRUtils.getProcedureType());

            procedureCompositionSection.addEntry(FHIRUtils.getReferenceToResource(procedure));
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

        if (medicalHistory.size() > 0) {
            Composition.SectionComponent medicalHistoryCompositionSection = composition.addSection();
            medicalHistoryCompositionSection
                    .setTitle("Medical history")
                    .setCode(FHIRUtils.getMedicalHistoryType());
            medicalHistory
                    .stream()
                    .map(FHIRUtils::getReferenceToResource)
                    .forEach(medicalHistoryCompositionSection::addEntry);
        }

        if (observations.size() > 0) {
            Composition.SectionComponent physicalExaminationsCompositionSection = composition.addSection();
            physicalExaminationsCompositionSection
                    .setTitle("Physical examination")
                    .setCode(FHIRUtils.getPhysicalExaminationType());
            observations
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
        composition.setTitle("OP Consultation Document");
        return composition;
    }

    private static List<Practitioner> getPractitionersFrom(FHIRResourceMapper fhirResourceMapper, Set<EncounterProvider> encounterProviders) {
        return encounterProviders
                .stream()
                .map(fhirResourceMapper::mapToPractitioner)
                .collect(Collectors.toList());
    }

}
