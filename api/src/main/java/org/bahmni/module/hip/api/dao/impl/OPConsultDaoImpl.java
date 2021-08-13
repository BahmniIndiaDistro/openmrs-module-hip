package org.bahmni.module.hip.api.dao.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bahmni.module.hip.api.dao.OPConsultDao;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.openmrs.*;
import org.openmrs.api.EncounterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository
public class OPConsultDaoImpl implements OPConsultDao {
    public static final String CHIEF_COMPLAINT = "Chief Complaint";
    public static final String PROCEDURE_NOTES = "Procedure Notes, Procedure";
    public static final String CONSULTATION = "Consultation";
    private static final String CODED_DIAGNOSIS = "Coded Diagnosis";
    private SessionFactory sessionFactory;
    final static int CONSULTATION_ENCOUNTER_TYPE_ID = 1;
    protected static final Log log = LogFactory.getLog(PrescriptionOrderDaoImpl.class);

    @Autowired
    public OPConsultDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<Obs> getChiefComplaints(Patient patient, String visit, Date fromDate, Date toDate) {
        Criteria criteria = this.sessionFactory.openSession().createCriteria(Obs.class, "o");
            criteria.createCriteria("o.concept", "c");
            criteria.createCriteria("c.names", "cn");
            criteria.createCriteria("o.encounter", "e");
            criteria.createCriteria("e.visit", "v");
            criteria.createCriteria("v.visitType", "vt");
            criteria.add(Restrictions.eq("vt.name", visit));
            criteria.add(Restrictions.eq("cn.name", CHIEF_COMPLAINT));
            criteria.add(Restrictions.eq("o.voided", false));
            criteria.add(Restrictions.eq("v.patient", patient));
            criteria.add(Restrictions.isNotNull("o.valueCoded"));
            criteria.add(Restrictions.between("v.dateCreated", fromDate, toDate));
            criteria.add(Restrictions.eq("cn.localePreferred", true));
        return criteria.list();
    }

    @Override
    public List getMedicalHistory(Patient patient, String visit, Date fromDate, Date toDate) {
        List medicalHistory = new ArrayList();
        medicalHistory.addAll(getMedicalHistoryConditions(patient, visit, fromDate, toDate));
        return medicalHistory;
    }

    private List getMedicalHistoryConditions(Patient patient, String visit, Date fromDate, Date toDate) {
        final String conditionStatusHistoryOf = "'HISTORY_OF'";
        final String conditionStatusActive = "'ACTIVE'";
//        String medicalHistoryConditionsQueryString = "select\n" +
//                "\tc.concept_id,\n" +
//                "\tc.uuid,\n" +
//                "\tencounterIdTable.encounter_id,\n" +
//                "\tc.date_created\n" +
//                "from\n" +
//                "\tconditions c\n" +
//                "inner join (\n" +
//                "\tselect\n" +
//                "\t\te.encounter_datetime,\n" +
//                "\t\t(select\n" +
//                "\t\t\te2.encounter_datetime\n" +
//                "\t\tfrom\n" +
//                "\t\t\tencounter e2\n" +
//                "\t\twhere\n" +
//                "\t\t\te2.encounter_id > e.encounter_id\n" +
//                "\t\t\tand e2.patient_id = e.patient_id\n" +
//                "\t\tORDER BY\n" +
//                "\t\t\te.encounter_datetime LIMIT 1) end_time,\n" +
//                "\t\te.encounter_id,\n" +
//                "\t\te.visit_id,\n" +
//                "\t\tv.patient_id\n" +
//                "\tfrom\n" +
//                "\t\tencounter e\n" +
//                "\tinner join visit v on\n" +
//                "\t\te.visit_id = v.visit_id\n" +
//                "\tinner join visit_type vt on\n" +
//                "\t\tv.visit_type_id = vt.visit_type_id\n" +
//                "\twhere\n" +
//                "\t\te.encounter_type = " + CONSULTATION_ENCOUNTER_TYPE_ID + "\n" +
//                "\t\tand v.patient_id = (\n" +
//                "\t\tselect\n" +
//                "\t\t\tp.person_id\n" +
//                "\t\tfrom\n" +
//                "\t\t\tperson p\n" +
//                "\t\twhere\n" +
//                "\t\t\tp.uuid = :patientUUID)\n" +
//                "\t\tand vt.name = :visit \n" +
//                "\t\tand v.date_started between :fromDate and :toDate) encounterIdTable on\n" +
//                "\tencounterIdTable.patient_id = c.patient_id\n" +
//                "\t\twhere (c.status = " + conditionStatusHistoryOf +" or c.status = "+ conditionStatusActive + ") and\n" +
//                "\t\t((c.date_created > encounterIdTable.encounter_datetime and encounterIdTable.end_time is NULL)\n" +
//                "\t\tor c.date_created between encounterIdTable.encounter_datetime and encounterIdTable.end_time)\n" +
//                "group by\n" +
//                "\tc.condition_id ;";
//        Query query = this.sessionFactory.getCurrentSession().createSQLQuery(medicalHistoryConditionsQueryString);
//        query.setParameter("patientUUID", patientUUID);
//        query.setParameter("visit", visit);
//        query.setParameter("fromDate", fromDate);
//        query.setParameter("toDate", toDate);
        return new ArrayList();
    }

