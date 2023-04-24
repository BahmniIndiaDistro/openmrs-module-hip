package org.bahmni.module.hip.web.service;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.hip.web.model.OrganizationContext;
import org.bahmni.module.hip.web.model.WellnessRecordBundle;
import org.openmrs.*;
import org.openmrs.api.ConceptService;
import org.openmrs.api.VisitService;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
	private final FhirWellnessRecordBundleBuilder fhirWellnessRecordBundleBuilder;


	@Autowired
	public WellnessRecordService(VisitService visitService, ConceptService conceptService,
								 OrganizationContextService organizationContextService,
								 FHIRResourceMapper fhirResourceMapper,
								 ConceptTranslator conceptTranslator,
								 EncounterTranslator<Encounter> encounterTranslator,
								 AbdmConfig abdmConfig,
								 FhirWellnessRecordBundleBuilder fhirWellnessRecordBundleBuilder
	) {
		this.visitService = visitService;
		this.conceptService = conceptService;
		this.organizationContextService = organizationContextService;
		this.fhirResourceMapper = fhirResourceMapper;
		this.conceptTranslator = conceptTranslator;
		this.encounterTranslator = encounterTranslator;
		this.abdmConfig = abdmConfig;
		this.fhirWellnessRecordBundleBuilder = fhirWellnessRecordBundleBuilder;
	}

	public List<WellnessRecordBundle> getWellnessForVisit(String patientUuid, String visitUuid, Date fromEncounterDate, Date toEncounterDate) {
		Visit visit = visitService.getVisitByUuid(visitUuid);
		if (visit == null) {
			log.warn(String.format("Could not identify visit by uuid [%s]", visitUuid));
			return Collections.emptyList();
		}
		Optional<Location> location = OrganizationContextService.findOrganization(visit.getLocation());
		OrganizationContext organizationContext = organizationContextService.buildContext(location);

		if (!visit.getPatient().getUuid().equals(patientUuid.trim())) {
			log.warn("Identified visit is not for the requested patient. " +
					"This should never happen. This may mean a invalid linkage in the care context");
			return Collections.emptyList();
		}

		Map<AbdmConfig.WellnessAttribute, List<Concept>> wellnessAttributeConceptMap = getWellnessAttributeConcept();

		if (wellnessAttributeConceptMap.isEmpty())
			return Collections.emptyList();
		Map<Encounter, List<Obs>> encounterObsList = visit.getEncounters().stream()
				.filter(e -> fromEncounterDate == null || e.getEncounterDatetime().after(fromEncounterDate))
				.filter(e-> toEncounterDate == null || e.getEncounterDatetime().before(toEncounterDate))
				.map(encounter -> encounter.getObsAtTopLevel(false))
				.flatMap(Collection::stream)
				.collect(Collectors.groupingBy(obs -> obs.getEncounter()));

		Map<AbdmConfig.WellnessAttribute, List<Obs>> wellnessAttributeObsMap = new HashMap<>();
		encounterObsList.entrySet().stream().forEach(entry -> entry.getValue().stream().forEach(obs -> {
					AbdmConfig.WellnessAttribute wellnessAttributeTypeForObs = getWellnessAttributeTypeForObs(obs, wellnessAttributeConceptMap);
					if (wellnessAttributeTypeForObs != null) {
						if (wellnessAttributeObsMap.containsKey(wellnessAttributeTypeForObs)) {
							List<Obs> obsList = wellnessAttributeObsMap.get(wellnessAttributeTypeForObs);
							obsList.add(obs);
							wellnessAttributeObsMap.put(wellnessAttributeTypeForObs, obsList);
						} else
							wellnessAttributeObsMap.put(wellnessAttributeTypeForObs, new ArrayList<> (Collections.singletonList(obs)));
					}
				}
		));
		return encounterObsList.entrySet()
				.stream().map(entry -> fhirWellnessRecordBundleBuilder.build(entry.getKey(), wellnessAttributeObsMap, organizationContext))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	private AbdmConfig.WellnessAttribute getWellnessAttributeTypeForObs(Obs obs, Map<AbdmConfig.WellnessAttribute, List<Concept>> wellnessAttributeConceptMap) {
		for (Map.Entry<AbdmConfig.WellnessAttribute, List<Concept>> entry : wellnessAttributeConceptMap.entrySet()) {
			if (entry.getValue().contains(obs.getConcept()))
				return entry.getKey();
		}
		return null;
	}

	private Map<AbdmConfig.WellnessAttribute, List<Concept>> getWellnessAttributeConcept() {
		return abdmConfig.getWellnessAttributeConfigs().entrySet()
				.stream()
				.collect(HashMap::new, (m, v) -> m.put(v.getKey(),
						abdmConfig.getWellnessAttributeConcept(v.getKey())), HashMap::putAll);
	}
}
