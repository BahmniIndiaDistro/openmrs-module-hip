package org.bahmni.module.hip.web.service;

import org.bahmni.module.hip.web.model.CareContext;
import org.bahmni.module.hip.web.model.HealthDocumentRecordBundle;
import org.bahmni.module.hip.web.model.OrganizationContext;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FhirHealthDocumentRecordBuilder {
    private FHIRResourceMapper fhirResourceMapper;
    private OmrsObsDocumentTransformer documentTransformer;

    public FhirHealthDocumentRecordBuilder(FHIRResourceMapper fhirResourceMapper,
                                           OmrsObsDocumentTransformer documentTransformer) {
        this.fhirResourceMapper = fhirResourceMapper;
        this.documentTransformer = documentTransformer;
    }

    public HealthDocumentRecordBundle build(Obs docObs, OrganizationContext organizationContext) {
        DocumentReference documentReference = documentTransformer.transForm(docObs, DocumentReference.class, AbdmConfig.HiTypeDocumentKind.HEALTH_DOCUMENT_RECORD);
        if (documentReference == null) return null;
        org.hl7.fhir.r4.model.Encounter docEncounter = fhirResourceMapper.mapToEncounter(docObs.getEncounter());
        docEncounter.getClass_().setDisplay("ambulatory");
        Patient patient = fhirResourceMapper.mapToPatient(docObs.getEncounter().getPatient());
        List<Practitioner> practitioners = docObs.getEncounter().getEncounterProviders()
                .stream()
                .map(fhirResourceMapper::mapToPractitioner)
                .collect(Collectors.toList());
        String bundleId = String.format("HDR-%d-%d", docObs.getEncounter().getId(), docObs.getId());
        Bundle bundle = FHIRUtils.createBundle(docObs.getEncounter().getEncounterDatetime(), bundleId, organizationContext.getWebUrl());
        Composition composition =
                compositionFrom(docObs.getEncounter(), organizationContext)
                    .setSubject(FHIRUtils.getReferenceToResource(patient))
                    .setEncounter(FHIRUtils.getReferenceToResource(docEncounter));
        Meta meta = new Meta();
        CanonicalType profileCanonical = new CanonicalType("https://nrces.in/ndhm/fhir/r4/StructureDefinition/HealthDocumentRecord");
        List<CanonicalType> profileList = Collections.singletonList(profileCanonical);
        meta.setProfile(profileList);
        composition.setMeta(meta);
        FHIRUtils.addToBundleEntry(bundle, composition, false);
        FHIRUtils.addToBundleEntry(bundle, organizationContext.getOrganization(), false);
        FHIRUtils.addToBundleEntry(bundle, patient, false);
        FHIRUtils.addToBundleEntry(bundle, docEncounter, false);
        FHIRUtils.addToBundleEntry(bundle, documentReference, false);
        FHIRUtils.addToBundleEntry(bundle, practitioners, false);
        composition.addSection()
                .setTitle("Record Artifact")
                .setCode(FHIRUtils.getRecordArtifactType())
                .addEntry(FHIRUtils.getReferenceToResource(documentReference));
        CareContext careContext = CareContext.builder().careContextReference(docObs.getEncounter().getVisit().getUuid()).careContextType("Visit").build();
        return new HealthDocumentRecordBundle(careContext, bundle);
    }

    private Composition compositionFrom(Encounter docEncounter, OrganizationContext organizationContext) {
        Composition composition = new Composition();
        composition.setDate(docEncounter.getEncounterDatetime())
                    .setIdentifier(FHIRUtils.getIdentifier(composition.getId(), organizationContext.getWebUrl(), "document"))
                    .setStatus(Composition.CompositionStatus.FINAL)
                    .setType(FHIRUtils.getRecordArtifactType())
                    .setTitle("Record artifact")
                    .setAuthor(Collections.singletonList(FHIRUtils.getReferenceToResource(organizationContext.getOrganization(), "Organization")))
                    .setId(UUID.randomUUID().toString());
        return composition;
    }

    public HealthDocumentRecordBundle build(Encounter encounter, List<Obs> obsDocList, OrganizationContext orgContext) {
        if (obsDocList.isEmpty()) return null;
        List<DocumentReference> documentRefs = obsDocList.stream()
                .map(docObs -> documentTransformer.transForm(docObs, DocumentReference.class, AbdmConfig.HiTypeDocumentKind.HEALTH_DOCUMENT_RECORD))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (documentRefs.isEmpty()) return null;

        org.hl7.fhir.r4.model.Encounter docEncounter = fhirResourceMapper.mapToEncounter(encounter);
        docEncounter.getClass_().setDisplay("ambulatory");
        Patient patient = fhirResourceMapper.mapToPatient(encounter.getPatient());
        List<Practitioner> practitioners = encounter.getEncounterProviders()
                .stream()
                .map(fhirResourceMapper::mapToPractitioner)
                .collect(Collectors.toList());
        String bundleId = String.format("HDR-%d", encounter.getId());
        Bundle bundle = FHIRUtils.createBundle(encounter.getEncounterDatetime(), bundleId, orgContext.getWebUrl());
        Composition composition =
                compositionFrom(encounter, orgContext)
                        .setSubject(FHIRUtils.getReferenceToResource(patient))
                        .setEncounter(FHIRUtils.getReferenceToResource(docEncounter));
        FHIRUtils.addToBundleEntry(bundle, composition, false);
        FHIRUtils.addToBundleEntry(bundle, orgContext.getOrganization(), false);
        FHIRUtils.addToBundleEntry(bundle, patient, false);
        FHIRUtils.addToBundleEntry(bundle, docEncounter, false);
        FHIRUtils.addToBundleEntry(bundle, practitioners, false);
        Composition.SectionComponent recordSection = composition.addSection()
                .setTitle("Record Artifact")
                .setCode(FHIRUtils.getRecordArtifactType());
        documentRefs.forEach(docRef -> {
            FHIRUtils.addToBundleEntry(bundle, docRef, false);
            recordSection.addEntry(FHIRUtils.getReferenceToResource(docRef));
        });
        CareContext careContext = CareContext.builder().careContextReference(encounter.getVisit().getUuid()).careContextType("Visit").build();
        return new HealthDocumentRecordBundle(careContext, bundle);
    }
}
