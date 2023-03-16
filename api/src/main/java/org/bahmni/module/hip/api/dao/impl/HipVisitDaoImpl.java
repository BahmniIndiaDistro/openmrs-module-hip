package org.bahmni.module.hip.api.dao.impl;

import org.bahmni.module.hip.api.dao.HipVisitDao;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.VisitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class HipVisitDaoImpl implements HipVisitDao {

    private SessionFactory sessionFactory;

    @Autowired
    public HipVisitDaoImpl(SessionFactory sessionFactory, VisitService visitService) {
        this.sessionFactory = sessionFactory;
    }

    private String sqlGetVisitIdsForProgramForLabResults = "\n" +
            "select distinct e.visit_id from encounter as e, patient_program_attribute as ppa, visit as v, patient_program pp, program p where \n" +
            "p.name = :programName and \n" +
            "pp.program_id = p.program_id and pp.patient_id = v.patient_id\n" +
            "and ppa.value_reference = :programEnrollmentId and ppa.attribute_type_id = 1 and\n" +
            " v.date_started between :fromDate and :toDate and\n" +
            "e.visit_id in (select e1.visit_id from encounter as e1 inner join episode_encounter on episode_encounter.encounter_id = e1.encounter_id) \n" +
            "and v.patient_id in (select person_id from person as p2 where p2.uuid = :patientUUID) ;";



    @Override
    public List<Integer> GetVisitIdsForProgramForLabResults(String patientUUID, String program, String programEnrollmentID, Date fromDate, Date toDate) {
        Query query = this.sessionFactory.getCurrentSession().createSQLQuery(sqlGetVisitIdsForProgramForLabResults);
        query.setParameter("patientUUID", patientUUID);
        query.setParameter("programName", program);
        query.setParameter("programEnrollmentId", programEnrollmentID);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);

        return query.list();
    }
}
