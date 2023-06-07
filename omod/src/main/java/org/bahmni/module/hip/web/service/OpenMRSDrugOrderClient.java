package org.bahmni.module.hip.web.service;

import org.bahmni.module.hip.api.dao.PrescriptionOrderDao;
import org.bahmni.module.hip.web.model.DateRange;
import org.bahmni.module.hip.web.model.DrugOrders;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
class OpenMRSDrugOrderClient {

    private final PatientService patientService;
    private final OrderService orderService;
    private final PrescriptionOrderDao prescriptionOrderDao;

    @Autowired
    public OpenMRSDrugOrderClient(PatientService patientService, OrderService orderService, PrescriptionOrderDao prescriptionOrderDao) {
        this.patientService = patientService;
        this.orderService = orderService;
        this.prescriptionOrderDao = prescriptionOrderDao;
    }

    List<DrugOrder> drugOrdersFor(String patientUUID, String visitType) {

        Patient patient = patientService.getPatientByUuid(patientUUID);

        return orderService.getAllOrdersByPatient(patient).stream()
                .filter(order -> matchesVisitType(visitType, order))
                .filter(this::isDrugOrder)
                .map(order -> (DrugOrder) order)
                .collect(Collectors.toList());
    }

    private boolean isDrugOrder(Order order) {
        return order.getOrderType().getUuid().equals(OrderType.DRUG_ORDER_TYPE_UUID);
    }

    private boolean matchesVisitType(String visitType, Order order) {
        return order.getEncounter().getVisit().getVisitType().getName().equals(visitType);
    }

    Map<Encounter, DrugOrders> getDrugOrdersByDateAndVisitTypeFor(Visit visit, Date fromDate, Date toDate) {
        return prescriptionOrderDao
                .getDrugOrders(visit,fromDate,toDate).entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new DrugOrders(entry.getValue())));
    }

    Map<Encounter, DrugOrders> getDrugOrdersByDateAndProgramFor(String forPatientUUID, DateRange dateRange, String programName, String programEnrolmentId) {
        Patient patient = patientService.getPatientByUuid(forPatientUUID);
        OrderType drugOrderType = orderService.getOrderTypeByUuid(OrderType.DRUG_ORDER_TYPE_UUID);
        return prescriptionOrderDao
                .getDrugOrdersForProgram(patient, dateRange.getFrom(), dateRange.getTo(), drugOrderType, programName, programEnrolmentId).entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new DrugOrders(entry.getValue())));
    }
}
