package org.bahmni.module.hip.service;

import org.bahmni.module.hip.model.ImmunizationRecordBundle;

import java.util.Date;
import java.util.List;

public interface ImmunizationRecordService {
    List<ImmunizationRecordBundle> getImmunizationRecordsForVisit(String patientUuid, String visitUuid, Date startDate, Date endDate);
}
