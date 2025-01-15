package org.bahmni.module.hip.service;

import org.bahmni.module.hip.model.WellnessRecordBundle;

import java.util.Date;
import java.util.List;

public interface WellnessRecordService {
    List<WellnessRecordBundle> getWellnessForVisit(String patientUuid, String visitUuid, Date fromEncounterDate, Date toEncounterDate);
}
