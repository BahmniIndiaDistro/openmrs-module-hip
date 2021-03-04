package org.bahmni.module.hip.web.service;

import org.bahmni.module.hip.api.dao.HipOrderDao;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class HipOrderService {

    private HipOrderDao orderDao;

    @Autowired
    public HipOrderService(HipOrderDao orderDao) {
        this.orderDao = orderDao;
    }

    public List<Visit> getVisitsWithAllOrders(Patient patient, Date fromDate, Date toDate) {
        return orderDao.GetVisitsWithOrders(patient.getUuid(), fromDate, toDate);
    }

}
