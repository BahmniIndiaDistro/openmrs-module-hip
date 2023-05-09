package org.bahmni.module.hip.api.dao;

import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Visit;

import java.util.Date;
import java.util.List;

public interface ConsultationDao {
    List<Order> getOrders(Visit visit, Date fromDate, Date toDate);
    List<Order> getOrdersForProgram(String programName, Date fromDate, Date toDate, Patient patient);
    List<Obs> getAllObsForProgram(String programName, Date fromDate, Date toDate, Patient patient);
}
