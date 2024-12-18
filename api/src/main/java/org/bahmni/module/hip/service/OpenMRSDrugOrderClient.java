package org.bahmni.module.hip.service;

import org.bahmni.module.hip.model.DateRange;
import org.bahmni.module.hip.model.DrugOrders;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Visit;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface OpenMRSDrugOrderClient {
    List<DrugOrder> drugOrdersFor(String patientUUID, String visitType);
    Map<Encounter, DrugOrders> getDrugOrdersByDateAndVisitTypeFor(Visit visit, Date fromDate, Date toDate);
    Map<Encounter, DrugOrders> getDrugOrdersByDateAndProgramFor(String forPatientUUID, DateRange dateRange, String programName, String programEnrolmentId);
}
