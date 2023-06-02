package org.bahmni.module.hip.api.dao;

import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.Visit;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface PrescriptionOrderDao {
    Map<Encounter, List<DrugOrder>> getDrugOrders(Visit visit, Date fromDate, Date toDate);
    Map<Encounter, List<DrugOrder>>getDrugOrdersForProgram(Patient patient, Date fromDate, Date toDate, OrderType orderType, String program, String programEnrollmentId);

}
