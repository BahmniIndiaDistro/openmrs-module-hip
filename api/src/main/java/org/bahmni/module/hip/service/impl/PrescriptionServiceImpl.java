package org.bahmni.module.hip.service.impl;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bahmni.module.hip.builder.FhirBundledPrescriptionBuilder;
import org.bahmni.module.hip.config.AbdmConfig;
import org.bahmni.module.hip.model.DateRange;
import org.bahmni.module.hip.model.DrugOrders;
import org.bahmni.module.hip.model.OpenMrsPrescription;
import org.bahmni.module.hip.model.PrescriptionBundle;
import org.bahmni.module.hip.service.ConsultationService;
import org.bahmni.module.hip.service.OpenMRSDrugOrderClient;
import org.bahmni.module.hip.service.PrescriptionService;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Visit;
import org.openmrs.api.VisitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PrescriptionServiceImpl implements PrescriptionService {
    private static final Logger logger = LogManager.getLogger(PrescriptionService.class);

    private final OpenMRSDrugOrderClient openMRSDrugOrderClient;
    private final FhirBundledPrescriptionBuilder fhirBundledPrescriptionBuilder;
    private final VisitService visitService;
    private final ConsultationService consultationService;

    @Autowired
    public PrescriptionServiceImpl(OpenMRSDrugOrderClient openMRSDrugOrderClient,
                                   FhirBundledPrescriptionBuilder fhirBundledPrescriptionBuilder,
                                   VisitService visitService,
                                   ConsultationService consultationService) {
        this.openMRSDrugOrderClient = openMRSDrugOrderClient;
        this.fhirBundledPrescriptionBuilder = fhirBundledPrescriptionBuilder;
        this.visitService = visitService;
        this.consultationService = consultationService;
    }


    @Override
    public List<PrescriptionBundle> getPrescriptions(String patientUuid, String visitUuid, Date fromDate, Date toDate) {
        Visit visit = visitService.getVisitByUuid(visitUuid);
        Map<Encounter, DrugOrders> drugOrders = openMRSDrugOrderClient.getDrugOrdersByDateAndVisitTypeFor(visit, fromDate, toDate);
        Map<Encounter, List<Obs>> encounterDocObs = consultationService.getEncounterPatientDocumentsMap(visit, fromDate, toDate, AbdmConfig.HiTypeDocumentKind.PRESCRIPTION);
        if (drugOrders.isEmpty() && encounterDocObs.isEmpty()) {
            return new ArrayList<>();
        }
        List<OpenMrsPrescription> openMrsPrescriptions = OpenMrsPrescription
                .from(drugOrders, encounterDocObs);
        return openMrsPrescriptions
                .stream()
                .map(fhirBundledPrescriptionBuilder::fhirBundleResponseFor)
                .collect(Collectors.toList());
    }

    @Override
    public List<PrescriptionBundle> getPrescriptionsForProgram(String patientIdUuid, DateRange dateRange, String programName, String programEnrolmentId) {
        Map<Encounter, DrugOrders> drugOrders = openMRSDrugOrderClient.getDrugOrdersByDateAndProgramFor(patientIdUuid, dateRange, programName, programEnrolmentId);
        if (drugOrders.isEmpty()) {
            //TODO: Need to identify if there are unstructured docs captured for prescription as part of program
            return new ArrayList<>();
        }
        List<OpenMrsPrescription> openMrsPrescriptions = OpenMrsPrescription.from(drugOrders, new HashMap<>());
        return openMrsPrescriptions
                .stream()
                .map(fhirBundledPrescriptionBuilder::fhirBundleResponseFor)
                .collect(Collectors.toList());
    }
}
