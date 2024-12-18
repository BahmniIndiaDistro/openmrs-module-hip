package org.bahmni.module.hip.service;

import org.bahmni.module.bahmnicore.dao.OrderDao;
import org.bahmni.module.hip.Config;
import org.bahmni.module.hip.api.dao.DiagnosticReportDao;
import org.bahmni.module.hip.api.dao.EncounterDao;
import org.bahmni.module.hip.api.dao.HipVisitDao;
import org.bahmni.module.hip.model.DateRange;
import org.bahmni.module.hip.model.DiagnosticReportBundle;
import org.bahmni.module.hip.model.OpenMrsDiagnosticReport;
import org.bahmni.module.hip.model.OpenMrsLabResults;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.openmrs.module.bahmniemrapi.laborder.contract.LabOrderResult;
import org.openmrs.module.bahmniemrapi.laborder.contract.LabOrderResults;
import org.openmrs.module.bahmniemrapi.laborder.service.LabOrderResultsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.bahmni.module.hip.utils.DateUtils.isDateBetweenDateRange;

@Service
public class DiagnosticReportService {
    private final FhirBundledDiagnosticReportBuilder fhirBundledDiagnosticReportBuilder;
    private final PatientService patientService;
    private final EncounterService encounterService;
    private final EncounterDao encounterDao;
    private final VisitService visitService;
    private final HipVisitDao hipVisitDao;
    private final OrderDao orderDao;
    private final DiagnosticReportDao diagnosticReportDao;
    private final ConsultationService consultationService;


    private LabOrderResultsService labOrderResultsService;


    @Autowired
    public DiagnosticReportService(FhirBundledDiagnosticReportBuilder fhirBundledDiagnosticReportBuilder,
                                   PatientService patientService,
                                   EncounterService encounterService,
                                   LabOrderResultsService labOrderResultsService,
                                   EncounterDao encounterDao,
                                   VisitService visitService, HipVisitDao hipVisitDao,
                                   OrderDao orderDao,
                                   DiagnosticReportDao diagnosticReportDao, ConsultationService consultationService) {
        this.fhirBundledDiagnosticReportBuilder = fhirBundledDiagnosticReportBuilder;
        this.patientService = patientService;
        this.encounterService = encounterService;
        this.encounterDao = encounterDao;
        this.visitService = visitService;
        this.hipVisitDao = hipVisitDao;
        this.labOrderResultsService = labOrderResultsService;
        this.orderDao = orderDao;
        this.diagnosticReportDao = diagnosticReportDao;
        this.consultationService = consultationService;
    }

