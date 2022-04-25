package org.bahmni.module.hip.api.dao.impl;

import org.bahmni.module.hip.api.dao.CareContextRepository;
import org.bahmni.module.hip.model.PatientCareContext;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.transform.Transformers;
import org.hibernate.type.IntegerType;
import org.hibernate.type.StringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CareContextRepositoryImpl implements CareContextRepository {
    private SessionFactory sessionFactory;

    @Autowired
    public CareContextRepositoryImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<PatientCareContext> getPatientCareContext(String patientUuid) {
        Query query = this.sessionFactory.getCurrentSession().createSQLQuery("SELECT\n" +
                "    case\n" +
                "        when care_context = 'PROGRAM' then value_reference\n" +
                "        else visit_type_id end as careContextReference,\n" +
                "    care_context as careContextType,\n" +
                "    case when care_context = 'PROGRAM' then\n" +
                "        concat (program_name,\" / \",visit_startDate)\n" +
                "    else\n" +
                "        concat (visit_type_name,\" / \",visit_startDate)\n" +
                "    end as careContextName\n" +
                "from\n" +
                "    (\n" +
                "        select\n" +
                "            ppa.value_reference,p3.uuid,e.patient_id, p2.program_id, vt.visit_type_id , vt.name ,\n" +
                "            v.date_started As visit_startDate,\n" +
                "            pp.patient_program_id , p2.name as program_name, vt.name as visit_type_name,\n" +
                "            case\n" +
                "                when p2.program_id is null then 'VISIT_TYPE'\n" +
                "                else 'PROGRAM'\n" +
                "                end as care_context\n" +
                "        from\n" +
                "            encounter e\n" +
                "                left join episode_encounter ee on\n" +
                "                    e.encounter_id = ee.encounter_id\n" +
                "                left join episode_patient_program epp on\n" +
                "                    ee.episode_id = epp.episode_id\n" +
                "                left join patient_program pp on\n" +
                "                    epp.patient_program_id = pp.patient_program_id\n" +
                "                left join program p2 on\n" +
                "                    pp.program_id = p2.program_id\n" +
                "                left join visit v on\n" +
                "                        v.visit_id = e.visit_id\n" +
                "                    and v.patient_id = e.patient_id\n" +
                "                left join visit_type vt on\n" +
                "                    v.visit_type_id = vt.visit_type_id\n" +
                "                left join person p3 on\n" +
                "                \te.patient_id = p3.person_id\n" +
                "                left join patient_program_attribute ppa on\n" +
                "                \tpp.patient_program_id=ppa.patient_program_id) as a\n" +
                "where\n" +
                "        a.uuid = :patientUuid\n" +
                " group by \n" +
                "visit_startDate, \n" +
                "case when care_context = 'PROGRAM' \n" +
                "then patient_program_id else visit_type_id \n" +
                "end")
                .addScalar("careContextReference", IntegerType.INSTANCE)
                .addScalar("careContextType", StringType.INSTANCE)
                .addScalar("careContextName", StringType.INSTANCE);
        query.setParameter("patientUuid", patientUuid);
        return query.setResultTransformer(Transformers.aliasToBean(PatientCareContext.class)).list();
    }

    public List<PatientCareContext> getNewPatientCareContext(Integer patientId) {
        Query query = this.sessionFactory.getCurrentSession().createSQLQuery("select\n" +
                "  care_context_type as careContextType,\n" +
                "  case when care_context_type = 'PROGRAM' then\n" +
                "  concat (program_name,\" / \",visit_startDate)\n" +
                "  else\n" +
                "  concat (visit_type_name,\" / \",visit_startDate)\n" +
                "  end as careContextName,\n" +
                "  case when care_context_type = 'PROGRAM' then value_reference else visit_type_id end as careContextReference\n" +
                "from\n" +
                "  (\n" +
                "    select\n" +
                "      ppa.value_reference,\n" +
                "      p3.uuid,\n" +
                "      e.patient_id,\n" +
                "      p2.program_id,\n" +
                "      vt.visit_type_id,\n" +
                "      v.date_started As visit_startDate,\n" +
                "      vt.name,\n" +
                "      pp.patient_program_id,\n" +
                "      p2.name as program_name,\n" +
                "      vt.name as visit_type_name,\n" +
                "      case when p2.program_id is null then 'VISIT_TYPE' else 'PROGRAM' end as care_context_type\n" +
                "    from\n" +
                "      encounter as e\n" +
                "      left join episode_encounter ee on e.encounter_id = ee.encounter_id\n" +
                "      left join episode_patient_program epp on ee.episode_id = epp.episode_id\n" +
                "      left join patient_program pp on epp.patient_program_id = pp.patient_program_id\n" +
                "      left join program p2 on pp.program_id = p2.program_id\n" +
                "      left join visit v on v.visit_id = e.visit_id\n" +
                "      and v.patient_id = e.patient_id\n" +
                "      left join visit_type vt on v.visit_type_id = vt.visit_type_id\n" +
                "      left join person p3 on e.patient_id = p3.person_id\n" +
                "      left join patient_program_attribute ppa on pp.patient_program_id = ppa.patient_program_id\n" +
                "    where\n" +
                "      v.visit_id = (\n" +
                "        select\n" +
                "          max(visit_id)\n" +
                "        from\n" +
                "          visit\n" +
                "        where\n" +
                "          patient_id = :patientId\n" +
                "      )\n" +
                "  ) as a\n" +
                "group by\n" +
                "  visit_startDate;\n").addScalar("careContextReference", IntegerType.INSTANCE)
                .addScalar("careContextType", StringType.INSTANCE)
                .addScalar("careContextName", StringType.INSTANCE);
        query.setParameter("patientId", patientId);
        return query.setResultTransformer(Transformers.aliasToBean(PatientCareContext.class)).list();
    }

}
