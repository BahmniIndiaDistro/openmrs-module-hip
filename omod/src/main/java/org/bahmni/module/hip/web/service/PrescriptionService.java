package org.bahmni.module.hip.web.service;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bahmni.module.hip.web.model.DateRange;
import org.bahmni.module.hip.web.model.DrugOrders;
import org.bahmni.module.hip.web.model.OpenMrsPrescription;
import org.bahmni.module.hip.web.model.PrescriptionBundle;
import org.openmrs.Visit;
import org.openmrs.api.VisitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.bahmni.module.hip.web.utils.DateUtils.isDateBetweenDateRange;

@Service
public class PrescriptionService {
    private static final Logger logger = LogManager.getLogger(PrescriptionService.class);

    private final OpenMRSDrugOrderClient openMRSDrugOrderClient;
    private final FhirBundledPrescriptionBuilder fhirBundledPrescriptionBuilder;
    private final VisitService visitService;
    private final AbdmConfig abdmConfig;

    @Autowired
    public PrescriptionService(OpenMRSDrugOrderClient openMRSDrugOrderClient,
                               FhirBundledPrescriptionBuilder fhirBundledPrescriptionBuilder,
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
                    .map(omrsPrescription -> fhirBundledPrescriptionBuilder.fhirBundleResponseFor(omrsPrescription, abdmConfig.getPrescriptionDocumentConcept()))
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
                .map(omrsPrescription -> fhirBundledPrescriptionBuilder.fhirBundleResponseFor(omrsPrescription, abdmConfig.getPrescriptionDocumentConcept()))
                .collect(Collectors.toList());
    }

}
