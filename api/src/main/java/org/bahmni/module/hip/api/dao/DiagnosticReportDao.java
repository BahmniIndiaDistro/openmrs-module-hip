package org.bahmni.module.hip.api.dao;

import org.openmrs.Obs;
import org.openmrs.OrderType;
import org.openmrs.Patient;

import java.util.Date;
import java.util.List;

public interface DiagnosticReportDao {
    List<Obs> getObsForPrograms(Patient patient, Date fromDate, Date toDate, OrderType orderType, String program, String programEnrollmentId);
}
