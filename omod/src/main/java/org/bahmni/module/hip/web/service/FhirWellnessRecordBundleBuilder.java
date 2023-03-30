package org.bahmni.module.hip.web.service;

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
		WellnessRecordBundle wellnessRecordBundle = buildWellnessBundle(encounter, wellnessAttributeObsMap, organizationContext);
		return null;
	}

	private WellnessRecordBundle buildWellnessBundle(Encounter encounter, Map<AbdmConfig.WellnessAttribute, List<Obs>> wellnessAttributeObsMap, OrganizationContext orgContext) {
		Patient patient = fhirResourceMapper.mapToPatient(encounter.getPatient());
		org.hl7.fhir.r4.model.Encounter wellnessEncounter = encounterTranslator.toFhirResource(encounter);
		List<Practitioner> practitioners = practitionersFrom(encounter.getEncounterProviders());
		Reference patientRef = FHIRUtils.getReferenceToResource(patient);

		Bundle bundle = FHIRUtils.createBundle(
				encounter.getEncounterDatetime(),
				String.format("WR-%d", encounter.getId()),
				orgContext.getWebUrl());
		Composition document = compositionFrom(encounter.getEncounterDatetime(), UUID.randomUUID().toString(), orgContext);
		document
				.setSubject(patientRef)
				.setAuthor(Collections.singletonList(FHIRUtils.getReferenceToResource(orgContext.getOrganization(), "Organization")));

		if (wellnessAttributeObsMap.size() != 0) {
			if (wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.VITAL_SIGNS).size() != 0) {
				Composition.SectionComponent vitalSignCompositionSection = document.addSection()
						.setTitle("Vital Sign")
						.setCode(FHIRUtils.getVitalDocumentType());

				wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.VITAL_SIGNS)
						.stream()
						.map(fhirResourceMapper::mapToObs)
						.map(FHIRUtils::getReferenceToResource)
						.forEach(vitalSignCompositionSection::addEntry);
			}
			if (wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.BODY_MEASUREMENT).size() != 0) {

				Composition.SectionComponent bodyMeasurementCompositionSection = document.addSection()
						.setTitle("Body Measurement")
						.setCode(FHIRUtils.getBodyMeasurementType());

				wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.BODY_MEASUREMENT)
						.stream()
						.map(fhirResourceMapper::mapToObs)
						.map(FHIRUtils::getReferenceToResource)
						.forEach(bodyMeasurementCompositionSection::addEntry);
			}
			if (wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.PHYSICAL_ACTIVITY).size() != 0) {

				Composition.SectionComponent physicalActivityCompositionSection = document.addSection()
						.setTitle("Physical Activity")
						.setCode(FHIRUtils.getBodyMeasurementType());
				wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.PHYSICAL_ACTIVITY)
						.stream()
						.map(fhirResourceMapper::mapToObs)
						.map(FHIRUtils::getReferenceToResource)
						.forEach(physicalActivityCompositionSection::addEntry);
			}
			if (wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.GENERAL_ASSESSMENT).size() != 0) {

				Composition.SectionComponent generalAssessmentCompositionSection = document.addSection()
						.setTitle("General Assessment")
						.setCode(FHIRUtils.getGeneralAssessmentType());
				wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.GENERAL_ASSESSMENT)
						.stream()
						.map(fhirResourceMapper::mapToObs)
						.map(FHIRUtils::getReferenceToResource)
						.forEach(generalAssessmentCompositionSection::addEntry);
			}
			if (wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.WOMEN_HEALTH).size() != 0) {

				Composition.SectionComponent womenHealthCompositionSection = document.addSection()
						.setTitle("Women Health")
						.setCode(FHIRUtils.getWomenHealthType());
				wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.WOMEN_HEALTH)
						.stream()
						.map(fhirResourceMapper::mapToObs)
						.map(FHIRUtils::getReferenceToResource)
						.forEach(womenHealthCompositionSection::addEntry);
			}
			if (wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.LIFESTYLE).size() != 0) {
				Composition.SectionComponent lifestyleCompositionSection = document.addSection()
						.setTitle("Lifestyle")
						.setCode(FHIRUtils.getLifestyleType());
				wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.LIFESTYLE)
						.stream()
						.map(fhirResourceMapper::mapToObs)
						.map(FHIRUtils::getReferenceToResource)
						.forEach(lifestyleCompositionSection::addEntry);
			}
			if (wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.OTHER_OBSERVATIONS).size() != 0) {
				Composition.SectionComponent otherObservationsCompositionSection = document.addSection()
						.setTitle("Other Observations")
						.setCode(FHIRUtils.getOtherObservationType());
				wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.OTHER_OBSERVATIONS)
						.stream()
						.map(fhirResourceMapper::mapToObs)
						.map(FHIRUtils::getReferenceToResource)
						.forEach(otherObservationsCompositionSection::addEntry);
			}
			if (wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.DOCUMENT_REFERENCE).size() != 0) {
				Composition.SectionComponent documentReferenceCompositionSection = document.addSection()
						.setTitle("Lifestyle")
						.setCode(FHIRUtils.getDocumentReferenceType());
				wellnessAttributeObsMap.get(AbdmConfig.WellnessAttribute.DOCUMENT_REFERENCE)
						.stream()
						.map(fhirResourceMapper::mapToObs)
						.map(FHIRUtils::getReferenceToResource)
						.forEach(documentReferenceCompositionSection::addEntry);
			}
		}
		FHIRUtils.addToBundleEntry(bundle, document, false);
		FHIRUtils.addToBundleEntry(bundle, patient, false); //add patient
		FHIRUtils.addToBundleEntry(bundle, orgContext.getOrganization(), false);
		FHIRUtils.addToBundleEntry(bundle, wellnessEncounter, false);
		FHIRUtils.addToBundleEntry(bundle, practitioners, false);
		CareContext careContext = CareContext.builder().careContextReference(encounter.getVisit().getUuid()).careContextType("Visit").build();
		return new WellnessRecordBundle(careContext, bundle);
	}

	private Composition compositionFrom(Date compositionDate, String documentId, OrganizationContext orgContext) {
		Composition document = new Composition();
		document.setId(documentId);
		document.setDate(compositionDate); //or should it be now?
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
