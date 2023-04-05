package org.bahmni.module.hip.web.service;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.hip.Config;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
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
    private final ConceptTranslator conceptTranslator;
    private final List<Class<? extends Resource>> supportedFhirResources = Arrays.asList(Binary.class, DocumentReference.class);


    @Autowired
    public OmrsObsDocumentTransformer(AbdmConfig config, ConceptTranslator conceptTranslator) {
        this.config = config;
        this.conceptTranslator = conceptTranslator;
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
        } else if (Binary.class.equals(claz)) {
            return (T) getBinaryDocument(obs);
        }
        log.warn(String.format("Can not transform document from obs [%d] for specified type [%s]", obs.getId(), claz.getName()));
        return null;
    }

    private DocumentReference getDocumentRefFromTemplate(Obs obs) {
        Concept attachmentConcept = config.getDocTemplateAtributeConcept(AbdmConfig.DocTemplateAttribute.ATTACHMENT);
        if (attachmentConcept == null) return null;
        Optional<Attachment> attachment = obs.getGroupMembers().stream()
                .filter(member -> member.getConcept().getUuid().equals(attachmentConcept.getUuid()))
                .findFirst()
                .map(this::getAttachment);

        if (!attachment.isPresent()) {
            log.warn("Can not find Document attachment in the captured document template.");
            return null;
        }

        Concept docTypeField = config.getDocTemplateAtributeConcept(AbdmConfig.DocTemplateAttribute.DOC_TYPE);
        if (docTypeField == null) {
            log.warn(
               String.format("Can not identify Document Type from captured document template. Have you set property [%s] ?",
                       AbdmConfig.DocTemplateAttribute.DOC_TYPE.getMapping()));
            return null;
        }

        Optional<Concept> capturedDocType = obs.getGroupMembers().stream()
                .filter(member -> member.getConcept().getUuid().equals(docTypeField.getUuid()))
                .map(Obs::getValueCoded).findFirst();

        if (!capturedDocType.isPresent()) {
            log.warn("Can not identify captured Document Type. This must be captured against the DocType observation as coded value (obs.value_coded).");
            return null;
        }

        Concept patientFileConcept = config.getDocumentConcept(AbdmConfig.DocumentKind.PATIENT_FILE);
        if (patientFileConcept == null) {
            log.warn(
               String.format("Can not identify concept for Patient File. Have you set property [%s] ?",
                       AbdmConfig.DocumentKind.PATIENT_FILE.getMapping()));
            return null;
        }

        if (!capturedDocType.get().getUuid().equals(patientFileConcept.getUuid())) {
            log.warn("Document is not a Patient File. Can not share this document");
            return null;
        }

        CodeableConcept docRefType = capturedDocType.get().getConceptMappings().isEmpty()
                ? FHIRUtils.getPatientRecordType()
                : conceptTranslator.toFhirResource(capturedDocType.get());
        DocumentReference documentReference = new DocumentReference();
        documentReference.setId(obs.getObsId().toString());
        documentReference
                .setStatus(Enumerations.DocumentReferenceStatus.CURRENT)
                .setType(docRefType)
                .addContent()
                .setAttachment(attachment.get());
        return documentReference;
    }

    private Attachment getAttachment(Obs obs) {
        try {
            Path filePath = Paths.get(Config.PATIENT_DOCUMENTS_PATH.getValue(), getFileLocation(obs));
            if (!Files.exists(filePath)) {
                log.warn(String.format("Can not read file: %s", filePath));
                return null;
            }
            byte[] fileContent = Files.readAllBytes(filePath);
            Attachment attachment = new Attachment();
            attachment
                    .setContentType(FHIRUtils.getTypeOfTheObsDocument(getFileLocation(obs)))
                    .setData(fileContent)
                    .setId(UUID.randomUUID().toString());
            return attachment;
        } catch (IOException | RuntimeException ex) {
            log.error(String.format("Could not load file associated with observation for prescription document. %s", ex.getMessage()));
            return null;
        }
    }

    private Binary getBinaryFromTemplate(Obs obs) {
        Concept attachmentConcept = config.getDocTemplateAtributeConcept(AbdmConfig.DocTemplateAttribute.ATTACHMENT);
        if (attachmentConcept == null) return null;
        return obs.getGroupMembers().stream()
                .filter(member -> member.getConcept().getUuid().equals(attachmentConcept.getUuid()))
                .findFirst()
                .map(this::getBinaryDocument)
                .orElse(null);
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

    private Binary getBinaryDocument(Obs obs) {
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
            log.error(String.format("Could not load file associated with observation for prescription document. %s", ex.getMessage()));
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
