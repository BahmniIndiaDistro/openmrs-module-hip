package org.bahmni.module.hip.web.service;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.hip.web.model.*;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.*;
import org.openmrs.Encounter;
import org.openmrs.*;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterTranslator;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
@Slf4j
@Service
public class FhirWellnessRecordBundleBuilder {
	private final FHIRResourceMapper fhirResourceMapper;
	private final EncounterTranslator<Encounter> encounterTranslator;
	private final Map<AbdmConfig.WellnessAttribute, Concept> wellnessAttributeConceptMap;
	private final ConceptTranslator conceptTranslator;

	public FhirWellnessRecordBundleBuilder(FHIRResourceMapper fhirResourceMapper,
										   EncounterTranslator<Encounter> encounterTranslator,
										   Map<AbdmConfig.WellnessAttribute, Concept> wellnessAttributeConceptMap,
										   ConceptTranslator conceptTranslator) {
		this.fhirResourceMapper = fhirResourceMapper;
		this.encounterTranslator = encounterTranslator;
		this.wellnessAttributeConceptMap = wellnessAttributeConceptMap;
		this.conceptTranslator = conceptTranslator;
	}

	public WellnessRecordBundle build(Encounter encounter, Map<AbdmConfig.WellnessAttribute, List<Obs>> wellnessAttributeObsMap, OrganizationContext organizationContext) {
		if (wellnessAttributeObsMap.isEmpty()) return null;
		return buildWellnessBundle(encounter, wellnessAttributeObsMap, organizationContext);
	}

