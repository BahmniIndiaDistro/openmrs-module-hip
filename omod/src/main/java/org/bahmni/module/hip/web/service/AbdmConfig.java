package org.bahmni.module.hip.web.service;

import lombok.extern.slf4j.Slf4j;
import org.openmrs.Concept;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AbdmConfig {

    private static final String ABDM_PROPERTIES_FILE_NAME = "abdm_config.properties";
    private static final String CONCEPT_MAP_RESOLUTION_KEY = "abdm.conceptResolution";
    private final AdministrationService adminService;
    private ConceptService conceptService;

    private final List<String> allConfigurationKeys = new ArrayList<>();

    /**
     * Raw properties. No direct access
     */
    private final Properties properties = new Properties();

    /**
     * For now this is safe, as this is only initialized at initialization. Otherwise, have to use a
     * Concurrent HashMap and then ensure key and value to be not null. If value can be null then have
     * Optional as value
     */
    private final Map<ImmunizationAttribute, String> immunizationAttributesMap = new HashMap<>();
    private final Map<ProcedureAttribute, String> procedureAttributesMap = new HashMap<>();
    private final HashMap<WellnessAttribute, String> wellnessAttributeStringHashMap = new HashMap<>();
    private final Map<String, Integer> conceptCache = new ConcurrentHashMap<>();

    @Autowired
    public AbdmConfig(@Qualifier("adminService") AdministrationService adminService,
                      ConceptService conceptService) {
        this.adminService = adminService;
        this.conceptService = conceptService;
        Arrays.stream(ImmunizationAttribute.values()).forEach(immunizationAttribute -> {
            immunizationAttributesMap.put(immunizationAttribute, "");
            allConfigurationKeys.add(immunizationAttribute.getMapping());
        });

        Arrays.stream(HiTypeDocumentKind.values()).forEach(document -> {
            allConfigurationKeys.add(document.getMapping());
        });

        Arrays.stream(DocTemplateAttribute.values()).forEach(templateAttribute -> {
            allConfigurationKeys.add(templateAttribute.getMapping());
        });

        Arrays.stream(PhysicalExamination.values()).forEach(templateAttribute -> {
            allConfigurationKeys.add(templateAttribute.getMapping());
        });

        Arrays.stream(HistoryAndExamination.values()).forEach(templateAttribute -> {
            allConfigurationKeys.add(templateAttribute.getMapping());
        });

        Arrays.stream(WellnessAttribute.values()).forEach(templateAttribute -> {
            allConfigurationKeys.add(templateAttribute.getMapping());
        });

        Arrays.stream(ProcedureAttribute.values()).forEach(templateAttribute -> {
            allConfigurationKeys.add(templateAttribute.getMapping());
        });

        Arrays.stream(OpConsultAttribute.values()).forEach(templateAttribute -> {
            allConfigurationKeys.add(templateAttribute.getMapping());
        });

        allConfigurationKeys.add(CONCEPT_MAP_RESOLUTION_KEY);
    }


    public enum HiTypeDocumentKind {
        OP_CONSULT("abdm.conceptMap.document.opConsult"),
        DISCHARGE_SUMMARY("abdm.conceptMap.document.dischargeSummary"),
        DIAGNOSTIC_REPORT("abdm.conceptMap.document.diagnosticReport"),
        WELLNESS_RECORD("abdm.conceptMap.document.wellnessRecord"),
        PRESCRIPTION("abdm.conceptMap.document.prescription"),
        HEALTH_DOCUMENT_RECORD("abdm.conceptMap.document.healthDocumentRecord");

        private final String mapping;

        HiTypeDocumentKind(String mapping) {
            this.mapping = mapping;
        }

        public String getMapping() {
            return mapping;
        }

    }
    public enum DocTemplateAttribute {
        TEMPLATE("abdm.conceptMap.docTemplate.template"),
        DOC_TYPE("abdm.conceptMap.docTemplate.docType"),
        ATTACHMENT("abdm.conceptMap.docTemplate.attachment"),
        UPLOAD_REF("abdm.conceptMap.docTemplate.uploadRef"),
        EXTERNAL_ORIGIN("abdm.conceptMap.docTemplate.externalOrigin"),
        DATE_OF_DOCUMENT("abdm.conceptMap.docTemplate.documentDate");;
        private final String mapping;

        DocTemplateAttribute(String mapping) {
            this.mapping = mapping;
        }

        public String getMapping() {
            return mapping;
        }

    }

    public enum ImmunizationAttribute {
        VACCINE_CODE("abdm.conceptMap.immunization.vaccineCode"),
        OCCURRENCE_DATE("abdm.conceptMap.immunization.occurrenceDateTime"),
        MANUFACTURER("abdm.conceptMap.immunization.manufacturer"),
        BRAND_NAME("abdm.conceptMap.immunization.brandName"),
        DOSE_NUMBER("abdm.conceptMap.immunization.doseNumber"),
        LOT_NUMBER("abdm.conceptMap.immunization.lotNumber"),
        EXPIRATION_DATE("abdm.conceptMap.immunization.expirationDate"),
        TEMPLATE("abdm.conceptMap.immunization.template"),
        STATUS("abdm.conceptMap.immunization.status"),
        VACCINE_NON_CODED("abdm.conceptMap.immunization.vaccineNonCoded");

        private final String mapping;

        ImmunizationAttribute(String mapping) {
            this.mapping = mapping;
        }

        public String getMapping() {
            return mapping;
        }
    }

    public enum WellnessAttribute {
        VITAL_SIGNS("abdm.conceptMap.wellness.vitalSigns"),
        BODY_MEASUREMENT("abdm.conceptMap.wellness.bodyMeasurement"),
        PHYSICAL_ACTIVITY("abdm.conceptMap.wellness.physicalActivity"),
        GENERAL_ASSESSMENT("abdm.conceptMap.wellness.generalAssessment"),
        WOMEN_HEALTH("abdm.conceptMap.wellness.womenHealth"),
        LIFESTYLE("abdm.conceptMap.wellness.lifestyle"),
        OTHER_OBSERVATIONS("abdm.conceptMap.wellness.otherObservations"),
        DOCUMENT_REFERENCE("abdm.conceptMap.wellness.documentReference");

        private final String mapping;

        WellnessAttribute(String mapping) {
            this.mapping = mapping;
        }
        public String getMapping() {
            return mapping;
        }
    }
    public enum PhysicalExamination {
        HEIGHT("abdm.conceptMap.physicalExamination.height"),
        WEIGHT("abdm.conceptMap.physicalExamination.weight"),
        TEMPERATURE("abdm.conceptMap.physicalExamination.temperature"),
        SYSTOLICBP("abdm.conceptMap.physicalExamination.systolicBP"),
        DIASTOLICBP("abdm.conceptMap.physicalExamination.diastolicBP"),
        PULSE("abdm.conceptMap.physicalExamination.pulse");

        private final String mapping;

        PhysicalExamination(String mapping) {
            this.mapping = mapping;
        }

        public String getMapping() {
            return mapping;
        }
    }

    public enum HistoryAndExamination {
        CHIFF_COMPLAINT_TEMPLATE("abdm.conceptMap.historyExamination.chiefComplaintTemplate"),
        CHIEF_COMPLAINT_CODED("abdm.conceptMap.historyExamination.codedChiefComplaint"),
        CHIEF_COMPLAINT_NON_CODED("abdm.conceptMap.historyExamination.nonCodedChiefComplaint"),
        SIGN_SYMPTOM_DURATION("abdm.conceptMap.historyExamination.signAndSymptomDuration"),
        CHIEF_COMPLAINT_DURATION("abdm.conceptMap.historyExamination.chiefComplainDuration");

        private final String mapping;

        HistoryAndExamination(String mapping) {
            this.mapping = mapping;
        }

        public String getMapping() {
            return mapping;
        }
    }

    public enum ProcedureAttribute {
        PROCEDURE_TEMPLATE("abdm.conceptMap.procedure.procedureTemplate"),
        PROCEDURE_NAME("abdm.conceptMap.procedure.procedureName"),
        PROCEDURE_NAME_NONCODED("abdm.conceptMap.procedure.procedureNameNonCoded"),
        PROCEDURE_START_DATETIME("abdm.conceptMap.procedure.procedureStartDate"),
        PROCEDURE_END_DATETIME("abdm.conceptMap.procedure.procedureEndDate"),
        PROCEDURE_BODYSITE("abdm.conceptMap.procedure.procedureBodySite"),
        PROCEDURE_NONCODED_BODYSITE("abdm.conceptMap.procedure.procedureNonCodedBodySite"),
        PROCEDURE_OUTCOME("abdm.conceptMap.procedure.procedureOutcome"),
        PROCEDURE_NONCODED_OUTCOME("abdm.conceptMap.procedure.procedureOutcomeNonCoded"),
        PROCEDURE_NOTE("abdm.conceptMap.procedure.procedureNote");

        private final String mapping;

        ProcedureAttribute(String mapping) {
            this.mapping = mapping;
        }

        public String getMapping() {
            return mapping;
        }
    }

    public enum OpConsultAttribute {
        OTHER_OBSERVATIONS("abdm.conceptMap.opConsult.otherObservations");

        private final String mapping;

        OpConsultAttribute(String mapping) {
            this.mapping = mapping;
        }
        public String getMapping() {
            return mapping;
        }
    }

    public Concept getProcedureObsRootConcept() {
        return lookupConcept(ProcedureAttribute.PROCEDURE_TEMPLATE.getMapping());
    }

    public Map<ProcedureAttribute, String> getProcedureAttributesMap() {
        return procedureAttributesMap;
    }

    public Concept getChiefComplaintObsRootConcept() {
        return lookupConcept(HistoryAndExamination.CHIFF_COMPLAINT_TEMPLATE.getMapping());
    }

    public Concept getHnEConcept(HistoryAndExamination type) {
        return lookupConcept(type.getMapping());
    }

    public List<Concept> getHistoryExaminationConcepts(){
        List<Concept> conceptList = new ArrayList<>();
        Arrays.stream(HistoryAndExamination.values()).forEach(attribute ->
                conceptList.add(lookupConcept(attribute.getMapping())));
        return conceptList.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<Concept> getPhysicalExaminationConcepts(){
        List<Concept> conceptList = new ArrayList<>();
        Arrays.stream(PhysicalExamination.values()).forEach(attribute ->
                conceptList.add(lookupConcept(attribute.getMapping())));
        return conceptList.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    public Map<ImmunizationAttribute, String> getImmunizationAttributeConfigs() {
        return immunizationAttributesMap;
    }

    public Map<WellnessAttribute, String> getWellnessAttributeConfigs() {
        return wellnessAttributeStringHashMap;
    }
    public Concept getImmunizationObsRootConcept() {
        return getImmunizationAttributeConcept(ImmunizationAttribute.TEMPLATE);
    }

    private Concept lookupConcept(String lookupKey) {
        String lookupValue = (String) properties.get(lookupKey);
        if (StringUtils.isEmpty(lookupValue)) {
            log.info(String.format("Property [%s] is not set. System may not be able to send data", lookupKey));
            return null;
        }
        return retrieveConcept(lookupValue);
    }

    private Concept retrieveConcept(String lookupValue) {
        return Optional.ofNullable(conceptCache.get(lookupValue))
                .map(conceptId -> conceptService.getConcept(conceptId))
                .orElseGet(() -> {
                    Concept concept = conceptService.getConceptByUuid(lookupValue);
                    if (concept != null) {
                        conceptCache.put(lookupValue, concept.getConceptId().intValue());
                    }
                    return concept;
                });
    }

    private List<Concept> lookupConcepts(String lookupKey) {
        String lookupValues = (String) properties.get(lookupKey);
        if (StringUtils.isEmpty(lookupValues)) {
            log.info(String.format("Property [%s] is not set. System may not be able to send data", lookupKey));
            return Collections.emptyList();
        }
        return retrieveConcepts(lookupValues);
    }

    private List<Concept> retrieveConcepts(String lookupValues) {
        List<String> lookupValueList = Arrays.asList(lookupValues.split(","));
        return lookupValueList.stream().map(lookupValue -> Optional.ofNullable(conceptCache.get(lookupValue))
				.map(conceptId -> conceptService.getConcept(conceptId))
                .orElseGet(() -> {
                    Concept concept = conceptService.getConceptByUuid(lookupValue);
                    if (concept != null) {
                        conceptCache.put(lookupValue, concept.getConceptId().intValue());
                    }
                    return concept;
                })).collect(Collectors.toList());
    }

    public Concept getDocTemplateAtributeConcept(DocTemplateAttribute docAttribute) {
        return lookupConcept(docAttribute.getMapping());
    }

    public Concept getImmunizationAttributeConcept(ImmunizationAttribute type) {
        return lookupConcept(type.getMapping());
    }
    public List<Concept> getWellnessAttributeConcept(WellnessAttribute type) {
        return lookupConcepts(type.getMapping());
    }
    public List<Concept> getHiTypeDocumentTypes(HiTypeDocumentKind type) {
        return lookupConcepts(type.getMapping());
    }

    public List<Concept> getOPConsultAttributeConcept(OpConsultAttribute type) {
        return lookupConcepts(type.getMapping());
    }

    public List<String> getAllConfigurationKeys() {
        return allConfigurationKeys;
    }

    public String getConceptMapResolver() {
        String resolution = (String) properties.get(CONCEPT_MAP_RESOLUTION_KEY);
        return resolution != null ? resolution : "UUID";
    }

    public static AbdmConfig instanceFrom(Properties props, AdministrationService adminService, ConceptService conceptService) {
        AbdmConfig instance = new AbdmConfig(adminService, conceptService);
        instance.properties.putAll(props);
        updateImmunizationAttributeMap(instance);
        updateWellnessAttributeMap(instance);
        return instance;
    }

    private static void updateImmunizationAttributeMap(AbdmConfig conf) {
        Arrays.stream(ImmunizationAttribute.values()).forEach(conceptAttribute ->
           conf.immunizationAttributesMap.put(conceptAttribute, (String) conf.properties.get(conceptAttribute.getMapping())));
    }

    private static void updateWellnessAttributeMap(AbdmConfig conf) {
        Arrays.stream(WellnessAttribute.values()).forEach(conceptAttribute ->
                conf.wellnessAttributeStringHashMap.put(conceptAttribute, (String) conf.properties.get(conceptAttribute.getMapping())));
    }

    public Map<AbdmConfig.ProcedureAttribute, Concept> getProcedureAttributeConcepts() {
        return getProcedureAttributesMap().entrySet()
                .stream()
                .collect(HashMap::new, (m, v) -> m.put(v.getKey(),
                        lookupConcept(v.getKey().getMapping())), HashMap::putAll);
    }

    private static void updateProcedureAttributeMap(AbdmConfig conf) {
        Arrays.stream(ProcedureAttribute.values()).forEach(conceptAttribute ->
                conf.procedureAttributesMap.put(conceptAttribute, (String) conf.properties.get(conceptAttribute.getMapping())));
    }

    @PostConstruct
    private void postConstruct() {
        Path configFilePath = Paths.get(OpenmrsUtil.getApplicationDataDirectory(), ABDM_PROPERTIES_FILE_NAME);
        if (!Files.exists(configFilePath)) {
            log.warn(String.format("ABDM config file does not exist: [%s]. Trying to read from Global Properties", configFilePath));
            readFromGlobalProperties();
            return;
        }
        log.info(String.format("Reading  ABDM config properties from : %s", configFilePath));
        try (InputStream configFile = Files.newInputStream(configFilePath)) {
            properties.load(configFile);
            updateImmunizationAttributeMap(this);
            updateWellnessAttributeMap(this);
            updateProcedureAttributeMap(this);
        } catch (IOException e) {
            log.error("Error Occurred while trying to read ABDM config file", e);
        }
    }

    private void readFromGlobalProperties() {
        allConfigurationKeys.forEach(key -> {
            String value = adminService.getGlobalProperty(key);
            if (!StringUtils.isEmpty(value)) {
                properties.put(key, value);
            } else {
                log.warn("ABDM: No property set for " + key);
            }
        });
        updateImmunizationAttributeMap(this);
        updateWellnessAttributeMap(this);
        updateProcedureAttributeMap(this);
    }
}
