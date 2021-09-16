package org.bahmni.module.hip.api.dao.impl;

import org.bahmni.module.hip.api.dao.ConsultationDao;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ObsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

@Repository
public class ConsultationDaoImpl implements ConsultationDao {
    private final ObsService obsService;
    public static final String OPD = "OPD";
    public static final String CONSULTATION = "Consultation";
    public static final String CHIEF_COMPLAINT = "Chief Complaint";

    @Autowired
    public ConsultationDaoImpl(ObsService obsService) {
        this.obsService = obsService;
    }

    @Override
    public List<Obs> getChiefComplaints(Patient patient, String visit, Date fromDate, Date toDate) {
        List<Obs> patientObs = obsService.getObservationsByPerson(patient);
        List<Obs> chiefComplaintObsMap = new ArrayList<>();
        for(Obs o :patientObs){
            if(Objects.equals(o.getEncounter().getEncounterType().getName(), CONSULTATION)
                    && o.getEncounter().getVisit().getStartDatetime().after(fromDate)
                    && o.getEncounter().getVisit().getStartDatetime().before(toDate)
                    && Objects.equals(o.getEncounter().getVisit().getVisitType().getName(), OPD)
                    && Objects.equals(o.getConcept().getName().getName(), CHIEF_COMPLAINT)
                    && o.getValueCoded() != null
                    && o.getConcept().getName().getLocalePreferred()
            )
            {
                chiefComplaintObsMap.add(o);
            }
        }
        return chiefComplaintObsMap;
    }
}
