package org.bahmni.module.hip.api.dao;

import org.openmrs.Obs;

import java.util.Date;
import java.util.List;

public interface OPConsultDao {
    List<Integer> getChiefComplaints(String patientUUID, String visit, Date fromDate, Date toDate);
}
