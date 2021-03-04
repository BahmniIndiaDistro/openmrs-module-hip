package org.bahmni.module.hip.api.dao.impl;

import org.bahmni.module.hip.api.dao.HipOrderDao;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.Visit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public class HipOrderDaoImpl implements HipOrderDao {

    private SessionFactory sessionFactory;

    @Autowired
    public HipOrderDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }


    @Override
    public List<Visit> GetVisitsWithOrders(String patientUUID, Date fromDate, Date toDate) {
        Session currentSession = this.sessionFactory.getCurrentSession();

        Query queryVisitsWithDrugOrders = currentSession.createQuery("select v from Orders o, Encounter e, Visit v where o.encounter = e.encounterId and e.visit = v.visitId and v.patient = (:patientId) " +
                "and o.voided = false  group by v.visitId order by v.startDatetime desc");

        queryVisitsWithDrugOrders.setParameter("patientId", patientUUID);

        return (List<Visit>) queryVisitsWithDrugOrders.list();
    }

}
