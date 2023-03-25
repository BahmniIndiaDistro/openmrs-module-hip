package org.bahmni.module.hip.web.service;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.hip.Config;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.Obs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class OmrsObsDocumentTransformer {

    private final AbdmConfig config;
    private final List<Class<? extends Resource>> supportedFhirResources = Arrays.asList(Binary.class, DocumentReference.class);


    @Autowired
    public OmrsObsDocumentTransformer(AbdmConfig config) {
        this.config = config;
    }

    public boolean isDocumentRef(Obs obs) {
        if (obs.isObsGrouping() && isSupportedDocument(obs, AbdmConfig.DocumentKind.TEMPLATE)) {
            return true;
        }
        return isSupportedDocumentType(obs);
    }

    public <T extends Resource> T transForm(Obs obs, Class<T> claz) {
        if (!supportedFhirResources.contains(claz)) {
            log.warn("Can not transform requested document resource : " + claz.getName());
            return null;
        }

        if (obs.isObsGrouping() && isDocumentTemplate(obs)) {
            if (Binary.class.equals(claz)) {
                return (T) getBinaryFromTemplate(obs);
            } else if (DocumentReference.class.equals(claz)) {
                return (T) getDocumentRefFromTemplate(obs);
            }
        } else {
            return (T) getPrescriptionDocument(obs);
        }
        return null;
    }

    private DocumentReference getDocumentRefFromTemplate(Obs obs) {
        throw new RuntimeException("Not yet implemented for Document Template");
    }

    private Binary getBinaryFromTemplate(Obs obs) {
        Concept attachmentConcept = config.getDocTemplateAtributeConcept(AbdmConfig.DocTemplateAttribute.ATTACHMENT);
        if (attachmentConcept == null) return null;
        return obs.getGroupMembers().stream()
                .filter(member -> member.getConcept().getUuid().equals(attachmentConcept.getUuid()))
                .findFirst()
                .map(this::getPrescriptionDocument)
                .orElse(null);
    }

    public boolean isSupportedDocumentType(Obs obs, List<AbdmConfig.DocumentKind> types) {
        return types.stream().anyMatch(type -> isSupportedDocument(obs, type));
    }

    private boolean isSupportedDocumentType(Obs obs) {
         return Arrays.stream(AbdmConfig.DocumentKind.values()).anyMatch(type -> isSupportedDocument(obs, type));
    }

    public boolean isSupportedDocument(Obs obs, AbdmConfig.DocumentKind type) {
        Concept documentConcept = config.getDocumentConcept(type);
        return documentConcept != null && obs.getConcept().getUuid().equals(documentConcept.getUuid());
    }

    private boolean isDocumentTemplate(Obs obs) {
        Concept documentConcept = config.getDocumentConcept(AbdmConfig.DocumentKind.TEMPLATE);
        return documentConcept != null && obs.getConcept().getUuid().equals(documentConcept.getUuid());
    }

    private Binary getPrescriptionDocument(Obs obs) {
        try {
            Path filePath = Paths.get(Config.PATIENT_DOCUMENTS_PATH.getValue(), getFileLocation(obs));
            if (!Files.exists(filePath)) {
                log.info(String.format("Can not read file: %s", filePath));
                return null;
            }
            byte[] fileContent = Files.readAllBytes(filePath);
            Binary binary = new Binary();
            binary
                .setContentType(FHIRUtils.getTypeOfTheObsDocument(getFileLocation(obs)))
                .setData(fileContent)
                .setId(UUID.randomUUID().toString());
            return binary;
        } catch (IOException | RuntimeException ex) {
            log.error("Could not load file associated with observation for prescription document", ex.getMessage());
            return null;
        }
    }

    private String getFileLocation(Obs obs) {
        ConceptDatatype datatype = obs.getConcept().getDatatype();
        //currently only supporting complex data type
        if (!datatype.isComplex()) {
            throw new RuntimeException("Can not translate concept as document for non-complex datatypes");
        }
        return obs.getValueComplex() != null ? obs.getValueComplex() : obs.getValueText();
    }
}
