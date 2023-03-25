package org.bahmni.module.hip.web.service;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bahmni.module.hip.web.model.DateRange;
import org.bahmni.module.hip.web.model.DrugOrders;
import org.bahmni.module.hip.web.model.OpenMrsPrescription;
import org.bahmni.module.hip.web.model.PrescriptionBundle;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Visit;
import org.openmrs.api.VisitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static org.bahmni.module.hip.web.utils.DateUtils.isDateBetweenDateRange;

@Service
public class PrescriptionService {
    private static final Logger logger = LogManager.getLogger(PrescriptionService.class);

    private final OpenMRSDrugOrderClient openMRSDrugOrderClient;
    private final FhirBundledPrescriptionBuilder fhirBundledPrescriptionBuilder;
    private final VisitService visitService;
    private final OmrsObsDocumentTransformer documentTransformer;
    private final AbdmConfig abdmConfig;

    @Autowired
    public PrescriptionService(OpenMRSDrugOrderClient openMRSDrugOrderClient,
                               FhirBundledPrescriptionBuilder fhirBundledPrescriptionBuilder,
                               VisitService visitService,
                               OmrsObsDocumentTransformer documentTransformer,
                               AbdmConfig abdmConfig) {
        this.openMRSDrugOrderClient = openMRSDrugOrderClient;
        this.fhirBundledPrescriptionBuilder = fhirBundledPrescriptionBuilder;
        this.visitService = visitService;
        this.documentTransformer = documentTransformer;
        this.abdmConfig = abdmConfig;
    }


    public List<PrescriptionBundle> getPrescriptions(String patientUuid,String visitUuid, String fromDate, String ToDate) throws ParseException {
        Visit visit = visitService.getVisitByUuid(visitUuid);
        if (isDateBetweenDateRange(visit.getStartDatetime(), fromDate, ToDate)) {
            DrugOrders drugOrders = new DrugOrders(openMRSDrugOrderClient.getDrugOrdersByDateAndVisitTypeFor(visit));
            Concept docType = abdmConfig.getDocumentConcept(AbdmConfig.DocumentKind.PRESCIPTION);
            Map<Encounter, List<Obs>> encounterDocObs = visit.getEncounters()
                    .stream()
                    .map(e -> e.getObsAtTopLevel(false))
                    .flatMap(Collection::stream)
                    .filter(obs -> isPrescriptionDoc(obs, docType))
                    .collect(Collectors.groupingBy(Obs::getEncounter));
            if (drugOrders.isEmpty() && encounterDocObs.isEmpty()) {
                return new ArrayList<>();
            }
            List<OpenMrsPrescription> openMrsPrescriptions = OpenMrsPrescription
                    .from(drugOrders.groupByEncounter(), encounterDocObs);
            return openMrsPrescriptions
                    .stream()
                    .map(fhirBundledPrescriptionBuilder::fhirBundleResponseFor)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private boolean isPrescriptionDoc(Obs obs, Concept docType) {
        if (documentTransformer.isSupportedDocument(obs, AbdmConfig.DocumentKind.PRESCIPTION)) {
            return true;
        }
        if (documentTransformer.isSupportedDocument(obs, AbdmConfig.DocumentKind.TEMPLATE)) {
            Concept docTypeField = abdmConfig.getDocTemplateAtributeConcept(AbdmConfig.DocTemplateAttribute.DOC_TYPE);
            if (docTypeField == null) return false;
            Optional<Concept> identifiedMember = obs.getGroupMembers().stream()
                    .filter(member -> member.getConcept().getUuid().equals(docTypeField.getUuid()))
                    .map(Obs::getValueCoded).findFirst();
            return identifiedMember.map(concept -> concept.getUuid().equals(docType.getUuid())).orElse(false);
        }
        return false;
    }

    public List<PrescriptionBundle> getPrescriptionsForProgram(String patientIdUuid, DateRange dateRange, String programName, String programEnrolmentId) {
        DrugOrders drugOrders = new DrugOrders(openMRSDrugOrderClient.getDrugOrdersByDateAndProgramFor(patientIdUuid, dateRange, programName, programEnrolmentId));
        if (drugOrders.isEmpty()) {
            //TODO: Need to identify if there are unstructured docs captured for prescription as part of program
            return new ArrayList<>();
        }
        List<OpenMrsPrescription> openMrsPrescriptions = OpenMrsPrescription.from(drugOrders.groupByEncounter(), new HashMap<>());
        return openMrsPrescriptions
                .stream()
                .map(fhirBundledPrescriptionBuilder::fhirBundleResponseFor)
                .collect(Collectors.toList());
    }
}
