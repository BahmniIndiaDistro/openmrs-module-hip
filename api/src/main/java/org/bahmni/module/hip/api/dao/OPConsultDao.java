package org.bahmni.module.hip.api.dao;

import java.util.Date;
import java.util.List;

public interface OPConsultDao {
    List<Integer> getChiefComplaints(String patientUUID, String visit, Date fromDate, Date toDate);
    List<String[]> getMedicalHistory(String patientUUID, String visit, Date fromDate, Date toDate);
    List<Integer> getPhysicalExamination(String patientUUID, String visit, Date fromDate, Date toDate);
}
