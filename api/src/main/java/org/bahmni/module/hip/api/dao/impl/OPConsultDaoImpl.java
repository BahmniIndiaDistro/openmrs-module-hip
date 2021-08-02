package org.bahmni.module.hip.api.dao.impl;

import org.bahmni.module.hip.api.dao.OPConsultDao;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository
public class OPConsultDaoImpl implements OPConsultDao {
    private SessionFactory sessionFactory;
    final static int CONSULTATION_ENCOUNTER_TYPE_ID = 1;

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

    @Override
    public List getMedicalHistory(String patientUUID, String visit, Date fromDate, Date toDate) {
        List medicalHistory = new ArrayList();
        medicalHistory.addAll(getMedicalHistoryConditions(patientUUID, visit, fromDate, toDate));
        medicalHistory.addAll(getMedicalHistoryDiagnosis(patientUUID, visit, fromDate, toDate));
        return medicalHistory;
    }

    private List getMedicalHistoryConditions(String patientUUID, String visit, Date fromDate, Date toDate) {
        final String conditionStatusHistoryOf = "'HISTORY_OF'";
        final String conditionStatusActive = "'ACTIVE'";
        String medicalHistoryConditionsQueryString = "select\n" +
                "\tc.concept_id,\n" +
                "\tc.uuid,\n" +
                "\tencounterIdTable.encounter_id,\n" +
                "\tc.date_created\n" +
                "from\n" +
                "\tconditions c\n" +
                "inner join (\n" +
                "\tselect\n" +
                "\t\te.encounter_datetime,\n" +
                "\t\t(select\n" +
                "\t\t\te2.encounter_datetime\n" +
                "\t\tfrom\n" +
                "\t\t\tencounter e2\n" +
                "\t\twhere\n" +
                "\t\t\te2.encounter_id > e.encounter_id\n" +
                "\t\t\tand e2.patient_id = e.patient_id\n" +
                "\t\tORDER BY\n" +
                "\t\t\te.encounter_datetime LIMIT 1) end_time,\n" +
                "\t\te.encounter_id,\n" +
                "\t\te.visit_id,\n" +
                "\t\tv.patient_id\n" +
                "\tfrom\n" +
                "\t\tencounter e\n" +
                "\tinner join visit v on\n" +
                "\t\te.visit_id = v.visit_id\n" +
                "\tinner join visit_type vt on\n" +
                "\t\tv.visit_type_id = vt.visit_type_id\n" +
                "\twhere\n" +
                "\t\te.encounter_type = " + CONSULTATION_ENCOUNTER_TYPE_ID + "\n" +
                "\t\tand v.patient_id = (\n" +
                "\t\tselect\n" +
                "\t\t\tp.person_id\n" +
                "\t\tfrom\n" +
                "\t\t\tperson p\n" +
                "\t\twhere\n" +
                "\t\t\tp.uuid = :patientUUID)\n" +
                "\t\tand vt.name = :visit \n" +
                "\t\tand v.date_started between :fromDate and :toDate) encounterIdTable on\n" +
                "\tencounterIdTable.patient_id = c.patient_id\n" +
                "\t\twhere (c.status = " + conditionStatusHistoryOf +" or c.status = "+ conditionStatusActive + ") and\n" +
                "\t\t((c.date_created > encounterIdTable.encounter_datetime and encounterIdTable.end_time is NULL)\n" +
                "\t\tor c.date_created between encounterIdTable.encounter_datetime and encounterIdTable.end_time)\n" +
                "group by\n" +
                "\tc.condition_id ;";
        Query query = this.sessionFactory.getCurrentSession().createSQLQuery(medicalHistoryConditionsQueryString);
        query.setParameter("patientUUID", patientUUID);
        query.setParameter("visit", visit);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        return query.list();
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
    public List<Integer> getPhysicalExamination(String patientUUID, String visit, Date fromDate, Date toDate) {
        final String[] formNames = new String[]{"'Discharge Summary'","'Death Note'", "'Delivery Note'", "'Opioid Substitution Therapy - Intake'", "'Opportunistic Infection'",
                "'Safe Abortion'", "'ECG Notes'", "'Operative Notes'", "'USG Notes'", "'Procedure Notes'", "'Triage Reference'", "'History and Examination'"};
        final String[] conceptNames = new String[]{"'Treatment Plan'","'Next Followup Visit'","'Plan for next visit'","'Patient Category'","'Current Followup Visit After'",
                "'Plan for next visit'","'Parents name'","'Death Date'","'Contact number'","'Vitamin A Capsules Provided'","'Albendazole Given'","'Referred out'",
                "'Vitamin A Capsules Provided'","'Albendazole Given'","'Bal Vita provided'","'Bal Vita Provided by FCHV'","'Condoms given'","'Marital Status'","'Contact Number'",
                "'Transferred out (Complete Section)'"};
        String physicalExamination = "SELECT\n" +
                "\to.obs_id\n" +
                "FROM\n" +
                "\tobs o\n" +
                "\tINNER JOIN encounter e ON e.encounter_id = o.encounter_id\n" +
                "\tINNER JOIN encounter_type et ON e.encounter_type = et.encounter_type_id\n" +
                "\tINNER JOIN visit v ON v.visit_id = e.visit_id\n" +
                "WHERE\n" +
                "\tv.patient_id = (\n" +
                "\t\tSELECT\n" +
                "\t\t\tp.person_id\n" +
                "\t\tFROM\n" +
                "\t\t\tperson p\n" +
                "\t\tWHERE\n" +
                "\t\t\tp.uuid = :patientUUID)\n" +
                "\t\tAND v.visit_type_id = (\n" +
                "\t\t\tSELECT\n" +
                "\t\t\t\tvt.visit_type_id\n" +
                "\t\t\tFROM\n" +
                "\t\t\t\tvisit_type vt\n" +
                "\t\t\tWHERE\n" +
                "\t\t\t\tvt.name = :visit)\n" +
                "\t\t\tAND v.date_started BETWEEN :fromDate AND :toDate\n" +
                "\t\t\tAND o.obs_datetime NOT in( SELECT DISTINCT\n" +
                "\t\t\t\t\t(o2.obs_datetime)\n" +
                "\t\t\t\t\tFROM obs o2 WHERE o2.concept_id in(\n" +
                "\t\t\t\t\t\tSELECT\n" +
                "\t\t\t\t\t\t\tcn.concept_id FROM concept_name cn\n" +
                "\t\t\t\t\t\tWHERE\n" +
                "\t\t\t\t\t\t\tcn.name in ("+ String.join( ",", formNames) +")))\n" +
                "\t\t\tAND et.name = 'Consultation'\n" +
                "\t\t\tAND o.concept_id NOT in(\n" +
                "\t\t\t\tSELECT\n" +
                "\t\t\t\t\tcn.concept_id FROM concept_name cn\n" +
                "\t\t\t\tWHERE\n" +
                "\t\t\t\t\tcn.name in ("+ String.join(",", conceptNames) +")) ;";
        Query query = this.sessionFactory.getCurrentSession().createSQLQuery(physicalExamination);
        query.setParameter("patientUUID", patientUUID);
        query.setParameter("visit", visit);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        return query.list();
    }
}
