package org.bahmni.module.hip.service;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.hip.Config;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.Concept;
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

    public <T extends Resource> T transForm(Obs obs, Class<T> claz, AbdmConfig.HiTypeDocumentKind hiTypeDocumentKind) {
        if (!supportedFhirResources.contains(claz)) {
            log.warn("Can not transform requested document resource : " + claz.getName());
            return null;
        }
        System.out.println("obs.isObsGrouping() = " + obs.isObsGrouping());
        System.out.println("isDocumentTemplate(obs) = " + isDocumentTemplate(obs));
        System.out.println("obs.getConcept().getUuid() = " + obs.getConcept().getUuid());
        if (obs.isObsGrouping()) {
            if(isDocumentTemplate(obs)) {
                if (Binary.class.equals(claz)) {
                    return (T) getBinaryFromTemplate(obs, hiTypeDocumentKind);
                } else if (DocumentReference.class.equals(claz)) {
                    return (T) getDocumentRefFromTemplate(obs, hiTypeDocumentKind);
                }
            }
            else {
                if (Binary.class.equals(claz)) {
                    return (T) getBinaryFromPatientUpload(obs);
                } else if (DocumentReference.class.equals(claz)) {
                    return (T) getDocumentRef(obs, hiTypeDocumentKind);
                }
            }
        } else if (Binary.class.equals(claz)) {
             return (T) getBinaryDocument(obs);
        }
        log.warn(String.format("Can not transform document from obs [%d] for specified type [%s]", obs.getId(), claz.getName()));
        return null;
    }

    private DocumentReference getDocumentRefFromTemplate(Obs obs, AbdmConfig.HiTypeDocumentKind hiTypeDocumentKind) {
        Concept attachmentConcept = config.getDocTemplateAtributeConcept(AbdmConfig.DocTemplateAttribute.ATTACHMENT);
        if (attachmentConcept == null) return null;

        Concept docTypeField = config.getDocTemplateAtributeConcept(AbdmConfig.DocTemplateAttribute.DOC_TYPE);
        if (docTypeField == null) {
            log.warn(
               String.format("Can not identify Document Type from captured document template. Have you set property [%s] ?",
                       AbdmConfig.DocTemplateAttribute.DOC_TYPE.getMapping()));
            return null;
        }

        Concept capturedDocType = getDocumentConcept(obs, hiTypeDocumentKind, docTypeField);

        if(capturedDocType != null) {

            Optional<Attachment> attachment = obs.getGroupMembers().stream()
                    .filter(member -> member.getConcept().equals(attachmentConcept))
                    .findFirst()
                    .map(o -> getAttachment(o, capturedDocType, hiTypeDocumentKind));

            if (!attachment.isPresent()) {
                log.warn("Can not find Document attachment in the captured document template.");
                return null;
            }

            Concept dateConcept = config.getDocTemplateAtributeConcept(AbdmConfig.DocTemplateAttribute.DATE_OF_DOCUMENT);

            Optional<Obs> dateObs = obs.getGroupMembers().stream()
                    .filter(member -> member.getConcept().equals(dateConcept))
                    .findFirst();

            CodeableConcept docRefType = capturedDocType.getConceptMappings().isEmpty()
                    ? FHIRUtils.getPatientRecordType()
                    : conceptTranslator.toFhirResource(capturedDocType);
            DocumentReference documentReference = new DocumentReference();
            documentReference.setId(obs.getObsId().toString());
            dateObs.ifPresent(value -> documentReference.setDate(value.getValueDatetime()));
            documentReference
                    .setStatus(Enumerations.DocumentReferenceStatus.CURRENT)
                    .setType(docRefType)
                    .addContent()
                    .setAttachment(attachment.get());
            return documentReference;
        }
        return null;
    }

    private DocumentReference getDocumentRef(Obs obs, AbdmConfig.HiTypeDocumentKind hiTypeDocumentKind) {
        Optional<Obs> attachmentObs = obs.getGroupMembers().stream()
                .filter(member -> member.getConcept().getName().getName().equals(Config.DOCUMENT_TYPE.getValue()))
                .findFirst();

        if(!attachmentObs.isPresent()) {
            return null;
        }

        Attachment attachment = getAttachment(attachmentObs.get(),obs.getConcept(),hiTypeDocumentKind);

        if (attachment == null) {
            log.warn("Can not find Document attachment in the captured document template.");
            return null;
        }

        CodeableConcept docRefType = attachmentObs.get().getConcept().getConceptMappings().isEmpty()
                ? FHIRUtils.getPatientRecordType()
                : conceptTranslator.toFhirResource(attachmentObs.get().getConcept());
        DocumentReference documentReference = new DocumentReference();
        documentReference.setId(obs.getObsId().toString());
        documentReference
                .setStatus(Enumerations.DocumentReferenceStatus.CURRENT)
                .setType(docRefType)
                .addContent()
                .setAttachment(attachment);
        return documentReference;
    }

    public Attachment getAttachment(Obs obs, Concept capturedDocType, AbdmConfig.HiTypeDocumentKind hiTypeDocumentKind) {
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
            attachment.setTitle(getAttachmentName(hiTypeDocumentKind, (capturedDocType != null ? capturedDocType : obs.getConcept())));
            return attachment;
        } catch (IOException | RuntimeException ex) {
            log.error(String.format("Could not load file associated with observation for %s", ex.getMessage()));
            return null;
        }
    }

    private String getAttachmentName(AbdmConfig.HiTypeDocumentKind hiTypeDocumentKind, Concept capturedDocType) {
        String title;
        if(hiTypeDocumentKind == AbdmConfig.HiTypeDocumentKind.OP_CONSULT)
            title = "OP Consultation - ";
        else if(hiTypeDocumentKind == AbdmConfig.HiTypeDocumentKind.DISCHARGE_SUMMARY)
            title = "Discharge Summary - ";
        else if(hiTypeDocumentKind == AbdmConfig.HiTypeDocumentKind.DIAGNOSTIC_REPORT)
            title = "Diagnostic Report - ";
        else if(hiTypeDocumentKind == AbdmConfig.HiTypeDocumentKind.WELLNESS_RECORD)
            title = "Wellness Record - ";
        else if(hiTypeDocumentKind == AbdmConfig.HiTypeDocumentKind.HEALTH_DOCUMENT_RECORD)
            title = "Health Document - ";
        else if(hiTypeDocumentKind == AbdmConfig.HiTypeDocumentKind.PRESCRIPTION)
            title = "Prescription - ";
        else
            title = "Document - ";
        System.out.println("title: " + title + " " + capturedDocType.getDisplayString());
        title = title + capturedDocType.getDisplayString();
        return title;
    }

    private Binary getBinaryFromTemplate(Obs obs, AbdmConfig.HiTypeDocumentKind hiTypeDocumentKind) {
        Concept docTypeField = config.getDocTemplateAtributeConcept(AbdmConfig.DocTemplateAttribute.DOC_TYPE);

        Concept capturedDocType = getDocumentConcept(obs, hiTypeDocumentKind, docTypeField);
        if(capturedDocType == null) return null;

        Concept attachmentConcept = config.getDocTemplateAtributeConcept(AbdmConfig.DocTemplateAttribute.ATTACHMENT);
        if (attachmentConcept == null) return null;
        return obs.getGroupMembers().stream()
                .filter(member -> member.getConcept().getUuid().equals(attachmentConcept.getUuid()))
                .findFirst()
                .map(this::getBinaryDocument)
                .orElse(null);
    }

    private Binary getBinaryFromPatientUpload(Obs obs) {

        Optional<Obs> attachmentObs = obs.getGroupMembers().stream()
                .findFirst();

        if(!attachmentObs.isPresent()) {
            return null;
        }
        return attachmentObs
                .map(this::getBinaryDocument)
                .orElse(null);
    }

    public boolean isSupportedDocument(Obs obs, AbdmConfig.DocTemplateAttribute type) {
        Concept documentConcept = config.getDocTemplateAtributeConcept(type);
        return documentConcept != null && obs.getConcept().getUuid().equals(documentConcept.getUuid());
    }

    public boolean isSupportedHiTypeDocument(Concept concept, AbdmConfig.HiTypeDocumentKind type) {
        List<Concept> supportedConcepts = config.getHiTypeDocumentTypes(type);
        if(supportedConcepts.contains(concept)) {
            return true;
        }
        return false;
    }

    public Concept getDocumentConcept(Obs obs, AbdmConfig.HiTypeDocumentKind type, Concept docTypeField) {
        Optional<Concept> capturedDocType = obs.getGroupMembers().stream()
                .filter(member -> member.getConcept().getUuid().equals(docTypeField.getUuid()))
                .map(Obs::getValueCoded).findFirst();

        if (!capturedDocType.isPresent()) {
            log.warn("Can not identify captured Document Type. This must be captured against the DocType observation as coded value (obs.value_coded).");
            return null;
        }

        return capturedDocType.get();
    }

    private boolean isDocumentTemplate(Obs obs) {
        Concept documentConcept = config.getDocTemplateAtributeConcept(AbdmConfig.DocTemplateAttribute.TEMPLATE);
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
//        ConceptDatatype datatype = obs.getConcept().getDatatype();
//        //currently only supporting complex data type
//        if (!datatype.isComplex()) {
//            throw new RuntimeException("Can not translate concept as document for non-complex datatypes");
//        }
        return obs.getValueComplex() != null ? obs.getValueComplex() : obs.getValueText();
    }
}
