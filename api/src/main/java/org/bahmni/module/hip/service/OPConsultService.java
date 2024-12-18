package org.bahmni.module.hip.service;

import org.bahmni.module.hip.model.DateRange;
import org.bahmni.module.hip.model.OPConsultBundle;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

public interface OPConsultService {
    List<OPConsultBundle> getOpConsultsForVisit(String patientUuid, String visitUuid, Date fromDate, Date toDate) throws ParseException;

    List<OPConsultBundle> getOpConsultsForProgram(String patientUuid, DateRange dateRange, String programName, String programEnrollmentId);
}