    public List<DiagnosticReportBundle> getDiagnosticReportsForVisit(String patientUuid, String visitUuid, Date fromDate, Date toDate) throws ParseException {
        Visit visit = visitService.getVisitByUuid(visitUuid);

        Map<Encounter, List<Obs>> encounterListMap = consultationService.getEncounterPatientDocumentsMap(visit,fromDate,toDate, AbdmConfig.HiTypeDocumentKind.DIAGNOSTIC_REPORT);
        List<OpenMrsDiagnosticReport> openMrsDiagnosticReports = OpenMrsDiagnosticReport.fromDiagnosticReport(encounterListMap);

        return openMrsDiagnosticReports
                .stream()
                .map(fhirBundledDiagnosticReportBuilder::fhirBundleResponseFor)
                .collect(Collectors.toList());

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

    public HashMap<Encounter, List<Obs>> getAllObservationsForPrograms(Date fromDate, Date toDate,
                                                                        Patient patient,
                                                                        String programName,
                                                                        String programEnrollmentId) {
        HashMap<Encounter, List<Obs>> encounterListMap = new HashMap<>();
        List<Integer> encounterIds = encounterDao.getEncounterIdsForProgramForDiagnosticReport(patient.getUuid(), programName,
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


    private List<DiagnosticReportBundle> getOrderedLabResults(Patient patient, List<Visit> visits){

        List<Order> orders = orderDao.getAllOrdersForVisits(new OrderType(Integer.parseInt(Config.LAB_ORDER_TYPE_ID.getValue())), visits);
        Map<Encounter, List<Order>> groupedByEncounter= orders.stream().collect(Collectors.groupingBy(Order::getEncounter));

        List<Visit> visitsWithAllOrders = orderDao.getVisitsWithAllOrders(patient, Config.ORDER_TYPE.getValue(), true, null );

        LabOrderResults results = labOrderResultsService.getAll(patient, visitsWithAllOrders, Integer.MAX_VALUE);
        Map<String, List<LabOrderResult>> groupedByOrderUUID = results.getResults().stream().collect(Collectors.groupingBy(LabOrderResult::getOrderUuid));

        Map<Order,List<Obs>> groupObs = diagnosticReportDao.getAllObsForDiagnosticReports(patient.getUuid(), true).stream().collect(Collectors.groupingBy(Obs::getOrder));

        List<OpenMrsLabResults> labResults = groupedByOrderUUID.entrySet().stream().map(entry -> {
            Optional<Order> orderForUuid = orders
                    .stream()
                    .filter(order -> order.getUuid().equals(entry.getKey()))
                    .findFirst();
            if (orderForUuid.isPresent()) {
                Map<LabOrderResult, Obs> labresult = new HashMap<>();
                groupedByEncounter.get(orderForUuid.get().getEncounter()).stream().forEach(entry1 -> {
                    if(groupedByOrderUUID.containsKey(entry1.getUuid())){
                        putFulFilledOrders(entry1.getUuid(),groupedByOrderUUID,groupObs,labresult);
                        orders.remove(entry1);
                    }
                });
                if(!labresult.isEmpty())
                    return new OpenMrsLabResults(orderForUuid.get().getEncounter(), orderForUuid.get().getPatient(),
                        labresult);
            }
            return null;
        } ).filter(Objects::nonNull).collect(Collectors.toList());


        List<DiagnosticReportBundle> bundles = labResults.stream().map(fhirBundledDiagnosticReportBuilder::fhirBundleResponseFor).collect(Collectors.toList());
        return bundles;
    }

    private void putFulFilledOrders(String orderUuid, Map<String, List<LabOrderResult>> groupedByOrderUUID, Map<Order,List<Obs>> groupObs, Map<LabOrderResult, Obs> labresult){
        List<LabOrderResult> labOrderResults = groupedByOrderUUID.get(orderUuid);
        Order order = orderDao.getOrderByUuid(orderUuid);
        Obs labObservation = groupObs.containsKey(order) ? groupObs.get(order).get(0) : null;
        for (LabOrderResult labOrderResult: labOrderResults) {
            if(labOrderResult.getResult() != null){
                labresult.put(labOrderResult, labObservation);
            }
            else{
                if(labObservation != null)
                    labresult.put(labOrderResult,labObservation);
            }
        }
    }

    private List<DiagnosticReportBundle> getUnordersLabResults(Patient patient, List<Visit> visitList) {

        Map<Encounter,List<Obs>> unorderedUploads = diagnosticReportDao.getAllUnorderedUploadsForVisit(patient.getUuid(), visitList.size() != 0 ? visitList.get(0) : null);

        List<OpenMrsLabResults> labResults = new ArrayList<>();
        List<Obs> labRecords;

        for (Map.Entry<Encounter, List<Obs>> map : unorderedUploads.entrySet()) {
            labRecords =  new ArrayList<>();
            for (Obs obs: unorderedUploads.get(map.getKey())) {
                labRecords.add(obs);
            }
            labResults.add(new OpenMrsLabResults(map.getKey(),map.getKey().getPatient(),labRecords));
        }
        List<DiagnosticReportBundle> bundles = labResults.stream().map(fhirBundledDiagnosticReportBuilder::fhirBundleResponseFor).collect(Collectors.toList());
        return bundles;
    }

    public List<DiagnosticReportBundle> getLabResultsForVisits(String patientUuid, String visitUuid, String fromDate, String ToDate) {
        Patient patient = patientService.getPatientByUuid(patientUuid);
        Visit visit = visitService.getVisitByUuid(visitUuid);
        try {
            if (isDateBetweenDateRange(visit.getStartDatetime(), fromDate, ToDate)) {
                List<Visit> visits = new ArrayList<>();
                visits.add(visit);
                List<DiagnosticReportBundle> list = getUnordersLabResults(patient, visits);
                list.addAll(getOrderedLabResults(patient,visits));
                return list;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

        public List<DiagnosticReportBundle> getLabResultsForPrograms(String patientUuid, DateRange dateRange, String programName, String programEnrollmentId)
    {
        List<Integer> visitsForProgram =  hipVisitDao.getVisitIdsForProgramForLabResults(patientUuid, programName, programEnrollmentId, dateRange.getFrom(), dateRange.getTo() );
        Patient patient = patientService.getPatientByUuid(patientUuid);

        List<Visit> visits, visitsWithOrdersForProgram ;

        visits = orderDao.getVisitsWithAllOrders(patient, Config.ORDER_TYPE.getValue(), null, null );
        visitsWithOrdersForProgram = visits.stream().filter( visit -> visitsForProgram.contains(visit.getVisitId()) ).collect(Collectors.toList());
        List<DiagnosticReportBundle> list = getUnordersLabResults(patient, visits);
        list.addAll(getOrderedLabResults(patient,visits));
        return list;
    }

}
