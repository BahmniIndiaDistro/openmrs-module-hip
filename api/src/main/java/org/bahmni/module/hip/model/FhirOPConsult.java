package org.bahmni.module.hip.model;

import org.bahmni.module.hip.config.AbdmConfig;
import org.bahmni.module.hip.mapper.FHIRResourceMapper;
import org.bahmni.module.hip.utils.FHIRUtils;
import org.bahmni.module.hip.builder.OmrsObsDocumentTransformer;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.EncounterProvider;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Meta;

import java.util.*;
import java.util.stream.Collectors;

public class FhirOPConsult {
    private final List<Condition> chiefComplaints;
    private final List<Condition> medicalHistory;
    private final Date visitTimeStamp;
    private final Integer encounterID;
    private final Encounter encounter;
    private final List<Practitioner> practitioners;
    private final Patient patient;
    private final Reference patientReference;
    private final List<Observation> observations;
    private final List<MedicationRequest> medicationRequests;
    private final List<Medication> medications;
    private final List<Procedure> procedure;
    private final List<DocumentReference> patientDocuments;
    private final List<ServiceRequest> serviceRequest;
    private final List<Observation> otherObsverations;

    public FhirOPConsult(List<Condition> chiefComplaints,
                         List<Condition> medicalHistory, Date visitTimeStamp,
                         Integer encounterID,
                         Encounter encounter,
                         List<Practitioner> practitioners,
                         Patient patient,
                         Reference patientReference,
                         List<Observation> observations,
                         List<MedicationRequest> medicationRequests,
                         List<Medication> medications,
                         List<Procedure> procedure,
                         List<DocumentReference> patientDocuments,
                         List<ServiceRequest> serviceRequest,
                         List<Observation> otherObservations) {
        this.chiefComplaints = chiefComplaints;
        this.medicalHistory = medicalHistory;
        this.visitTimeStamp = visitTimeStamp;
        this.encounterID = encounterID;
        this.encounter = encounter;
        this.practitioners = practitioners;
        this.patient = patient;
        this.patientReference = patientReference;
        this.observations = observations;
        this.medicationRequests = medicationRequests;
        this.medications = medications;
        this.procedure = procedure;
        this.patientDocuments = patientDocuments;
        this.serviceRequest = serviceRequest;
        this.otherObsverations = otherObservations;
    }

    public Bundle bundleOPConsult(OrganizationContext orgContext) {
        String bundleId = String.format("OPR-%d", encounterID);
        Bundle bundle = FHIRUtils.createBundle(visitTimeStamp, bundleId, orgContext.getWebUrl());
        FHIRUtils.addToBundleEntry(bundle, compositionFrom(orgContext), false);
        FHIRUtils.addToBundleEntry(bundle, orgContext.getOrganization(), false);
        FHIRUtils.addToBundleEntry(bundle, patient, false);
        FHIRUtils.addToBundleEntry(bundle, practitioners, false);
        FHIRUtils.addToBundleEntry(bundle, encounter, false);
        FHIRUtils.addToBundleEntry(bundle, chiefComplaints, false);
        FHIRUtils.addToBundleEntry(bundle, medicalHistory, false);
        FHIRUtils.addToBundleEntry(bundle, observations, false);
        FHIRUtils.addToBundleEntry(bundle, medicationRequests, false);
        FHIRUtils.addToBundleEntry(bundle, medications, false);
        FHIRUtils.addToBundleEntry(bundle, serviceRequest, false);
        FHIRUtils.addToBundleEntry(bundle, otherObsverations, false);
        if (procedure != null) FHIRUtils.addToBundleEntry(bundle, procedure, false);
        if(patientDocuments != null) FHIRUtils.addToBundleEntry(bundle, patientDocuments, false);

        return bundle;
    }

