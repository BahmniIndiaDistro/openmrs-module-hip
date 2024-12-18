package org.bahmni.module.hip.service.impl;

import org.bahmni.module.hip.service.DrugOrderService;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.api.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DrugOrderServiceImpl implements DrugOrderService {

    private OrderService orderService;

    @Autowired
    public DrugOrderServiceImpl(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public List<DrugOrder> getAllDrugOrderFor(Patient patient, String visitType) {

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
}
