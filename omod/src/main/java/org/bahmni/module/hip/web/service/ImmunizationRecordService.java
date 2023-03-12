package org.bahmni.module.hip.web.service;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.hip.web.model.ImmunizationRecordBundle;
import org.openmrs.Concept;
import org.openmrs.Encounter;
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
import java.util.stream.Collectors;

@Slf4j
@Service
public class ImmunizationRecordService {

    private final ImmunizationObsTemplateConfig immunizationObsTemplateConfig;
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
                                     ImmunizationObsTemplateConfig immunizationObsTemplateConfig) {
        this.visitService = visitService;
        this.conceptService = conceptService;
        this.organizationContextService = organizationContextService;
        this.fhirResourceMapper = fhirResourceMapper;
        this.conceptTranslator = conceptTranslator;
        this.encounterTranslator = encounterTranslator;
        this.immunizationObsTemplateConfig = immunizationObsTemplateConfig;
    }

    public List<ImmunizationRecordBundle> getImmunizationRecordsForVisit(String patientUuid, String visitUuid, Date startDate, Date endDate) {
        Visit visit = visitService.getVisitByUuid(visitUuid);
        if (!visit.getPatient().getUuid().equals(patientUuid.trim())) {
            log.warn("Identified visit is not for the requested patient. " +
                    "This should never happen. This may mean a invalid linkage in the care context");
            return Collections.emptyList();
        }
        if (!isImmunizationObsTemplateConfigured()) {
           //no form template configured
            return Collections.emptyList();
        }

        Map<ImmunizationObsTemplateConfig.ImmunizationAttribute, Concept> immunizationAttributeConceptMap =
                immunizationObsTemplateConfig.getImmunizationAttributeConfigs().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> identifyConcept(e)));

        FhirImmunizationRecordBundleBuilder immunizationTransformer =
                new FhirImmunizationRecordBundleBuilder(fhirResourceMapper,
                        conceptTranslator, encounterTranslator,
                        organizationContextService.buildContext(),
                        immunizationAttributeConceptMap);

        return visit.getEncounters().stream()
                .filter(e -> startDate == null || e.getEncounterDatetime().after(startDate))
                .map(immunizationTransformer::build)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private Concept identifyConcept(Map.Entry<ImmunizationObsTemplateConfig.ImmunizationAttribute, String> entry) {
        return conceptService.getConceptByUuid(entry.getValue());
    }


    private boolean isImmunizationObsTemplateConfigured() {
        return !StringUtils.isEmpty(immunizationObsTemplateConfig.getRootConcept());
    }

}
