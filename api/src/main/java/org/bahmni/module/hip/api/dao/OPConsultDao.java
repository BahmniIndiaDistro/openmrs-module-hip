package org.bahmni.module.hip.api.dao;

import org.openmrs.Obs;
import org.openmrs.Patient;

import java.util.Date;
import java.util.List;

public interface OPConsultDao {
    List<Obs> getChiefComplaints(Patient patient, String visit, Date fromDate, Date toDate);
    List<String[]> getMedicalHistory(Patient patient, String visit, Date fromDate, Date toDate);
    List<Obs> getPhysicalExamination(Patient patient, String visit, Date fromDate, Date toDate);
    List<Obs> getProcedures(Patient patient, String visit, Date fromDate, Date toDate);
}
