package org.bahmni.module.hip.model;

import org.bahmni.module.hip.config.AbdmConfig;
import org.bahmni.module.hip.mapper.FHIRResourceMapper;
import org.bahmni.module.hip.utils.FHIRUtils;
import org.bahmni.module.hip.builder.OmrsObsDocumentTransformer;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.EncounterProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class FhirDischargeSummary {

    private final Date visitTimestamp;
    private final Integer encounterID;
    private final Encounter encounter;
    private final List<Practitioner> practitioners;
    private final List<Condition> chiefComplaints;
    private final Patient patient;
    private final List<MedicationRequest> medicationRequests;
    private final List<Medication> medications;
    private final List<Condition> medicalHistory;
    private final Reference patientReference;
    private final List<Observation> observations;
    private final List<CarePlan> carePlan;
    private final List<DocumentReference> patientDocuments;
    private final List<Procedure> procedures;
    private final List<ServiceRequest> serviceRequest;

    public FhirDischargeSummary(Integer encounterID,
                                Encounter encounter,
                                Date visitTimestamp,
                                List<Practitioner> practitioners,
                                Reference patientReference,
                                List<Condition> chiefComplaints,
                                List<MedicationRequest> medicationRequests,
                                List<Medication> medications,
                                List<Condition> medicalHistory,
                                Patient patient,
                                List<CarePlan> carePlan,
                                List<Observation> observations,
                                List<DocumentReference> patientDocuments,
                                List<Procedure> procedures,
                                List<ServiceRequest> serviceRequest){
        this.visitTimestamp = visitTimestamp;
        this.encounterID = encounterID;
        this.encounter = encounter;
        this.chiefComplaints = chiefComplaints;
        this.practitioners = practitioners;
        this.medicationRequests = medicationRequests;
        this.medications = medications;
        this.medicalHistory = medicalHistory;
        this.patient = patient;
        this.patientReference = patientReference;
        this.carePlan = carePlan;
        this.observations = observations;
        this.patientDocuments = patientDocuments;
        this.procedures = procedures;
        this.serviceRequest = serviceRequest;
    }
    public static FhirDischargeSummary fromOpenMrsDischargeSummary(OpenMrsDischargeSummary openMrsDischargeSummary, FHIRResourceMapper fhirResourceMapper,
                                                                   AbdmConfig abdmConfig, OmrsObsDocumentTransformer omrsObsDocumentTransformer) {
        Patient patient = fhirResourceMapper.mapToPatient(openMrsDischargeSummary.getPatient());
        Reference patientReference = FHIRUtils.getReferenceToResource(patient);
        Encounter encounter = fhirResourceMapper.mapToEncounter(openMrsDischargeSummary.getEncounter());
        encounter.getClass_().setDisplay("ambulatory");
        Date visitDatetime = openMrsDischargeSummary.getEncounter().getVisit().getStartDatetime();
        Integer encounterId = openMrsDischargeSummary.getEncounter().getId();
        List<Practitioner> practitioners = getPractitionersFrom(fhirResourceMapper, openMrsDischargeSummary.getEncounter().getEncounterProviders());
        List<CarePlan> carePlans = openMrsDischargeSummary.getCarePlanObs().stream().
                map(fhirResourceMapper::mapToCarePlan).collect(Collectors.toList());
        List<Condition> chiefComplaints = new ArrayList<>();
        for(int i=0;i<openMrsDischargeSummary.getChiefComplaints().size();i++){
            chiefComplaints.add(fhirResourceMapper.mapToCondition(openMrsDischargeSummary.getChiefComplaints().get(i), patient));
        }
        List<MedicationRequest> medicationRequestsList = openMrsDischargeSummary.getDrugOrders().stream().
                map(fhirResourceMapper::mapToMedicationRequest).collect(Collectors.toList());
        List<Medication> medications = openMrsDischargeSummary.getDrugOrders().stream().map(fhirResourceMapper::mapToMedication).
                filter(medication -> !Objects.isNull(medication)).collect(Collectors.toList());
        List<Condition> fhirMedicalHistoryList = new ArrayList<>();
        for(int i=0;i<openMrsDischargeSummary.getMedicalHistory().size();i++){
            fhirMedicalHistoryList.add(fhirResourceMapper.mapToCondition(openMrsDischargeSummary.getMedicalHistory().get(i), patient));
        }
        List<Observation> physicalExaminations = openMrsDischargeSummary.getPhysicalExaminationObs().stream().
                map(fhirResourceMapper::mapToObs).collect(Collectors.toList());
        List<DocumentReference> patientDocuments = openMrsDischargeSummary.getPatientDocuments().stream().
                map(obs -> omrsObsDocumentTransformer.transForm(obs, DocumentReference.class, AbdmConfig.HiTypeDocumentKind.DISCHARGE_SUMMARY))
                .filter(Objects::nonNull).collect(Collectors.toList());
        List<Procedure> fhirProcedureList = new ArrayList<>();
        for(int i=0;i<openMrsDischargeSummary.getProcedure().size();i++){
            fhirProcedureList.add(fhirResourceMapper.mapToProcedure(encounter,openMrsDischargeSummary.getProcedure().get(i),abdmConfig.getProcedureAttributeConcepts()));
        }
        List<ServiceRequest> serviceRequest = openMrsDischargeSummary.getOrders().stream().
                map(fhirResourceMapper::mapToOrder).collect(Collectors.toList());

        return new FhirDischargeSummary(encounterId, encounter, visitDatetime, practitioners, patientReference, chiefComplaints, medicationRequestsList, medications, fhirMedicalHistoryList, patient, carePlans, physicalExaminations, patientDocuments, fhirProcedureList, serviceRequest);
    }

    public Bundle bundleDischargeSummary(OrganizationContext orgContext) {
        String bundleID = String.format("DS-%d", encounterID);
        Bundle bundle = FHIRUtils.createBundle(visitTimestamp, bundleID, orgContext.getWebUrl());
        FHIRUtils.addToBundleEntry(bundle, compositionFrom(orgContext), false);
        FHIRUtils.addToBundleEntry(bundle, orgContext.getOrganization(), false);
        FHIRUtils.addToBundleEntry(bundle, patient, false);
        FHIRUtils.addToBundleEntry(bundle, practitioners, false);
        FHIRUtils.addToBundleEntry(bundle, medicationRequests, false);
        FHIRUtils.addToBundleEntry(bundle, chiefComplaints, false);
        FHIRUtils.addToBundleEntry(bundle, medications, false);
        FHIRUtils.addToBundleEntry(bundle, medicalHistory, false);
        FHIRUtils.addToBundleEntry(bundle, encounter, false);
        FHIRUtils.addToBundleEntry(bundle, carePlan, false);
        FHIRUtils.addToBundleEntry(bundle, observations, false);
        if (procedures != null) FHIRUtils.addToBundleEntry(bundle, procedures, false);
        FHIRUtils.addToBundleEntry(bundle, patientDocuments, false);
        FHIRUtils.addToBundleEntry(bundle, serviceRequest, false);
        return bundle;
    }

    private Composition compositionFrom(OrganizationContext orgContext) {
        Composition composition = initializeComposition(visitTimestamp, orgContext.getWebUrl());
        Meta meta = new Meta();
        CanonicalType profileCanonical = new CanonicalType("https://nrces.in/ndhm/fhir/r4/StructureDefinition/DischargeSummaryRecord");
        List<CanonicalType> profileList = Collections.singletonList(profileCanonical);
        meta.setProfile(profileList);
        composition.setMeta(meta);
        composition
                .setEncounter(FHIRUtils.getReferenceToResource(encounter))
                .setSubject(patientReference)
                .setAuthor(Collections.singletonList(FHIRUtils.getReferenceToResource(orgContext.getOrganization(), "Organization")));

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

        if (medicationRequests.size() > 0) {
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

        if (procedures.size() > 0) {
            Composition.SectionComponent procedureCompositionSection = composition.addSection();
            procedureCompositionSection
                    .setTitle("Procedure")
                    .setCode(FHIRUtils.getProcedureType());

            procedures
                    .stream()
                    .map(FHIRUtils::getReferenceToResource)
                    .forEach(procedureCompositionSection::addEntry);
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

        return composition;
    }

    private Composition initializeComposition(Date visitTimestamp, String webURL) {
        Composition composition = new Composition();
        composition.setId(UUID.randomUUID().toString());
        composition.setDate(visitTimestamp);
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
