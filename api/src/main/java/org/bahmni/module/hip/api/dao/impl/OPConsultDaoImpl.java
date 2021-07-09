package org.bahmni.module.hip.api.dao.impl;

import org.bahmni.module.hip.api.dao.OPConsultDao;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.openmrs.Obs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class OPConsultDaoImpl implements OPConsultDao {
    private SessionFactory sessionFactory;

    @Autowired
    public OPConsultDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<Integer> getChiefComplaints(String patientUUID, String visit, Date fromDate, Date toDate) {
        String chiefComplaintsQueryString = "select o1.obs_id from obs o1 where o1.obs_datetime in (select max(o2.obs_datetime) from obs o2\n" +
                "\t\tinner join encounter e on o2.encounter_id = e.encounter_id \n" +
                "\t\tinner join visit v on e.visit_id = v.visit_id \n" +
                "\t\tinner join visit_type vt on v.visit_type_id = vt.visit_type_id where vt.name = :visit" +
                "\t\tand v.date_started between :fromDate and :toDate and \n" +
                "\t\to2.person_id = (select p.person_id from person p where p.uuid = :patientUUID)\n" +
                "\t\tand o2.concept_id = 156 group by o2.obs_group_id) and o1.value_coded is not null ;";
        Query query = this.sessionFactory.getCurrentSession().createSQLQuery(chiefComplaintsQueryString);
        query.setParameter("patientUUID", patientUUID);
        query.setParameter("visit", visit);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);

        return query.list();
    }
}
