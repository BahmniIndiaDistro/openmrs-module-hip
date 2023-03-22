package org.bahmni.module.hip.web.service;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bahmni.module.hip.api.dao.HipVisitDao;
import org.bahmni.module.hip.web.model.PrescriptionBundle;
import org.bahmni.module.hip.web.model.DateRange;
import org.bahmni.module.hip.web.model.DrugOrders;
import org.bahmni.module.hip.web.model.OpenMrsPrescription;
import org.openmrs.Concept;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.bahmni.module.hip.web.utils.DateUtils.isDateBetweenDateRange;

@Service
public class PrescriptionService {
    private static Logger logger = LogManager.getLogger(PrescriptionService.class);

    private final OpenMRSDrugOrderClient openMRSDrugOrderClient;
    private final FhirBundledPrescriptionBuilder fhirBundledPrescriptionBuilder;
    private final VisitService visitService;
    private AbdmConfig abdmConfig;

    @Autowired
    public PrescriptionService(OpenMRSDrugOrderClient openMRSDrugOrderClient,
                               FhirBundledPrescriptionBuilder fhirBundledPrescriptionBuilder,
                               PatientService patientService,
                               HipVisitDao hipVisitDao,
                               VisitService visitService,
                               AbdmConfig abdmConfig) {
        this.openMRSDrugOrderClient = openMRSDrugOrderClient;
        this.fhirBundledPrescriptionBuilder = fhirBundledPrescriptionBuilder;
        this.visitService = visitService;
        this.abdmConfig = abdmConfig;
    }


    public List<PrescriptionBundle> getPrescriptions(String patientUuid,String visitUuid, String fromDate, String ToDate) throws ParseException {
        Visit visit = visitService.getVisitByUuid(visitUuid);
        if (isDateBetweenDateRange(visit.getStartDatetime(), fromDate, ToDate)) {
            DrugOrders drugOrders = new DrugOrders(openMRSDrugOrderClient.getDrugOrdersByDateAndVisitTypeFor(visit));

            if (drugOrders.isEmpty())
                return new ArrayList<>();

            List<OpenMrsPrescription> openMrsPrescriptions = OpenMrsPrescription
                    .from(drugOrders.groupByEncounter());

            return openMrsPrescriptions
                    .stream()
                    .map(omrsPrescription -> fhirBundledPrescriptionBuilder.fhirBundleResponseFor(omrsPrescription, identifyPrescriptionFileConcept()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public List<PrescriptionBundle> getPrescriptionsForProgram(String patientIdUuid, DateRange dateRange, String programName, String programEnrolmentId) {
        DrugOrders drugOrders = new DrugOrders(openMRSDrugOrderClient.getDrugOrdersByDateAndProgramFor(patientIdUuid, dateRange, programName, programEnrolmentId));

        if (drugOrders.isEmpty())
            return new ArrayList<>();

        List<OpenMrsPrescription> openMrsPrescriptions = OpenMrsPrescription
                .from(drugOrders.groupByEncounter());

        return openMrsPrescriptions
                .stream()
                .map(omrsPrescription -> fhirBundledPrescriptionBuilder.fhirBundleResponseFor(omrsPrescription, identifyPrescriptionFileConcept()))
                .collect(Collectors.toList());
    }

    private Concept identifyPrescriptionFileConcept() {
        String prescriptionDocumentConceptId = Context.getAdministrationService().getGlobalProperty(abdmConfig.getPrescriptionDocumentConceptMap());
        if (!StringUtils.isEmpty(prescriptionDocumentConceptId)) {
            return Context.getConceptService().getConceptByUuid(prescriptionDocumentConceptId);
        }
        logger.info("Property abdm.conceptMap.prescription.document is not set. System would not be able to send unstructured prescription document");
        return null;
    }
}