    private List getMedicalHistoryDiagnosis(String patientUUID, String visit, Date fromDate, Date toDate) {
        final int diagnosisValueConceptId = 15;
        String medicalHistoryDiagnosisQueryString = "select\n " +
            "\t\t\to.value_coded, o.uuid, o.encounter_id, o.date_created \n" +
            "\t\t\tfrom obs o inner join encounter e on \n" +
            "\t\t\te.encounter_id = o.encounter_id \n" +
            "\t\t\tinner join visit v on \n" +
            "\t\t\tv.visit_id = e.visit_id \n" +
            "\t\t\tinner join visit_type vt on \n" +
            "\t\t\tv.visit_type_id = vt.visit_type_id \n" +
            "\t\t\twhere v.patient_id = (select p.person_id from person p where p.uuid = :patientUUID) \n" +
            "\t\t\tand v.date_started between :fromDate and :toDate \n" +
            "\t\t\tand e.encounter_type = " + CONSULTATION_ENCOUNTER_TYPE_ID + " \n" +
            "\t\t\tand vt.name = :visit and o.concept_id = " + diagnosisValueConceptId + ";";
        Query query = this.sessionFactory.getCurrentSession().createSQLQuery(medicalHistoryDiagnosisQueryString);
        query.setParameter("patientUUID", patientUUID);
        query.setParameter("visit", visit);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        return query.list();
    }

    @Override
    public List<Obs> getPhysicalExamination(Patient patient, String visit, Date fromDate, Date toDate) {
        final String[] formNames = new String[]{"Discharge Summary","Death Note", "Delivery Note", "Opioid Substitution Therapy - Intake", "Opportunistic Infection",
                "Safe Abortion", "ECG Notes", "Operative Notes", "USG Notes", "Procedure Notes", "Triage Reference", "History and Examination", "Visit Diagnoses"};
        Criteria criteria = this.sessionFactory.openSession().createCriteria(Obs.class, "o");
        criteria.createCriteria("o.concept", "c");
        criteria.createCriteria("c.names", "cn");
        criteria.createCriteria("o.encounter", "e");
        criteria.createCriteria("e.encounterType", "et");
        criteria.createCriteria("e.visit", "v");
        criteria.createCriteria("v.visitType", "vt");
        criteria.add(Restrictions.eq("vt.name", visit));
        criteria.add(Restrictions.not(Restrictions.in("cn.name", formNames)));
        criteria.add(Restrictions.eq("et.name", CONSULTATION));
        criteria.add(Restrictions.isNull("o.obsGroup"));
        criteria.add(Restrictions.eq("v.patient", patient));
        criteria.add(Restrictions.between("v.dateCreated", fromDate, toDate));
        criteria.add(Restrictions.eq("cn.localePreferred", true));
        return criteria.list();
    }

    @Override
    public List<Obs> getProcedures(Patient patient, String visit, Date fromDate, Date toDate) {
        Criteria criteria = this.sessionFactory.openSession().createCriteria(Obs.class, "o");
        criteria.createCriteria("o.concept", "c");
        criteria.createCriteria("c.names", "cn");
        criteria.createCriteria("o.encounter", "e");
        criteria.createCriteria("e.visit", "v");
        criteria.createCriteria("v.visitType", "vt");
        criteria.add(Restrictions.eq("vt.name", visit));
        criteria.add(Restrictions.eq("cn.name", PROCEDURE_NOTES));
        criteria.add(Restrictions.eq("o.voided", false));
        criteria.add(Restrictions.eq("v.patient", patient));
        criteria.add(Restrictions.between("v.dateCreated", fromDate, toDate));
        return criteria.list();
    }


}
