package org.bahmni.module.hip.api.dao;

import java.util.Date;
import java.util.List;

public interface EncounterDao {

    List<Integer> GetEncounterIdsForVisit(String patientUUID, String visit, Date fromDate, Date toDate) ;
    List<Integer> GetEncounterIdsForProgram(String patientUUID, String program, String programEnrollmentID, Date fromDate, Date toDate) ;
    List<Integer> GetEncounterIdsForProgramForDiagnosticReport(String patientUUID, String program, String programEnrollmentID, Date fromDate, Date toDate);
}
