package org.bahmni.module.hip.web.service;

import org.bahmni.module.bahmnicore.dao.OrderDao;
import org.bahmni.module.hip.api.dao.EncounterDao;
import org.bahmni.module.hip.web.model.*;
import org.openmrs.*;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.module.bahmniemrapi.laborder.contract.LabOrderResult;
import org.openmrs.module.bahmniemrapi.laborder.contract.LabOrderResults;
import org.openmrs.module.bahmniemrapi.laborder.service.LabOrderResultsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DiagnosticReportService {
    private final FhirBundledDiagnosticReportBuilder fhirBundledDiagnosticReportBuilder;
    private final PatientService patientService;
    private final EncounterService encounterService;
    private final EncounterDao encounterDao;
    private final org.openmrs.api.OrderService orderService;
    private OrderDao orderDao;


    private LabOrderResultsService labOrderResultsService;

    @Autowired
    public DiagnosticReportService(FhirBundledDiagnosticReportBuilder fhirBundledDiagnosticReportBuilder,
                                   PatientService patientService,
                                   EncounterService encounterService,
                                   EncounterDao encounterDao, OrderService orderService,
                                   LabOrderResultsService labOrderResultsService,
                                    OrderDao orderDao
    //                               VisitService visitService
    ) {
        this.fhirBundledDiagnosticReportBuilder = fhirBundledDiagnosticReportBuilder;
        this.patientService = patientService;
        this.encounterService = encounterService;
        this.encounterDao = encounterDao;
        this.orderService = orderService;
        this.labOrderResultsService = labOrderResultsService;
        this.orderDao = orderDao;
        //this.visitService = visitService;
    }

    public List<DiagnosticReportBundle> getDiagnosticReportsForVisit(String patientUuid, DateRange dateRange, String visitType) {
        Date fromDate = dateRange.getFrom();
        Date toDate = dateRange.getTo();
        Patient patient = patientService.getPatientByUuid(patientUuid);
        HashMap<Encounter, List<Obs>> encounterListMap = getAllObservationsForVisits(fromDate, toDate, patient, visitType);
        List<OpenMrsDiagnosticReport> openMrsDiagnosticReports = OpenMrsDiagnosticReport.fromDiagnosticReport(encounterListMap);

        return openMrsDiagnosticReports
                .stream()
                .map(fhirBundledDiagnosticReportBuilder::fhirBundleResponseFor)
                .collect(Collectors.toList());
    }

    private HashMap<Encounter, List<Obs>> getAllObservationsForVisits(Date fromDate, Date toDate,
                                                                      Patient patient,
                                                                      String visitType) {
        HashMap<Encounter, List<Obs>> encounterListMap = new HashMap<>();
        List<Integer> encounterIds = encounterDao.GetEncounterIdsForVisitForDiagnosticReport(patient.getUuid(), visitType, fromDate, toDate);
        List<Encounter> finalList = new ArrayList<>();
        for(Integer encounterId : encounterIds){
            finalList.add(encounterService.getEncounter(encounterId));
        }
        for (Encounter e : finalList) {
            encounterListMap.put(e, new ArrayList<>(e.getAllObs()));
        }
        return encounterListMap;
    }

    public List<DiagnosticReportBundle> getDiagnosticReportsForProgram(String patientUuid, DateRange dateRange, String programName, String programEnrollmentId) {
        Date fromDate = dateRange.getFrom();
        Date toDate = dateRange.getTo();
        Patient patient = patientService.getPatientByUuid(patientUuid);
        HashMap<Encounter, List<Obs>> encounterListMap = getAllObservationsForPrograms(fromDate, toDate, patient, programName, programEnrollmentId);
        List<OpenMrsDiagnosticReport> openMrsDiagnosticReports = OpenMrsDiagnosticReport.fromDiagnosticReport(encounterListMap);

        return openMrsDiagnosticReports
                .stream()
                .map(fhirBundledDiagnosticReportBuilder::fhirBundleResponseFor)
                .collect(Collectors.toList());

    }

    private HashMap<Encounter, List<Obs>> getAllObservationsForPrograms(Date fromDate, Date toDate,
                                                                        Patient patient,
                                                                        String programName,
                                                                        String programEnrollmentId) {
        HashMap<Encounter, List<Obs>> encounterListMap = new HashMap<>();
        List<Integer> encounterIds = encounterDao.GetEncounterIdsForProgramForDiagnosticReport(patient.getUuid(), programName,
                programEnrollmentId, fromDate, toDate);
        List<Encounter> finalList = new ArrayList<>();
        for(Integer encounterId : encounterIds){
            finalList.add(encounterService.getEncounter(encounterId));
        }
        for (Encounter e : finalList) {
            encounterListMap.put(e, new ArrayList<>(e.getAllObs()));
        }
        return encounterListMap;
    }


    public List<DiagnosticReportBundle> getLabResultsForVisit(String patientUuid, DateRange dateRange, String visittype) {
        Patient patient = patientService.getPatientByUuid(patientUuid);

        List<Visit> visits, visitsWithOrders ;

        List<Integer> visitsForVisitType =  encounterDao.GetEncounterIdsForVisitForLabResults(patientUuid, visittype, dateRange.getFrom(), dateRange.getTo() );
        visits = orderDao.getVisitsWithAllOrders(patient, "Order", null, null );

        List<Order> orders = orderDao.getAllOrdersForVisits(new OrderType(3), visits);


        visitsWithOrders = visits.stream().filter(visit -> {
            return visitsForVisitType.contains(visit.getVisitId());
        }).collect(Collectors.toList());

        LabOrderResults results = labOrderResultsService.getAll(patient, visits, Integer.MAX_VALUE);
        Map<String, List<LabOrderResult>> groupedByOrderUUID = results.getResults().stream().collect(Collectors.groupingBy(LabOrderResult::getOrderUuid));


        List<OpenMrsLabResults> labResults = groupedByOrderUUID.entrySet().stream().map(entry -> {
                    Order orderForUuid = orders
                            .stream()
                            .filter(order -> order.getUuid().equals(entry.getKey()))
                            .findFirst()
                            .get();
                    return new OpenMrsLabResults(orderForUuid.getEncounter(), orderForUuid.getPatient(), entry.getValue());
                } ).collect(Collectors.toList());

        List<FhirDiagnosticReport> reports = FhirDiagnosticReport.fromLabResults( labResults );

        List<DiagnosticReportBundle> bundles = reports.stream().map(fhirBundledDiagnosticReportBuilder::fhirBundleResponseFor).collect(Collectors.toList());
        return bundles;
    }
}
