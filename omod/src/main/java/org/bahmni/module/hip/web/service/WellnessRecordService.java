package org.bahmni.module.hip.web.service;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.hip.web.model.ImmunizationRecordBundle;
import org.bahmni.module.hip.web.model.WellnessRecordBundle;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Visit;
import org.openmrs.api.ConceptService;
import org.openmrs.api.VisitService;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class WellnessRecordService {
	private final AbdmConfig abdmConfig;
	private final VisitService visitService;
	private final ConceptService conceptService;
	private final OrganizationContextService organizationContextService;
	private final FHIRResourceMapper fhirResourceMapper;
	private final ConceptTranslator conceptTranslator;
	private final EncounterTranslator<Encounter> encounterTranslator;

	@Autowired
	public WellnessRecordService(VisitService visitService, ConceptService conceptService,
									 OrganizationContextService organizationContextService,
									 FHIRResourceMapper fhirResourceMapper,
									 ConceptTranslator conceptTranslator,
									 EncounterTranslator<Encounter> encounterTranslator,
									 AbdmConfig abdmConfig) {
		this.visitService = visitService;
		this.conceptService = conceptService;
		this.organizationContextService = organizationContextService;
		this.fhirResourceMapper = fhirResourceMapper;
		this.conceptTranslator = conceptTranslator;
		this.encounterTranslator = encounterTranslator;
		this.abdmConfig = abdmConfig;
	}
	public List<WellnessRecordBundle> getWellnessForVisit(String patientUuid, String visitUuid, Date fromEncounterDate, Date toEncounterDate) {
		Visit visit = visitService.getVisitByUuid(visitUuid);
		if (visit == null) {
			log.warn(String.format("Could not identify visit by uuid [%s]", visitUuid));
			return Collections.emptyList();
		}
		Optional<Location> location = identifyLocationByTag(visit.getLocation(), OrganizationContextService.ORGANIZATION_LOCATION_TAG);
		if (!location.isPresent()) {
			location = identifyLocationByTag(visit.getLocation(), OrganizationContextService.VISIT_LOCATION_TAG);
		}
		if (!visit.getPatient().getUuid().equals(patientUuid.trim())) {
			log.warn("Identified visit is not for the requested patient. " +
					"This should never happen. This may mean a invalid linkage in the care context");
			return Collections.emptyList();
		}

//		Map<AbdmConfig.WellnessAttribute, Concept> immunizationAttributeConceptMap =
//				getImmunizationAttributeConcepts();
    return null;
	}

	private Optional<Location> identifyLocationByTag(Location location, String tagName) {
		if (location == null) {
			return Optional.empty();
		}
		boolean isMatched = location.getTags().size() > 0 && location.getTags().stream().anyMatch(tag -> tag.getName().equalsIgnoreCase(tagName));
		return isMatched ? Optional.of(location) : identifyLocationByTag(location.getParentLocation(), tagName);
	}

}
