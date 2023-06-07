package org.bahmni.module.hip.api.dao;

import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Visit;

import java.util.Date;
import java.util.List;

public interface EncounterDao {

    List<Integer> getEpisodeEncounterIds();
    List<Encounter> getEncountersForVisit(Visit visit, String encounterType, Date fromDate, Date toDate);
    List<Obs> getAllObsForVisit(Visit visit, String encounterType, String conceptName, Date fromDate, Date toDate);
    List<Integer> getEncounterIdsForProgramForDiagnosticReport(String patientUUID, String program, String programEnrollmentID, Date fromDate, Date toDate);
}