	private WellnessRecordBundle buildWellnessBundle(Encounter encounter, Map<AbdmConfig.WellnessAttribute, List<Obs>> wellnessAttributeObsMap, OrganizationContext orgContext) {
		Patient patient = fhirResourceMapper.mapToPatient(encounter.getPatient());
		org.hl7.fhir.r4.model.Encounter wellnessEncounter = encounterTranslator.toFhirResource(encounter);
		List<Practitioner> practitioners = practitionersFrom(encounter.getEncounterProviders());
		Reference patientRef = FHIRUtils.getReferenceToResource(patient);
		List<Observation> observations = new ArrayList<>();
		List<DocumentReference> documentReference = new ArrayList<>();

		Bundle bundle = FHIRUtils.createBundle(
				encounter.getEncounterDatetime(),
				String.format("WR-%d", encounter.getId()),
				orgContext.getWebUrl());
		Composition document = compositionFrom(encounter.getEncounterDatetime(), UUID.randomUUID().toString(), orgContext);
		Meta meta = new Meta();
		CanonicalType profileCanonical = new CanonicalType("https://nrces.in/ndhm/fhir/r4/StructureDefinition/WellnessRecord");
		List<CanonicalType> profileList = Collections.singletonList(profileCanonical);
		meta.setProfile(profileList);
		document.setMeta(meta);
		document
				.setEncounter(FHIRUtils.getReferenceToResource(wellnessEncounter))
				.setSubject(patientRef)
				.setAuthor(Collections.singletonList(FHIRUtils.getReferenceToResource(orgContext.getOrganization(), "Organization")));

		if (wellnessAttributeObsMap.size() != 0) {
			if (wellnessAttributeObsMap.containsKey(AbdmConfig.WellnessAttribute.VITAL_SIGNS) && wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.VITAL_SIGNS).size() != 0) {
				Composition.SectionComponent vitalSignCompositionSection = document.addSection()
						.setTitle("Vital Sign")
						.setCode(FHIRUtils.getVitalDocumentType());

				addEntry(wellnessAttributeObsMap, vitalSignCompositionSection, AbdmConfig.WellnessAttribute.VITAL_SIGNS);
				addObservations(observations,wellnessAttributeObsMap,AbdmConfig.WellnessAttribute.VITAL_SIGNS);
			}
			if (wellnessAttributeObsMap.containsKey(AbdmConfig.WellnessAttribute.BODY_MEASUREMENT) && wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.BODY_MEASUREMENT).size() != 0) {

				Composition.SectionComponent bodyMeasurementCompositionSection = document.addSection()
						.setTitle("Body Measurement")
						.setCode(FHIRUtils.getBodyMeasurementType());

				addEntry(wellnessAttributeObsMap, bodyMeasurementCompositionSection, AbdmConfig.WellnessAttribute.BODY_MEASUREMENT);

				addObservations(observations,wellnessAttributeObsMap,AbdmConfig.WellnessAttribute.BODY_MEASUREMENT);
			}
			if (wellnessAttributeObsMap.containsKey(AbdmConfig.WellnessAttribute.PHYSICAL_ACTIVITY) && wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.PHYSICAL_ACTIVITY).size() != 0) {

				Composition.SectionComponent physicalActivityCompositionSection = document.addSection()
						.setTitle("Physical Activity")
						.setCode(FHIRUtils.getPhysicalActivityType());
				addEntry(wellnessAttributeObsMap, physicalActivityCompositionSection, AbdmConfig.WellnessAttribute.PHYSICAL_ACTIVITY);
				addObservations(observations,wellnessAttributeObsMap,AbdmConfig.WellnessAttribute.PHYSICAL_ACTIVITY);
			}
			if (wellnessAttributeObsMap.containsKey(AbdmConfig.WellnessAttribute.GENERAL_ASSESSMENT) && wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.GENERAL_ASSESSMENT).size() != 0) {

				Composition.SectionComponent generalAssessmentCompositionSection = document.addSection()
						.setTitle("General Assessment")
						.setCode(FHIRUtils.getGeneralAssessmentType());
				addEntry(wellnessAttributeObsMap, generalAssessmentCompositionSection, AbdmConfig.WellnessAttribute.GENERAL_ASSESSMENT);
				addObservations(observations,wellnessAttributeObsMap,AbdmConfig.WellnessAttribute.GENERAL_ASSESSMENT);
			}
			if (wellnessAttributeObsMap.containsKey(AbdmConfig.WellnessAttribute.WOMEN_HEALTH) && wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.WOMEN_HEALTH).size() != 0) {

				Composition.SectionComponent womenHealthCompositionSection = document.addSection()
						.setTitle("Women Health")
						.setCode(FHIRUtils.getWomenHealthType());
				addEntry(wellnessAttributeObsMap, womenHealthCompositionSection, AbdmConfig.WellnessAttribute.WOMEN_HEALTH);
				addObservations(observations,wellnessAttributeObsMap,AbdmConfig.WellnessAttribute.WOMEN_HEALTH);
			}
			if (wellnessAttributeObsMap.containsKey(AbdmConfig.WellnessAttribute.LIFESTYLE) && wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.LIFESTYLE).size() != 0) {
				Composition.SectionComponent lifestyleCompositionSection = document.addSection()
						.setTitle("Lifestyle")
						.setCode(FHIRUtils.getLifestyleType());
				addEntry(wellnessAttributeObsMap, lifestyleCompositionSection, AbdmConfig.WellnessAttribute.LIFESTYLE);
				addObservations(observations,wellnessAttributeObsMap,AbdmConfig.WellnessAttribute.LIFESTYLE);
			}
			if (wellnessAttributeObsMap.containsKey(AbdmConfig.WellnessAttribute.OTHER_OBSERVATIONS) && wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.OTHER_OBSERVATIONS).size() != 0) {
				Composition.SectionComponent otherObservationsCompositionSection = document.addSection()
						.setTitle("Other Observations")
						.setCode(FHIRUtils.getOtherObservationType());
				addEntry(wellnessAttributeObsMap, otherObservationsCompositionSection, AbdmConfig.WellnessAttribute.OTHER_OBSERVATIONS);
				addObservations(observations,wellnessAttributeObsMap,AbdmConfig.WellnessAttribute.OTHER_OBSERVATIONS);
			}
			if (wellnessAttributeObsMap.containsKey(AbdmConfig.WellnessAttribute.DOCUMENT_REFERENCE) && wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.DOCUMENT_REFERENCE).size() != 0) {
				Composition.SectionComponent documentReferenceCompositionSection = document.addSection()
						.setTitle("Lifestyle")
						.setCode(FHIRUtils.getDocumentReferenceType());
				addEntry(wellnessAttributeObsMap, documentReferenceCompositionSection, AbdmConfig.WellnessAttribute.DOCUMENT_REFERENCE);
				documentReference.addAll(wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.DOCUMENT_REFERENCE).stream().
						map(fhirResourceMapper::mapToDocumentDocumentReference).collect(Collectors.toList()));
			}
		}
		FHIRUtils.addToBundleEntry(bundle, document, false);
		FHIRUtils.addToBundleEntry(bundle, patient, false); //add patient
		FHIRUtils.addToBundleEntry(bundle, orgContext.getOrganization(), false);
		FHIRUtils.addToBundleEntry(bundle, wellnessEncounter, false);
		FHIRUtils.addToBundleEntry(bundle, practitioners, false);
		FHIRUtils.addToBundleEntry(bundle, observations, false);
		if(documentReference.size() != 0){
			FHIRUtils.addToBundleEntry(bundle, documentReference, false);
		}
		CareContext careContext = CareContext.builder().careContextReference(encounter.getVisit().getUuid()).careContextType("Visit").build();
		return new WellnessRecordBundle(careContext, bundle);
	}

	private void addObservations(List<Observation> observations, Map<AbdmConfig.WellnessAttribute, List<Obs>> wellnessAttributeObsMap, AbdmConfig.WellnessAttribute section){
		observations.addAll(wellnessAttributeObsMap.get(section).stream().
				map(fhirResourceMapper::mapToObs).collect(Collectors.toList()));
	}
	private void addEntry(Map<AbdmConfig.WellnessAttribute, List<Obs>> wellnessAttributeObsMap, Composition.SectionComponent sectionComponent, AbdmConfig.WellnessAttribute section){
		wellnessAttributeObsMap.get(section)
				.stream()
				.map(fhirResourceMapper::mapToObs)
				.map(FHIRUtils::getReferenceToResource)
				.forEach(sectionComponent::addEntry);
	}
	private Composition compositionFrom(Date compositionDate, String documentId, OrganizationContext orgContext) {
		Composition document = new Composition();
		document.setId(documentId);
		document.setDate(compositionDate);
		document.setIdentifier(FHIRUtils.getIdentifier(document.getId(), orgContext.getWebUrl(), "document"));
		document.setStatus(Composition.CompositionStatus.FINAL);
		document.setType(FHIRUtils.getWellnessRecordType());
		document.setTitle("Wellness Record");
		return document;
	}

	private List<Practitioner> practitionersFrom(Set<EncounterProvider> encounterProviders) {
		return encounterProviders
				.stream()
				.map(fhirResourceMapper::mapToPractitioner)
				.collect(Collectors.toList());
	}
}
