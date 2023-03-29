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
import java.util.*;

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

    private final HashMap<ImmunizationAttribute, String> immunizationAttributesMap = new HashMap<>();
    private final Map<String, Concept> conceptCache = new HashMap<>();

    @Autowired
    public AbdmConfig(@Qualifier("adminService") AdministrationService adminService,
                      ConceptService conceptService) {
        this.adminService = adminService;
        this.conceptService = conceptService;
        Arrays.stream(ImmunizationAttribute.values()).forEach(immunizationAttribute -> {
            immunizationAttributesMap.put(immunizationAttribute, "");
            allConfigurationKeys.add(immunizationAttribute.getMapping());
        });

        Arrays.stream(DocumentKind.values()).forEach(documentKind -> {
            allConfigurationKeys.add(documentKind.getMapping());
        });

        Arrays.stream(DocTemplateAttribute.values()).forEach(templateAttribute -> {
            allConfigurationKeys.add(templateAttribute.getMapping());
        });
        allConfigurationKeys.add(CONCEPT_MAP_RESOLUTION_KEY);
    }

    public enum DocumentKind {
        PRESCIPTION("abdm.conceptMap.docType.prescription"),
        DISCHARGE_SUMMARY("abdm.conceptMap.docType.dischargeSummary"),
        PATIENT_FILE("abdm.conceptMap.docType.patientFile"),
        REFERRAL("abdm.conceptMap.docType.referral"),
        TEMPLATE("abdm.conceptMap.docType.template");
        private final String mapping;

        DocumentKind(String mapping) {
            this.mapping = mapping;
        }

        public String getMapping() {
            return mapping;
        }

    }
    public enum DocTemplateAttribute {
        DOC_TYPE("abdm.conceptMap.docTemplate.docType"),
        ATTACHMENT("abdm.conceptMap.docTemplate.attachment"),
        UPLOAD_REF("abdm.conceptMap.docTemplate.uploadRef");
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

    public Map<ImmunizationAttribute, String> getImmunizationAttributeConfigs() {
        return immunizationAttributesMap;
    }

    public Concept getImmunizationObsRootConcept() {
        return getImmunizationAttributeConcept(ImmunizationAttribute.TEMPLATE);
    }

    public Concept getPrescriptionDocumentConcept() {
        return lookupConcept(DocumentKind.PRESCIPTION.getMapping());
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
        //should check with resolution (UUID now)
        return Optional.ofNullable(conceptCache.get(lookupValue))
                .orElseGet(() -> {
                    Concept concept = conceptService.getConceptByUuid(lookupValue);
                    conceptCache.put(lookupValue, concept);
                    return concept;
                });
    }


    public Concept getDocumentConcept(DocumentKind type) {
        return lookupConcept(type.getMapping());
    }

    public Concept getDocTemplateAtributeConcept(DocTemplateAttribute docAttribute) {
        return lookupConcept(docAttribute.getMapping());
    }

    public Concept getImmunizationAttributeConcept(ImmunizationAttribute type) {
        return lookupConcept(type.getMapping());
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
        return instance;
    }

    private static void updateImmunizationAttributeMap(AbdmConfig conf) {
        Arrays.stream(ImmunizationAttribute.values()).forEach(conceptAttribute ->
           conf.immunizationAttributesMap.put(conceptAttribute, (String) conf.properties.get(conceptAttribute.getMapping())));
    }

    @PostConstruct
    private void postConstruct() {
        Path configFilePath = Paths.get(OpenmrsUtil.getApplicationDataDirectory(), ABDM_PROPERTIES_FILE_NAME);
        if (!Files.exists(configFilePath)) {
            log.info(String.format("ABDM config file does not exist: [%s]. Trying to read from Global Properties", configFilePath));
            readFromGlobalProperties();
            return;
        }
        log.info(String.format("Reading  ABDM config properties from : %s", configFilePath));
        try (InputStream configFile = Files.newInputStream(configFilePath)) {
            properties.load(configFile);
            updateImmunizationAttributeMap(this);
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
    }
}
