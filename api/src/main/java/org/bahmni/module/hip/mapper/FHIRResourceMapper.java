package org.bahmni.module.hip.mapper;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.hip.Config;
import org.bahmni.module.hip.builder.OmrsObsDocumentTransformer;
import org.bahmni.module.hip.config.AbdmConfig;
import org.bahmni.module.hip.constants.Constants;
import org.bahmni.module.hip.model.OpenMrsCondition;
import org.bahmni.module.hip.utils.FHIRUtils;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Dosage;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.r4.model.DateTimeType;

import org.openmrs.DrugOrder;
import org.openmrs.EncounterProvider;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Concept;
import org.openmrs.module.fhir2.api.translators.MedicationRequestTranslator;
import org.openmrs.module.fhir2.api.translators.MedicationTranslator;
import org.openmrs.module.fhir2.api.translators.PatientTranslator;
import org.openmrs.module.fhir2.api.translators.impl.ConceptTranslatorImpl;
import org.openmrs.module.fhir2.api.translators.impl.EncounterTranslatorImpl;
import org.openmrs.module.fhir2.api.translators.impl.ObservationTranslatorImpl;
import org.openmrs.module.fhir2.api.translators.impl.PractitionerTranslatorProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class FHIRResourceMapper {

    private final PatientTranslator patientTranslator;
    private final PractitionerTranslatorProviderImpl practitionerTranslatorProvider;
    private final MedicationRequestTranslator medicationRequestTranslator;
    private final MedicationTranslator medicationTranslator;
    private final EncounterTranslatorImpl encounterTranslator;
    private final ObservationTranslatorImpl observationTranslator;
    private final ConceptTranslatorImpl conceptTranslator;
    private final AbdmConfig abdmConfig;
    private final OmrsObsDocumentTransformer omrsObsDocumentTransformer;
    public static Set<String> conceptNames = new HashSet<>(Arrays.asList("Follow up Date", "Additional Advice on Discharge", "Discharge Summary, Plan for follow up"));

    @Autowired
    public FHIRResourceMapper(PatientTranslator patientTranslator, PractitionerTranslatorProviderImpl practitionerTranslatorProvider, MedicationRequestTranslator medicationRequestTranslator, MedicationTranslator medicationTranslator, EncounterTranslatorImpl encounterTranslator, ObservationTranslatorImpl observationTranslator, ConceptTranslatorImpl conceptTranslator, AbdmConfig abdmConfig, OmrsObsDocumentTransformer omrsObsDocumentTransformer) {
        this.patientTranslator = patientTranslator;
        this.practitionerTranslatorProvider = practitionerTranslatorProvider;
        this.medicationRequestTranslator = medicationRequestTranslator;
        this.medicationTranslator = medicationTranslator;
        this.encounterTranslator = encounterTranslator;
        this.observationTranslator = observationTranslator;
        this.conceptTranslator = conceptTranslator;
        this.abdmConfig = abdmConfig;
        this.omrsObsDocumentTransformer = omrsObsDocumentTransformer;
    }

    public Encounter mapToEncounter(org.openmrs.Encounter emrEncounter) {
        return encounterTranslator.toFhirResource(emrEncounter);
    }

    public DiagnosticReport mapToDiagnosticReport(Obs obs) {
        DiagnosticReport diagnosticReport = new DiagnosticReport();
        diagnosticReport.setId(obs.getUuid());
        diagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);
        Attachment attachment = null;

        if(obs.isObsGrouping()){

            Concept docTypeField = abdmConfig.getDocTemplateAtributeConcept(AbdmConfig.DocTemplateAttribute.DOC_TYPE);
            Concept attachmentConcept = abdmConfig.getDocTemplateAtributeConcept(AbdmConfig.DocTemplateAttribute.ATTACHMENT);
            Concept capturedDocType = omrsObsDocumentTransformer.getDocumentConcept(obs, AbdmConfig.HiTypeDocumentKind.DIAGNOSTIC_REPORT,docTypeField);

            attachment = obs.getGroupMembers().stream()
                    .filter(member -> member.getConcept().getUuid().equals(attachmentConcept.getUuid()) || member.getConcept().getName().getName().equals(Config.DOCUMENT_TYPE.getValue()))
                    .findFirst()
                    .map(o -> omrsObsDocumentTransformer.getAttachment(o, capturedDocType, AbdmConfig.HiTypeDocumentKind.DIAGNOSTIC_REPORT))
                    .get();
        }
        else
        {
            attachment = omrsObsDocumentTransformer.getAttachment(obs, null, AbdmConfig.HiTypeDocumentKind.DIAGNOSTIC_REPORT);
        }
        if(attachment != null){
            diagnosticReport.addPresentedForm(attachment);
            return diagnosticReport;
        }
        return new DiagnosticReport();
    }



    public Procedure mapToProcedure(Encounter encounter, Obs procedureObs, Map<AbdmConfig.ProcedureAttribute, Concept> procedureAttributeConceptMap) {
        Procedure procedure = new Procedure();
        procedure.setId(procedureObs.getUuid());
        procedure.setStatus(Procedure.ProcedureStatus.COMPLETED);
        Patient patient = mapToPatient(procedureObs.getEncounter().getPatient());
        procedure.setSubject(FHIRUtils.getReferenceToResource(patient));
        procedure.setEncounter(FHIRUtils.getReferenceToResource(encounter));

        if (procedureObs.isObsGrouping() && procedureObs.hasGroupMembers()) {
            AtomicReference<Date> procedureStartDate = new AtomicReference<>();
            AtomicReference<Date> procedureEndDate = new AtomicReference<>();
            procedureObs.getGroupMembers().forEach(member -> {
                Concept memberConcept = member.getConcept();

                if (conceptMatchesForAttribute(memberConcept, procedureAttributeConceptMap.get(AbdmConfig.ProcedureAttribute.PROCEDURE_NAME))) {
                    procedure.setCode(conceptTranslator.toFhirResource(member.getValueCoded()));
                }
                else if (conceptMatchesForAttribute(memberConcept, procedureAttributeConceptMap.get(AbdmConfig.ProcedureAttribute.PROCEDURE_NAME_NONCODED))) {
                    CodeableConcept concept = new CodeableConcept();
                    concept.setText(member.getValueCoded().getDisplayString());
                    procedure.setCode(concept);
                }

                if (conceptMatchesForAttribute(memberConcept, procedureAttributeConceptMap.get(AbdmConfig.ProcedureAttribute.PROCEDURE_START_DATETIME))) {
                    procedureStartDate.set(member.getValueDatetime());
                    procedure.setPerformed(new DateTimeType(procedureStartDate.get()));
                }

                if (conceptMatchesForAttribute(memberConcept, procedureAttributeConceptMap.get(AbdmConfig.ProcedureAttribute.PROCEDURE_BODYSITE))) {
                    procedure.setBodySite(Arrays.asList(conceptTranslator.toFhirResource(member.getValueCoded())));
                }
                else if (conceptMatchesForAttribute(memberConcept, procedureAttributeConceptMap.get(AbdmConfig.ProcedureAttribute.PROCEDURE_NONCODED_BODYSITE))) {
                    CodeableConcept concept = new CodeableConcept();
                    concept.setText(member.getValueText());
                    procedure.setBodySite(Arrays.asList(concept));
                }

                if (conceptMatchesForAttribute(memberConcept, procedureAttributeConceptMap.get(AbdmConfig.ProcedureAttribute.PROCEDURE_OUTCOME))) {
                    procedure.setOutcome(conceptTranslator.toFhirResource(member.getValueCoded()));
                }
                else if (conceptMatchesForAttribute(memberConcept, procedureAttributeConceptMap.get(AbdmConfig.ProcedureAttribute.PROCEDURE_NONCODED_OUTCOME))) {
                    CodeableConcept concept = new CodeableConcept();
                    concept.setText(member.getValueCoded().getDisplayString());
                    procedure.setOutcome(concept);
                }

                if (conceptMatchesForAttribute(memberConcept, procedureAttributeConceptMap.get(AbdmConfig.ProcedureAttribute.PROCEDURE_END_DATETIME))) {
                    procedureEndDate.set(member.getValueDatetime());
                }

                if (conceptMatchesForAttribute(memberConcept, procedureAttributeConceptMap.get(AbdmConfig.ProcedureAttribute.PROCEDURE_NOTE))) {
                   procedure.addNote(new Annotation(new MarkdownType(member.getValueText())));
                }
            });
            Date currentDate = new Date();
            if(procedureStartDate.get() != null) {
                if(currentDate.compareTo(procedureStartDate.get()) >= 0) {
                    if (procedureEndDate.get() == null || currentDate.compareTo(procedureEndDate.get()) <= 0)
                        procedure.setStatus(Procedure.ProcedureStatus.INPROGRESS);
                }
                else
                {
                    procedure.setStatus(Procedure.ProcedureStatus.PREPARATION);
                }
            }
            else
                procedure.setStatus(Procedure.ProcedureStatus.NOTDONE);
        }

        return procedure;
    }

    private boolean conceptMatchesForAttribute(Concept memberConcept, Concept mappedConcept) {
        return mappedConcept != null && memberConcept.getUuid().equals(mappedConcept.getUuid());
    }

    public CarePlan mapToCarePlan(Obs obs){
        List<Obs> groupMembers = new ArrayList<>();
        getGroupMembersOfObs(obs, groupMembers);
        CarePlan carePlan = new CarePlan();
        carePlan.setId(obs.getUuid());
        String description = "";
        for(Obs o : groupMembers){
            if(o.getValueDatetime() != null) {
                description += description != "" ? ", " : "";
                description += o.getValueDatetime();
            } else if(o.getValueText() != null && Objects.equals(o.getConcept().getName().getName(), "Additional Advice on Discharge")){
                description += description != "" ? ", " : "";
                description +=  o.getValueText();
            }
            if(o.getValueText() != null && Objects.equals(o.getConcept().getName().getName(), "Discharge Summary, Plan for follow up")){
                carePlan.setTitle(o.getValueText());
                carePlan.setUserData("Discharge Summary, Plan for follow up", o.getValueText());
            }
        }
        if(!description.isEmpty()){
            carePlan.setDescription(description);
        }
        return carePlan;
    }

    private void getGroupMembersOfObs(Obs obs, List<Obs> groupMembers) {
        if (obs.getGroupMembers().size() > 0) {
            for (Obs groupMember : obs.getGroupMembers()) {
                if (conceptNames.contains(groupMember.getConcept().getDisplayString())){
                    groupMembers.add(groupMember);
                }
            }
        }
    }

    public DocumentReference mapToDocumentDocumentReference(Obs obs, AbdmConfig.HiTypeDocumentKind typeDocuments) {
        DocumentReference documentReference = new DocumentReference();
        documentReference.setId(obs.getUuid());
        documentReference.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
        documentReference.setDescription(obs.getComment());
        List<DocumentReference.DocumentReferenceContentComponent> contents = new ArrayList<>();
        try {
            List<Attachment> attachments = getAttachments(obs, typeDocuments);
            for (Attachment attachment : attachments) {
                DocumentReference.DocumentReferenceContentComponent documentReferenceContentComponent
                        = new DocumentReference.DocumentReferenceContentComponent();
                documentReferenceContentComponent.setAttachment(attachment);
                contents.add(documentReferenceContentComponent);
            }
            documentReference.setContent(contents);
            return documentReference;
        } catch (IOException exception) {
            return documentReference;
        }
    }

    private List<Attachment> getAttachments(Obs obs, AbdmConfig.HiTypeDocumentKind typeDocuments) throws IOException {
        List<Attachment> attachments = new ArrayList<>();
        Attachment attachment = new Attachment();
        StringBuilder valueText = new StringBuilder();
        Set<Obs> obsList = obs.getGroupMembers();
        StringBuilder contentType = new StringBuilder();
        Concept obsConcept = null;
        for(Obs obs1 : obsList) {
            if(obs.getConcept().equals(abdmConfig.getDocTemplateAtributeConcept(AbdmConfig.DocTemplateAttribute.TEMPLATE)))
            {
                obsConcept = obs1.getValueCoded();
            }
            if (obs1.getConcept().getName().getName().equals(Config.DOCUMENT_TYPE.getValue()) || obs1.getConcept().equals(abdmConfig.getDocTemplateAtributeConcept(AbdmConfig.DocTemplateAttribute.ATTACHMENT))) {
                valueText.append(obs1.getValueText() != null ? obs1.getValueText() : obs1.getValueComplex());
                contentType.append(FHIRUtils.getTypeOfTheObsDocument(obs1.getValueText()));
            }
        }
        if(obs.getConcept().getName().getName().equals(Config.IMAGE.getValue()) || obs.getConcept().getName().getName().equals(Config.PATIENT_VIDEO.getValue())){
            valueText.append(obs.getValueComplex());
            contentType.append(FHIRUtils.getTypeOfTheObsDocument(obs.getValueComplex()));
        }
        attachment.setContentType(contentType.toString());
        byte[] fileContent = Files.readAllBytes(new File(Config.PATIENT_DOCUMENTS_PATH.getValue() + valueText).toPath());
        attachment.setData(fileContent);
        StringBuilder title = new StringBuilder();
        if(typeDocuments == AbdmConfig.HiTypeDocumentKind.OP_CONSULT)
            title.append("OP Consultation - ");
        else if(typeDocuments == AbdmConfig.HiTypeDocumentKind.DISCHARGE_SUMMARY)
            title.append("Discharge Summary - ");
        else if(typeDocuments == AbdmConfig.HiTypeDocumentKind.DIAGNOSTIC_REPORT)
            title.append("Diagnostic Report - ");
        else if(typeDocuments == AbdmConfig.HiTypeDocumentKind.WELLNESS_RECORD)
            title.append("Wellness Record - ");
        else
            title.append("Document - ");
        if(obsConcept == null)
            title.append(obs.getConcept().getName().getName());
        else
            title.append(obsConcept.getDisplayString());
        attachment.setTitle(title.toString());
        attachments.add(attachment);
        return attachments;
    }

    public Condition mapToCondition(OpenMrsCondition openMrsCondition, Patient patient) {
        Condition condition = new Condition();
        CodeableConcept concept = new CodeableConcept();
        concept.setText(openMrsCondition.getName());
        condition.setCode(concept);
        condition.setSubject(new Reference("Patient/" + patient.getId()));
        condition.setId(openMrsCondition.getUuid());
        condition.setRecordedDate(openMrsCondition.getRecordedDate());
        condition.setClinicalStatus(new CodeableConcept(new Coding().setCode("active").setSystem(Constants.FHIR_CONDITION_CLINICAL_STATUS_SYSTEM)));
        return condition;
    }

    public Observation mapToObs(Obs obs) {
        Concept concept = initializeEntityAndUnproxy(obs.getConcept());
        obs.setConcept(concept);
        Observation observation = observationTranslator.toFhirResource(obs);
        observation.addNote(new Annotation(new MarkdownType(obs.getComment())));
        return observation;
    }

    public ServiceRequest mapToOrder(Order order){
        ServiceRequest serviceRequest = new ServiceRequest();
        CodeableConcept concept = new CodeableConcept();
        concept.setText(order.getConcept().getDisplayString());
        serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
        serviceRequest.setSubject(new Reference("Patient/"+ order.getPatient().getUuid()));
        serviceRequest.setCode(concept);
        serviceRequest.setId(order.getUuid());
        return serviceRequest;
    }

    public Patient mapToPatient(org.openmrs.Patient emrPatient) {
        Patient patient = patientTranslator.toFhirResource(emrPatient);
        patient.getName().get(0).setText(emrPatient.getPerson().getPersonName().getFullName());
        return patient;
    }

    public Practitioner mapToPractitioner(EncounterProvider encounterProvider) {
        Practitioner practitioner = practitionerTranslatorProvider.toFhirResource(encounterProvider.getProvider());
        practitioner.getName().get(0).setText(encounterProvider.getProvider().getName());
        return practitioner;
    }

    private String displayName(Object object) {
        if (object == null)
            return "";
        return object.toString() + " ";

    }

    public MedicationRequest mapToMedicationRequest(DrugOrder order) {
        String dosingInstrutions = displayName(order.getDose()) +
                displayName(order.getDoseUnits() == null ? "" : order.getDoseUnits().getName()) +
                displayName(order.getFrequency()) +
                displayName(order.getRoute() == null ? "" : order.getRoute().getName()) +
                displayName(order.getDuration()) +
                displayName(order.getDurationUnits() == null ? "" : order.getDurationUnits().getName());
        MedicationRequest medicationRequest = medicationRequestTranslator.toFhirResource(order);
        medicationRequest.setSubject(new Reference("Patient/"+ order.getPatient().getUuid()));
        Dosage dosage = medicationRequest.getDosageInstruction().get(0);
        dosage.setText(dosingInstrutions.trim());
        return medicationRequest;
    }

    public Medication mapToMedication(DrugOrder order) {
        if (order.getDrug() == null) {
            return null;
        }
        Medication medication = medicationTranslator.toFhirResource(order.getDrug());
        medication.getCode().setText(order.getDrug().getName());
        return medication;
    }

    public static <T> T initializeEntityAndUnproxy(T entity) {
        if (entity == null) {
            throw new NullPointerException("Entity passed for initialization is null");
        }
        Hibernate.initialize(entity);
        if (entity instanceof HibernateProxy) {
            entity = (T) ((HibernateProxy) entity).getHibernateLazyInitializer().getImplementation();
        }
        return entity;
    }
}
