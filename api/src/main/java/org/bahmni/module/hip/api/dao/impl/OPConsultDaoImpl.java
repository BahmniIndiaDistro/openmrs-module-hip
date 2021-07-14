package org.bahmni.module.hip.api.dao.impl;

import org.bahmni.module.hip.api.dao.OPConsultDao;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

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

    @Override
    public List getMedicalHistory(String patientUUID, String visit, Date fromDate, Date toDate) {
        String medicalHistoryQueryString = "select\n" +
                "\tc.condition_id,\n" +
                "\tc.concept_id,\n" +
                "\tc.uuid,\n" +
                "\tencounterIdTable.encounter_id,\n" +
                "\tc.date_created \n" +
                "from\n" +
                "\tconditions c\n" +
                "inner join (\n" +
                "\tselect\n" +
                "\t\te.encounter_datetime,\n" +
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
                "\t\tv.patient_id = (\n" +
                "\t\tselect\n" +
                "\t\t\tp.person_id\n" +
                "\t\tfrom\n" +
                "\t\t\tperson p\n" +
                "\t\twhere\n" +
                "\t\t\tp.uuid = :patientUUID)\n" +
                "\t\tand vt.name = :visit " +
                "\t\tand v.date_started between :fromDate and :toDate) encounterIdTable on\n" +
                "\tencounterIdTable.patient_id = c.patient_id\n" +
                "where\n" +
                "\tc.status = \"HISTORY_OF\"\n" +
                "\tand c.date_created between encounterIdTable.encounter_datetime and " +
                "date_add(encounterIdTable.encounter_datetime, interval 45 minute) group by c.condition_id;";
        Query query = this.sessionFactory.getCurrentSession().createSQLQuery(medicalHistoryQueryString);
        query.setParameter("patientUUID", patientUUID);
        query.setParameter("visit", visit);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        return query.list();
    }

    @Override
    public List<Integer> getPhysicalExamination(String patientUUID, String visit, Date fromDate, Date toDate) {
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
                "\t\t\t\t\t\t\tcn.name = 'Discharge Summary'\n" +
                "\t\t\t\t\t\t\tOR cn.name = 'Death Note'\n" +
                "\t\t\t\t\t\t\tOR cn.name = 'Delivery Note'\n" +
                "\t\t\t\t\t\t\tOR cn.name = 'Opioid Substitution Therapy - Intake'\n" +
                "\t\t\t\t\t\t\tOR cn.name = 'Opportunistic Infection'\n" +
                "\t\t\t\t\t\t\tOR cn.name = 'Safe Abortion'\n" +
                "\t\t\t\t\t\t\tOR cn.name = 'ECG Notes'\n" +
                "\t\t\t\t\t\t\tOR cn.name = 'Operative Notes'\n" +
                "\t\t\t\t\t\t\tOR cn.name = 'USG Notes'\n" +
                "\t\t\t\t\t\t\tOR cn.name = 'Procedure Notes'\n" +
                "\t\t\t\t\t\t\tOR cn.name = 'Triage Reference'))\n" +
                "\t\t\tAND et.name = 'Consultation'\n" +
                "\t\t\tAND o.concept_id NOT in(\n" +
                "\t\t\t\tSELECT\n" +
                "\t\t\t\t\tcn.concept_id FROM concept_name cn\n" +
                "\t\t\t\tWHERE\n" +
                "\t\t\t\t\tcn.name = 'Treatment Plan'\n" +
                "\t\t\t\t\tOR cn.name = 'Next Followup Visit'\n" +
                "\t\t\t\t\tOR cn.name = 'Plan for next visit'\n" +
                "\t\t\t\t\tOR cn.name = 'Patient Category'\n" +
                "\t\t\t\t\tOR cn.name = 'Current Followup Visit After'\n" +
                "\t\t\t\t\tOR cn.name = 'Plan for next visit'\n" +
                "\t\t\t\t\tOR cn.name = 'Parents name'\n" +
                "\t\t\t\t\tOR cn.name = 'Death Date'\n" +
                "\t\t\t\t\tOR cn.name = 'Contact number'\n" +
                "\t\t\t\t\tOR cn.name = 'Vitamin A Capsules Provided'\n" +
                "\t\t\t\t\tOR cn.name = 'Albendazole Given'\n" +
                "\t\t\t\t\tOR cn.name = 'Referred out'\n" +
                "\t\t\t\t\tOR cn.name = 'Vitamin A Capsules Provided'\n" +
                "\t\t\t\t\tOR cn.name = 'Albendazole Given'\n" +
                "\t\t\t\t\tOR cn.name = 'Bal Vita provided'\n" +
                "\t\t\t\t\tOR cn.name = 'Bal Vita Provided by FCHV'\n" +
                "\t\t\t\t\tOR cn.name = 'Condoms given'\n" +
                "\t\t\t\t\tOR cn.name = 'Marital Status'\n" +
                "\t\t\t\t\tOR cn.name = 'Contact Number'\n" +
                "\t\t\t\t\tOR cn.name = 'Transferred out (Complete Section)') ;";
        Query query = this.sessionFactory.getCurrentSession().createSQLQuery(physicalExamination);
        query.setParameter("patientUUID", patientUUID);
        query.setParameter("visit", visit);
        query.setParameter("fromDate", fromDate);
        query.setParameter("toDate", toDate);
        return query.list();
    }
}
