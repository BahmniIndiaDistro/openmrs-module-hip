package org.bahmni.module.hip.api.dao.impl;

import org.bahmni.module.hip.api.dao.EncounterDao;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public class EncounterDaoImpl implements EncounterDao {

    private SessionFactory sessionFactory;

    @Autowired
    public EncounterDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private String sqlGetEncounterIdsForVisitForPrescriptions = "select\n" +
            "\te.encounter_id\n" +
            "from\n" +
            "\tvisit as v\n" +
            "inner join visit_type as vt on\n" +
            "\tvt.visit_type_id = v.visit_type_id\n" +
            "inner join encounter as e on\n" +
            "\te.visit_id = v.visit_id\n" +
            "where\n" +
            "\tvt.name = :visit\n" +
            "\tand e.encounter_id not in (\n" +
            "\tselect\n" +
            "\t\tencounter_id\n" +
            "\tfrom\n" +
            "\t\tepisode_encounter)\n" +
            "\tand v.date_started between :fromDate and :toDate\n" +
            "\tand v.patient_id = (select person_id from person as p2 where p2.uuid = :patientUUID);";

    private String sqlGetEncounterIdsForProgramForPrescriptions = "select\n" +
            "\tle.encounter_id\n" +
            "from\n" +
            "\tpatient_program as pp\n" +
            "inner join program as p on\n" +
            "\tpp.program_id = p.program_id\n" +
            "inner join (\n" +
            "\tselect\n" +
            "\t\tee.episode_id,\n" +
            "\t\tee.encounter_id,\n" +
            "\t\tepp.patient_program_id\n" +
            "\tfrom\n" +
            "\t\tepisode_encounter as ee\n" +
            "\tinner join episode_patient_program as epp on\n" +
            "\t\tee.episode_id = epp.episode_id) as le on\n" +
            "\tle.patient_program_id = pp.patient_program_id\n" +
            "where\n" +
            "\tp.name = :programName \n" +
            "\tand pp.patient_id = (select person_id from person as p2 where p2.uuid = :patientUUID)\n" +
            "\tand pp.patient_program_id in (\n" +
            "\tselect\n" +
            "\t\tpatient_program_id\n" +
            "\tfrom\n" +
            "\t\tprogram_attribute_type as pat\n" +
            "\tinner join patient_program_attribute as ppa on\n" +
            "\t\tpat.program_attribute_type_id = ppa.attribute_type_id\n" +
            "\twhere\n" +
            "\t\tname = \"ID_Number\"\n" +
            "\t\tand value_reference = :programEnrollmentId )\n" +
            "\tand pp.date_enrolled between :fromDate and :toDate ;";

    private String sqlGetEncounterIdsForVisitForDiagnosticReports = "select \n" +
            "            e.encounter_id \n" +
            "            from \n" +
            "            visit as v \n" +
            "            inner join visit_type as vt on \n" +
            "            \tvt.visit_type_id = v.visit_type_id \n" +
            "            inner join encounter as e on \n" +
            "            \te.visit_id = v.visit_id\n" +
            "            inner join obs o on \n" +
            "            \to.encounter_id = e.encounter_id \n" +
            "            where\n" +
            "            (e.encounter_type =6 or e.encounter_type =9) \n" +
            "            and vt.name = :visit\n" +
            "            and o.concept_id =35\n" +
            "            and o.void_reason is null\n" +
            "            and e.encounter_id not in ( \n" +
            "            SELECT encounter_id from encounter e where e.visit_id in (SELECT visit_id from encounter e2 \n" +
            "                      inner join episode_encounter ee  on e2.encounter_id = ee.encounter_id)) \n" +
            "            and v.date_started between :fromDate and :toDate \n" +
            "            and v.patient_id = (select person_id from person as p2 where p2.uuid = :patientUUID) ;";

    private String sqlGetEncounterIdsForProgramForDiagnosticReports = "SELECT encounter_id from" +
            "(SELECT o.encounter_id,p.uuid,p2.name,ppa.value_reference,pp.date_enrolled,o.concept_id," +
            "o.value_text,o.void_reason from obs o\n" +
            "\tinner join person p on \t\n" +
            "\t\tp.person_id = o.person_id \n" +
            "\tinner join patient_program pp on \t\n" +
            "\t\tpp.patient_id = p.person_id \n" +
            "\tinner join program p2 on \t\n" +
            "\t\tp2.program_id = pp.program_id \n" +
            "\tinner join patient_program_attribute ppa on \t\n" +
            "\t\tppa.patient_program_id = pp.patient_program_id \n" +
            "\twhere encounter_id in \n" +
            "\t(SELECT encounter_id from encounter e  where (e.encounter_type =6 or e.encounter_type =9) and visit_id  in \n" +
            "\t\t(SELECT visit_id from encounter e2\n" +
            "\t\t\tinner join episode_encounter ee  on \n" +
            "\t\t\t\te2.encounter_id = ee.encounter_id ))) as t \n" +
            "\t\t\t\t\t  where concept_id=35 \n" +
            "\t\t\t\t\t\t\tand void_reason is null\n" +
            "\t\t\t\t\t\t\tand uuid = :patientUUID\n" +
            "\t\t\t\t\t\t\tand name= :programName\n" +
            "\t\t\t\t\t\t\tand value_reference = :programEnrollmentId\n" +
            "\t\t\t\t\t\t\tand date_enrolled between :fromDate and :toDate ;\n";

    @Override
    public List<Integer> GetEncounterIdsForVisitForPrescriptions(String patientUUID, String visit, Date fromDate, Date toDate) {

        Query query = this.sessionFactory.getCurrentSession().createSQLQuery(sqlGetEncounterIdsForVisitForPrescriptions);
        query.setParameter("patientUUID", patientUUID);
        query.setParameter("visit", visit);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);

        return query.list();

    }

    @Override
    public List<Integer> GetEncounterIdsForProgramForPrescriptions(String patientUUID, String program, String programEnrollmentID, Date fromDate, Date toDate) {
        Query query = this.sessionFactory.getCurrentSession().createSQLQuery(sqlGetEncounterIdsForProgramForPrescriptions);
        query.setParameter("patientUUID", patientUUID);
        query.setParameter("programName", program);
        query.setParameter("programEnrollmentId", programEnrollmentID);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);

        return query.list();
    }

    @Override
    public List<Integer> GetEncounterIdsForProgramForDiagnosticReport(String patientUUID, String program, String programEnrollmentID, Date fromDate, Date toDate) {
        Query query = this.sessionFactory.getCurrentSession().createSQLQuery(sqlGetEncounterIdsForProgramForDiagnosticReports);
        query.setParameter("patientUUID", patientUUID);
        query.setParameter("programName", program);
        query.setParameter("programEnrollmentId", programEnrollmentID);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);

        return query.list();
    }

    @Override
    public List<Integer> GetEncounterIdsForVisitForDiagnosticReport(String patientUUID, String visit, Date fromDate, Date toDate) {

        Query query = this.sessionFactory.getCurrentSession().createSQLQuery(sqlGetEncounterIdsForVisitForDiagnosticReports);
        query.setParameter("patientUUID", patientUUID);
        query.setParameter("visit", visit);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);


        return query.list();
    }
}
