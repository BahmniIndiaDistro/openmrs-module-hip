package org.bahmni.module.hip.service;

import org.bahmni.module.hip.model.DateRange;
import org.bahmni.module.hip.model.DischargeSummaryBundle;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

public interface DischargeSummaryService {
    List<DischargeSummaryBundle> getDischargeSummaryForVisit(String patientUuid, String visitUuid, Date fromDate, Date toDate) throws ParseException;

    List<DischargeSummaryBundle> getDischargeSummaryForProgram(String patientUuid, DateRange dateRange, String programName, String programEnrollmentId);
}
