package org.bahmni.module.hip.web.service;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.hip.web.model.DrugOrders;
import org.bahmni.module.hip.web.model.HealthDocumentRecordBundle;
import org.bahmni.module.hip.web.model.OrganizationContext;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Visit;
import org.openmrs.api.ConceptService;
import org.openmrs.api.VisitService;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HealthDocumentRecordService {

    private final VisitService visitService;
    private ConceptService conceptService;
    private OrganizationContextService organizationContextService;
    private FHIRResourceMapper fhirResourceMapper;
    private ConceptTranslator conceptTranslator;
    private EncounterTranslator<Encounter> encounterTranslator;
    private AbdmConfig abdmConfig;
    private FhirHealthDocumentRecordBuilder documentRecordBuilder;

    @Autowired
    public HealthDocumentRecordService(VisitService visitService, ConceptService conceptService,
                                       OrganizationContextService organizationContextService,
                                       FHIRResourceMapper fhirResourceMapper,
                                       ConceptTranslator conceptTranslator,
                                       EncounterTranslator<Encounter> encounterTranslator,
                                       AbdmConfig abdmConfig,
                                       FhirHealthDocumentRecordBuilder documentRecordBuilder) {

        this.visitService = visitService;
        this.conceptService = conceptService;
        this.organizationContextService = organizationContextService;
        this.fhirResourceMapper = fhirResourceMapper;
        this.conceptTranslator = conceptTranslator;
        this.encounterTranslator = encounterTranslator;
        this.abdmConfig = abdmConfig;
        this.documentRecordBuilder = documentRecordBuilder;
    }

    public List<HealthDocumentRecordBundle> getDocumentsForVisit(
            String visitUuid,
            String patientUuid,
            Date fromEncounterDate,
            Date toEncounterDate) {
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

        Concept documentConcept = abdmConfig.getDocumentConcept(AbdmConfig.DocumentKind.TEMPLATE);
        if (documentConcept == null) {
            //no document template configured
            log.info("Concept Document Template not found. Property [abdm.conceptMap.docType.template] is probably not defined");
            return Collections.emptyList();
        }
        Optional<Location> location = OrganizationContextService.findOrganization(visit.getLocation());
        OrganizationContext organizationContext = organizationContextService.buildContext(location);
        Map<Encounter, List<Obs>> encounterObsDocList = visit.getEncounters().stream()
                .filter(e -> fromEncounterDate == null || e.getEncounterDatetime().after(fromEncounterDate))
                .map(encounter -> encounter.getObsAtTopLevel(false))
                .flatMap(Collection::stream)
                .filter(obs -> obs.getConcept().getUuid().equals(documentConcept.getUuid()) && !isExternalOriginDoc(obs))
                .collect(Collectors.groupingBy(obs -> obs.getEncounter()));
        return encounterObsDocList.entrySet()
                .stream().map(entry -> documentRecordBuilder.build(entry.getKey(), entry.getValue(), organizationContext))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean isExternalOriginDoc(Obs obs) {
        if (obs.isObsGrouping()) {
            Concept externalOriginDocConcept = abdmConfig.getDocTemplateAtributeConcept(AbdmConfig.DocTemplateAttribute.EXTERNAL_ORIGIN);
            if (externalOriginDocConcept == null) {
                return false;
            }
            Optional<Obs> externalOriginObs = obs.getGroupMembers().stream().filter(o -> o.getConcept().getUuid().equals(externalOriginDocConcept.getUuid())).findFirst();
            if (externalOriginObs.isPresent()) {
                return !StringUtils.isEmpty(externalOriginObs.get().getValueText());
            }
        }
        return false;
    }
}
