package org.bahmni.module.hip.service;

import org.bahmni.module.hip.model.DateRange;
import org.bahmni.module.hip.model.DiagnosticReportBundle;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public interface DiagnosticReportService {
    List<DiagnosticReportBundle> getDiagnosticReportsForVisit(String patientUuid, String visitUuid, Date fromDate, Date toDate) throws ParseException;

    List<DiagnosticReportBundle> getDiagnosticReportsForProgram(String patientUuid, DateRange dateRange, String programName, String programEnrollmentId);

    HashMap<Encounter, List<Obs>> getAllObservationsForPrograms(Date fromDate, Date toDate,
                                                                Patient patient,
                                                                String programName,
                                                                String programEnrollmentId);

    List<DiagnosticReportBundle> getLabResultsForVisits(String patientUuid, String visitUuid, String fromDate, String ToDate);

    List<DiagnosticReportBundle> getLabResultsForPrograms(String patientUuid, DateRange dateRange, String programName, String programEnrollmentId);
}
