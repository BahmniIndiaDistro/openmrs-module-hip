package org.bahmni.module.hip.service;

import org.bahmni.module.hip.model.DateRange;
import org.bahmni.module.hip.model.PrescriptionBundle;

import java.util.Date;
import java.util.List;

public interface PrescriptionService {
    List<PrescriptionBundle> getPrescriptions(String patientUuid, String visitUuid, Date fromDate, Date toDate);

    List<PrescriptionBundle> getPrescriptionsForProgram(String patientIdUuid, DateRange dateRange, String programName, String programEnrolmentId);
}
