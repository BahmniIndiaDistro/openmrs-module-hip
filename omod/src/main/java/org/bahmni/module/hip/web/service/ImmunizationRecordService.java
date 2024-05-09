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

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ImmunizationRecordService {

    private static final String CIEL_CONEPT_IMMUNIZATION_RECEIVED = "163100AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private final AbdmConfig abdmConfig;
    private final VisitService visitService;
    private final ConceptService conceptService;
    private final OrganizationContextService organizationContextService;
    private final FHIRResourceMapper fhirResourceMapper;
    private final ConceptTranslator conceptTranslator;
    private final EncounterTranslator<Encounter> encounterTranslator;

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
        if (visit == null) {
            log.warn(String.format("Could not identify visit by uuid [%s]", visitUuid));
            return Collections.emptyList();
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

        Optional.ofNullable(immunizationAttributeConceptMap.get(AbdmConfig.ImmunizationAttribute.STATUS))
                .orElseGet(() -> {
                    Concept cielImmunizationReceivedConcept = conceptService.getConceptByUuid(CIEL_CONEPT_IMMUNIZATION_RECEIVED);
                    if (cielImmunizationReceivedConcept != null) {
                        immunizationAttributeConceptMap.put(AbdmConfig.ImmunizationAttribute.STATUS, cielImmunizationReceivedConcept);
                    }
                    return cielImmunizationReceivedConcept;
                });

        Optional<Location> location = OrganizationContextService.findOrganization(visit.getLocation());
        FhirImmunizationRecordBundleBuilder immunizationTransformer =
                new FhirImmunizationRecordBundleBuilder(fhirResourceMapper,
                        conceptTranslator, encounterTranslator,
                        organizationContextService.buildContext(location),
                        immunizationAttributeConceptMap);

        return visit.getEncounters().stream()
                .filter(e -> startDate == null || e.getEncounterDatetime().after(startDate))
                .filter(e-> endDate == null || e.getEncounterDatetime().before(endDate))
                .map(immunizationTransformer::build)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
    private Map<AbdmConfig.ImmunizationAttribute, Concept> getImmunizationAttributeConcepts() {
        return abdmConfig.getImmunizationAttributeConfigs().entrySet()
                .stream()
                .collect(HashMap::new, (m, v) -> m.put(v.getKey(),
                        abdmConfig.getImmunizationAttributeConcept(v.getKey())), HashMap::putAll);
    }


    private boolean isImmunizationObsTemplateConfigured() {
        return Optional.ofNullable(abdmConfig.getImmunizationObsRootConcept()).map((c) -> true).orElse(false);
    }

}
