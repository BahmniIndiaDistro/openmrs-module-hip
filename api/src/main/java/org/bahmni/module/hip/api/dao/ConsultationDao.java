package org.bahmni.module.hip.api.dao;

import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Visit;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ConsultationDao {
    Map<Encounter, List<Order>> getOrders(Visit visit, Date fromDate, Date toDate);
    Map<Encounter, List<Order>> getOrdersForProgram(String programName, Date fromDate, Date toDate, Patient patient);
    List<Obs> getAllObsForProgram(String programName, Date fromDate, Date toDate, Patient patient);
}
