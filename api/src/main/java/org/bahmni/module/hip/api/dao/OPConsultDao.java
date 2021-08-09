package org.bahmni.module.hip.api.dao;

import org.openmrs.Obs;
import org.openmrs.Patient;

import java.util.Date;
import java.util.List;

public interface OPConsultDao {
    List<Obs> getChiefComplaints(Patient patient, String visit, Date fromDate, Date toDate);
    List<String[]> getMedicalHistory(String patientUUID, String visit, Date fromDate, Date toDate);
    List<Integer> getPhysicalExamination(String patientUUID, String visit, Date fromDate, Date toDate);
    List<Integer> getProcedures(String patientUUID, String visit, Date fromDate, Date toDate);
}
