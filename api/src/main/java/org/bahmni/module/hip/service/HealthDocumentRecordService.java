package org.bahmni.module.hip.service;

import org.bahmni.module.hip.model.HealthDocumentRecordBundle;

import java.util.Date;
import java.util.List;

public interface HealthDocumentRecordService {
    List<HealthDocumentRecordBundle> getDocumentsForVisit(
            String visitUuid,
            String patientUuid,
            Date fromEncounterDate,
            Date toEncounterDate);
}
