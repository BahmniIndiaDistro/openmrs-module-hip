package org.bahmni.module.hip.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.hip.config.AbdmConfig;
import org.bahmni.module.hip.mapper.FHIRResourceMapper;
import org.bahmni.module.hip.utils.FHIRUtils;
import org.bahmni.module.hip.builder.OmrsObsDocumentTransformer;
import org.hl7.fhir.r4.model.*;
import org.openmrs.EncounterProvider;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Slf4j
public class FhirPrescription {
    private final Date visitTimeStamp;
    private final Integer encounterID;
    private final Encounter encounter;
    private final List<Practitioner> practitioners;
    private final Patient patient;
    private final Reference patientReference;
    private final List<Medication> medications;
    private final List<MedicationRequest> medicationRequests;
    private final List<Binary> documents;

    private FhirPrescription(Date visitTimeStamp, Integer encounterID, Encounter encounter,
                             List<Practitioner> practitioners, Patient patient,
                             Reference patientReference, List<Medication> medications,
                             List<MedicationRequest> medicationRequests, List<Binary> documents) {
        this.visitTimeStamp = visitTimeStamp;
        this.encounterID = encounterID;
        this.encounter = encounter;
        this.practitioners = practitioners;
        this.patient = patient;
        this.patientReference = patientReference;
        this.medications = medications;
        this.medicationRequests = medicationRequests;
        this.documents = documents;
    }

    public static FhirPrescription from(OpenMrsPrescription openMrsPrescription, FHIRResourceMapper fhirResourceMapper, OmrsObsDocumentTransformer documentTransformer) {
        Date encounterDatetime = openMrsPrescription.getEncounter().getEncounterDatetime();
        Integer encounterId = openMrsPrescription.getEncounter().getId();
        Patient patient = fhirResourceMapper.mapToPatient(openMrsPrescription.getPatient());
        Reference patientReference = FHIRUtils.getReferenceToResource(patient);
        Encounter encounter = fhirResourceMapper.mapToEncounter(openMrsPrescription.getEncounter());
        encounter.getClass_().setDisplay("ambulatory");
        List<Practitioner> practitioners = getPractitionersFrom(fhirResourceMapper, openMrsPrescription.getEncounterProviders());
        List<MedicationRequest> medicationRequests = medicationRequestsFor(fhirResourceMapper, openMrsPrescription.getDrugOrders());
        List<Medication> medications = medicationsFor(fhirResourceMapper, openMrsPrescription.getDrugOrders());
        List<Binary> prescriptionDocs = (openMrsPrescription.getDocObs() != null &&  !openMrsPrescription.getDocObs().isEmpty())
                ? openMrsPrescription.getDocObs()
                    .stream()
                    .map(o -> documentTransformer.transForm(o, Binary.class, AbdmConfig.HiTypeDocumentKind.PRESCRIPTION))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList())
                : Collections.emptyList();
        return new FhirPrescription(encounterDatetime, encounterId, encounter, practitioners, patient, patientReference, medications, medicationRequests, prescriptionDocs);
    }

    public Bundle bundle(OrganizationContext orgContext) {
        String bundleId = String.format("PR-%d", encounterID);
        Bundle bundle = FHIRUtils.createBundle(visitTimeStamp, bundleId, orgContext.getWebUrl());
        FHIRUtils.addToBundleEntry(bundle, compositionFrom(orgContext), false);
        FHIRUtils.addToBundleEntry(bundle, orgContext.getOrganization(), false);
        FHIRUtils.addToBundleEntry(bundle, patient, false);
        FHIRUtils.addToBundleEntry(bundle, encounter, false);
        FHIRUtils.addToBundleEntry(bundle, medications, false);
        FHIRUtils.addToBundleEntry(bundle, medicationRequests, false);
        FHIRUtils.addToBundleEntry(bundle, practitioners, false);
        if (!documents.isEmpty()) {
            FHIRUtils.addToBundleEntry(bundle, documents, false);
        }
        return bundle;
    }

    private Composition compositionFrom(OrganizationContext orgContext) {
        Composition composition = initializeComposition(visitTimeStamp, orgContext.getWebUrl());
        Composition.SectionComponent compositionSection = composition.addSection();

        Meta meta = new Meta();
        CanonicalType profileCanonical = new CanonicalType("https://nrces.in/ndhm/fhir/r4/StructureDefinition/PrescriptionRecord");
        List<CanonicalType> profileList = Collections.singletonList(profileCanonical);
        meta.setProfile(profileList);
        composition.setMeta(meta);

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
        documents
                .stream()
                .findFirst() //because ABDM allows only one binary per prescription doc
                .map(FHIRUtils::getReferenceToResource)
                .ifPresent(compositionSection::addEntry);

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
        if (drugOrders == null) {
            return Collections.emptyList();
        }
        return drugOrders
                .stream()
                .map(fhirResourceMapper::mapToMedicationRequest)
                .collect(Collectors.toList());
    }

    private static List<Medication> medicationsFor(FHIRResourceMapper fhirResourceMapper, DrugOrders drugOrders) {
        if (drugOrders == null) {
            return Collections.emptyList();
        }
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
