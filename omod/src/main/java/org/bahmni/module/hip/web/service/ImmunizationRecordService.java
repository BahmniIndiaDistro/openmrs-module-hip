package org.bahmni.module.hip.web.service;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.hip.web.model.ImmunizationRecordBundle;
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
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ImmunizationRecordService {

    private final AbdmConfig abdmConfig;
    private final VisitService visitService;
    private ConceptService conceptService;
    private OrganizationContextService organizationContextService;
    private FHIRResourceMapper fhirResourceMapper;
    private ConceptTranslator conceptTranslator;
    private EncounterTranslator<Encounter> encounterTranslator;

    @Autowired
    public ImmunizationRecordService(VisitService visitService, ConceptService conceptService,
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

    public List<ImmunizationRecordBundle> getImmunizationRecordsForVisit(String patientUuid, String visitUuid, Date startDate, Date endDate) {
        Visit visit = visitService.getVisitByUuid(visitUuid);
        Optional<Location> location = identifyLocationByTag(visit.getLocation(), OrganizationContextService.ORGANIZATION_LOCATION_TAG);
        if (!location.isPresent()) {
            location = identifyLocationByTag(visit.getLocation(), OrganizationContextService.VISIT_LOCATION_TAG);
        }
        if (!visit.getPatient().getUuid().equals(patientUuid.trim())) {
            log.warn("Identified visit is not for the requested patient. " +
                    "This should never happen. This may mean a invalid linkage in the care context");
            return Collections.emptyList();
        }
        if (!isImmunizationObsTemplateConfigured()) {
           //no form template configured
            return Collections.emptyList();
        }

        //this can potentially be cached, the concept maps are going to be same usually
        Map<AbdmConfig.ImmunizationAttribute, Concept> immunizationAttributeConceptMap =
                getImmunizationAttributeConcepts();

        FhirImmunizationRecordBundleBuilder immunizationTransformer =
                new FhirImmunizationRecordBundleBuilder(fhirResourceMapper,
                        conceptTranslator, encounterTranslator,
                        organizationContextService.buildContext(location),
                        immunizationAttributeConceptMap);

        return visit.getEncounters().stream()
                .filter(e -> startDate == null || e.getEncounterDatetime().after(startDate))
                .map(immunizationTransformer::build)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private Optional<Location> identifyLocationByTag(Location location, String tagName) {
        if (location == null) {
            return Optional.empty();
        }
        boolean isMatched = location.getTags().size() > 0 && location.getTags().stream().filter(tag -> tag.getName().equalsIgnoreCase(tagName)).count() != 0;
        return isMatched ? Optional.of(location) : identifyLocationByTag(location.getParentLocation(), tagName);
    }


    private Map<AbdmConfig.ImmunizationAttribute, Concept> getImmunizationAttributeConcepts() {
        return abdmConfig.getImmunizationAttributeConfigs().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> identifyConcept(e)));
    }

    private Concept identifyConcept(Map.Entry<AbdmConfig.ImmunizationAttribute, String> entry) {
        //TODO: We need to figure out how we identify concepts. Right now its by UUID, while coding or name would be easier
        return conceptService.getConceptByUuid(entry.getValue());
    }


    private boolean isImmunizationObsTemplateConfigured() {
        return !StringUtils.isEmpty(abdmConfig.getImmunizationObsRootConcept());
    }

}
