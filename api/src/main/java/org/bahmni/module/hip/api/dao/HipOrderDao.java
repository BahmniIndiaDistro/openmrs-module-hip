package org.bahmni.module.hip.api.dao;

import org.openmrs.Visit;

import java.util.Date;
import java.util.List;

public interface HipOrderDao {
    List<Visit> GetVisitsWithOrders(String patientUUID, Date fromDate, Date toDate) ;

}
