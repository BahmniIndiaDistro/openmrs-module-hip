package org.bahmni.module.hip.web.service;

import lombok.extern.slf4j.Slf4j;
import org.openmrs.Concept;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Component
public class AbdmConfig {

    private static final String ABDM_PROPERTIES_FILE_NAME = "abdm_config.properties";
    private static final String CONCEPT_MAP_RESOLUTION_KEY = "abdm.conceptResolution";
    private static final String CONCEPT_MAP_PRESCRIPTION_DOCUMENT = "abdm.conceptMap.prescription.document";
    private final AdministrationService adminService;

    public enum ImmunizationAttribute {
        VACCINE_CODE("abdm.conceptMap.immunization.vaccineCode"),
        OCCURRENCE_DATE("abdm.conceptMap.immunization.occurrenceDateTime"),
        MANUFACTURER("abdm.conceptMap.immunization.manufacturer"),
        BRAND_NAME("abdm.conceptMap.immunization.brandName"),
        DOSE_NUMBER("abdm.conceptMap.immunization.doseNumber"),
        LOT_NUMBER("abdm.conceptMap.immunization.lotNumber"),
        EXPIRATION_DATE("abdm.conceptMap.immunization.expirationDate"),
        ROOT_CONCEPT("abdm.conceptMap.immunization.root");

        private final String configKey;

        ImmunizationAttribute(String configKey) {
            this.configKey = configKey;
        }

        public String getConfigKey() {
            return configKey;
        }
    }

    private final List<String> allConfigurationKeys = new ArrayList<>();

    private final Properties properties = new Properties();

    private final HashMap<ImmunizationAttribute, String> immunizationAttributesMap = new HashMap<>();

    @Autowired
    public AbdmConfig(@Qualifier("adminService") AdministrationService adminService) {
        this.adminService = adminService;
        Arrays.stream(ImmunizationAttribute.values()).forEach(immunizationAttribute -> {
            immunizationAttributesMap.put(immunizationAttribute, "");
            allConfigurationKeys.add(immunizationAttribute.getConfigKey());
        });
        allConfigurationKeys.addAll(Arrays.asList(CONCEPT_MAP_PRESCRIPTION_DOCUMENT, CONCEPT_MAP_RESOLUTION_KEY));
    }

    public Map<ImmunizationAttribute, String> getImmunizationAttributeConfigs() {
        return immunizationAttributesMap;
    }

    public String getImmunizationObsRootConcept() {
        return immunizationAttributesMap.get(ImmunizationAttribute.ROOT_CONCEPT);
    }

    public Concept getPrescriptionDocumentConcept() {
        String key = (String) properties.get(CONCEPT_MAP_PRESCRIPTION_DOCUMENT);
        if (StringUtils.isEmpty(key)) {
            log.info(String.format("Property [%s] is not set. System may not be able to send unstructured prescription document", CONCEPT_MAP_PRESCRIPTION_DOCUMENT));
            return null;
        }
        return Context.getConceptService().getConceptByUuid(key);
    }


    public List<String> getAllConfigurationKeys() {
        return allConfigurationKeys;
    }

    public String getConceptMapResolver() {
        String resolution = (String) properties.get(CONCEPT_MAP_RESOLUTION_KEY);
        return resolution != null ? resolution : "UUID";
    }

    public static AbdmConfig instanceFrom(Properties props, AdministrationService adminService) {
        AbdmConfig instance = new AbdmConfig(adminService);
        instance.properties.putAll(props);
        updateImmunizationAttributeMap(instance);
        return instance;
    }

    private static void updateImmunizationAttributeMap(AbdmConfig conf) {
        Arrays.stream(ImmunizationAttribute.values()).forEach(conceptAttribute ->
           conf.immunizationAttributesMap.put(conceptAttribute, (String) conf.properties.get(conceptAttribute.getConfigKey())));
    }

    @PostConstruct
    private void postConstruct() {
        Path configFilePath = Paths.get(OpenmrsUtil.getApplicationDataDirectory(), ABDM_PROPERTIES_FILE_NAME);
        if (!Files.exists(configFilePath)) {
            log.info(String.format("ABDM config file does not exist: [%s]. Trying to read from Global Properties", configFilePath));
            readFromGlobalProperties();
            return;
        }
        log.info(String.format("Reading  properties from : %s", configFilePath));
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
