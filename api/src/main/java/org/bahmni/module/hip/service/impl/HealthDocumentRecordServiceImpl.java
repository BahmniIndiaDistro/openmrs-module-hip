package org.bahmni.module.hip.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.hip.builder.FhirHealthDocumentRecordBuilder;
import org.bahmni.module.hip.config.AbdmConfig;
import org.bahmni.module.hip.model.HealthDocumentRecordBundle;
import org.bahmni.module.hip.model.OrganizationContext;
import org.bahmni.module.hip.service.ConsultationService;
import org.bahmni.module.hip.mapper.FHIRResourceMapper;
import org.bahmni.module.hip.service.HealthDocumentRecordService;
import org.bahmni.module.hip.service.OrganizationContextService;
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

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HealthDocumentRecordServiceImpl implements HealthDocumentRecordService {

    private final VisitService visitService;
    private ConceptService conceptService;
    private OrganizationContextService organizationContextService;
    private FHIRResourceMapper fhirResourceMapper;
    private ConceptTranslator conceptTranslator;
    private EncounterTranslator<Encounter> encounterTranslator;
    private AbdmConfig abdmConfig;
    private FhirHealthDocumentRecordBuilder documentRecordBuilder;
    private ConsultationService consultationService;

    @Autowired
    public HealthDocumentRecordServiceImpl(VisitService visitService, ConceptService conceptService,
                                           OrganizationContextService organizationContextService,
                                           FHIRResourceMapper fhirResourceMapper,
                                           ConceptTranslator conceptTranslator,
                                           EncounterTranslator<Encounter> encounterTranslator,
                                           AbdmConfig abdmConfig,
                                           FhirHealthDocumentRecordBuilder documentRecordBuilder, ConsultationService consultationService) {

        this.visitService = visitService;
        this.conceptService = conceptService;
        this.organizationContextService = organizationContextService;
        this.fhirResourceMapper = fhirResourceMapper;
        this.conceptTranslator = conceptTranslator;
        this.encounterTranslator = encounterTranslator;
        this.abdmConfig = abdmConfig;
        this.documentRecordBuilder = documentRecordBuilder;
        this.consultationService = consultationService;
    }

    @Override
    public List<HealthDocumentRecordBundle> getDocumentsForVisit(
            String patientUuid,
            String visitUuid,
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

        Concept documentConcept = abdmConfig.getDocTemplateAtributeConcept(AbdmConfig.DocTemplateAttribute.TEMPLATE);
        if (documentConcept == null) {
            //no document template configured
            log.info("Concept Document Template not found. Property [abdm.conceptMap.docType.template] is probably not defined");
            return Collections.emptyList();
        }
        Optional<Location> location = OrganizationContextService.findOrganization(visit.getLocation());
        OrganizationContext organizationContext = organizationContextService.buildContext(location);
        Map<Encounter, List<Obs>> encounterObsDocList = consultationService.getEncounterPatientDocumentsMap(visit, fromEncounterDate, toEncounterDate, AbdmConfig.HiTypeDocumentKind.HEALTH_DOCUMENT_RECORD);

        return encounterObsDocList.entrySet()
                .stream().map(entry -> documentRecordBuilder.build(entry.getKey(), entry.getValue(), organizationContext))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


}
