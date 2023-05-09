package org.bahmni.module.hip.api.dao;

import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Visit;

import java.util.Date;
import java.util.List;

public interface EncounterDao {

    List<Integer> GetEpisodeEncounterIds();
    List<Order> GetOrdersForVisit(Visit visit,Date fromDate, Date toDate);
    List<Encounter> GetEncountersForVisit(Visit visit, String encounterType, Date fromDate, Date toDate);
    List<Obs> GetAllObsForVisit(Visit visit, String encounterType, String conceptName,Date fromDate, Date toDate);
    List<Integer> GetEncounterIdsForProgramForPrescriptions(String patientUUID, String program, String programEnrollmentID, Date fromDate, Date toDate) ;
    List<Integer> GetEncounterIdsForProgramForDiagnosticReport(String patientUUID, String program, String programEnrollmentID, Date fromDate, Date toDate);
}