    public static FhirOPConsult fromOpenMrsOPConsult(OpenMrsOPConsult openMrsOPConsult, FHIRResourceMapper fhirResourceMapper, AbdmConfig abdmConfig, OmrsObsDocumentTransformer patientDocumentTransformer) {
        Patient patient = fhirResourceMapper.mapToPatient(openMrsOPConsult.getPatient());
        Reference patientReference = FHIRUtils.getReferenceToResource(patient);
        Encounter encounter = fhirResourceMapper.mapToEncounter(openMrsOPConsult.getEncounter());
        encounter.getClass_().setDisplay("ambulatory");
        Date visitDatetime = openMrsOPConsult.getEncounter().getVisit().getStartDatetime();
        Integer encounterId = openMrsOPConsult.getEncounter().getId();
        List<MedicationRequest> medicationRequestsList = openMrsOPConsult.getDrugOrders().stream().
                map(fhirResourceMapper::mapToMedicationRequest).collect(Collectors.toList());
        List<Medication> medications = openMrsOPConsult.getDrugOrders().stream().map(fhirResourceMapper::mapToMedication).
                filter(medication -> !Objects.isNull(medication)).collect(Collectors.toList());
        List<Practitioner> practitioners = getPractitionersFrom(fhirResourceMapper, openMrsOPConsult.getEncounter().getEncounterProviders());
        List<Condition> fhirChiefComplaintConditionList = new ArrayList<>();
        for(int i=0;i<openMrsOPConsult.getChiefComplaintConditions().size();i++){
            fhirChiefComplaintConditionList.add(fhirResourceMapper.mapToCondition(openMrsOPConsult.getChiefComplaintConditions().get(i), patient));
        }
        List<Condition> fhirMedicalHistoryList = new ArrayList<>();
        for(int i=0;i<openMrsOPConsult.getMedicalHistoryConditions().size();i++){
            fhirMedicalHistoryList.add(fhirResourceMapper.mapToCondition(openMrsOPConsult.getMedicalHistoryConditions().get(i), patient));
        }
        List<Observation> fhirObservationList = openMrsOPConsult.getObservations().stream().
                    map(fhirResourceMapper::mapToObs).collect(Collectors.toList());
        List<Procedure> fhirProcedureList = new ArrayList<>();
        for(int i=0;i<openMrsOPConsult.getProcedure().size();i++){
            fhirProcedureList.add(fhirResourceMapper.mapToProcedure(encounter,openMrsOPConsult.getProcedure().get(i),abdmConfig.getProcedureAttributeConcepts()));
        }
        List<DocumentReference> patientDocuments = openMrsOPConsult.getPatientDocuments().stream().
                map(obs -> patientDocumentTransformer.transForm(obs, DocumentReference.class,AbdmConfig.HiTypeDocumentKind.OP_CONSULT))
                .filter(Objects::nonNull).collect(Collectors.toList());
        List<ServiceRequest> serviceRequest = openMrsOPConsult.getOrders().stream().
                map(fhirResourceMapper::mapToOrder).collect(Collectors.toList());
        List<Observation> otherObs = openMrsOPConsult.getOtherObs().stream().
                map(fhirResourceMapper::mapToObs).collect(Collectors.toList());

        return new FhirOPConsult(fhirChiefComplaintConditionList, fhirMedicalHistoryList, visitDatetime, encounterId, encounter, practitioners,
                patient, patientReference, fhirObservationList, medicationRequestsList, medications, fhirProcedureList, patientDocuments, serviceRequest, otherObs);
    }

    private Composition compositionFrom(OrganizationContext orgContext) {
        Composition composition = initializeComposition(visitTimeStamp, orgContext.getWebUrl());
        Meta meta = new Meta();
        CanonicalType profileCanonical = new CanonicalType("https://nrces.in/ndhm/fhir/r4/StructureDefinition/OPConsultRecord");
        List<CanonicalType> profileList = Collections.singletonList(profileCanonical);
        meta.setProfile(profileList);
        composition.setMeta(meta);
        composition
                .setEncounter(FHIRUtils.getReferenceToResource(encounter))
                .setSubject(patientReference)
                .setAuthor(Collections.singletonList(FHIRUtils.getReferenceToResource(orgContext.getOrganization(), "Organization")));

        if (patientDocuments.size() > 0) {
            Composition.SectionComponent patientDocumentsCompositionSection = composition.addSection();
            patientDocumentsCompositionSection
                    .setTitle("Patient Document")
                    .setCode(FHIRUtils.getPatientDocumentType());
            patientDocuments
                    .stream()
                    .map(FHIRUtils::getReferenceToResource)
                    .forEach(patientDocumentsCompositionSection::addEntry);
        }

        if (procedure.size() > 0) {
            Composition.SectionComponent procedureCompositionSection = composition.addSection();
            procedureCompositionSection
                    .setTitle("Procedure")
                    .setCode(FHIRUtils.getProcedureType());

            procedure
                    .stream()
                    .map(FHIRUtils::getReferenceToResource)
                    .forEach(procedureCompositionSection::addEntry);
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

        if (serviceRequest.size() > 0) {
            Composition.SectionComponent serviceRequestCompositionSection = composition.addSection();
            serviceRequestCompositionSection
                    .setTitle("Order")
                    .setCode(FHIRUtils.getOrdersType());
            serviceRequest.stream()
                    .map(FHIRUtils::getReferenceToResource)
                    .forEach(serviceRequestCompositionSection::addEntry);
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

        if(otherObsverations.size() > 0) {
            Composition.SectionComponent otherObservationCompositionSection = composition.addSection();
            otherObservationCompositionSection
                    .setTitle("Other Observations")
                    .setCode(FHIRUtils.getOtherObservationType());
            otherObsverations
                    .stream()
                    .map(FHIRUtils::getReferenceToResource)
                    .forEach(otherObservationCompositionSection::addEntry);
        }

        return composition;
    }

    private Composition initializeComposition(Date visitTimeStamp, String webURL) {
        Composition composition = new Composition();
        composition.setId(UUID.randomUUID().toString());
        composition.setDate(visitTimeStamp);
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
