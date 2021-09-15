package org.bahmni.module.hip.api.dao.impl;

import org.bahmni.module.hip.api.dao.DischargeSummaryDao;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ObsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class DischargeSummaryDaoImpl implements DischargeSummaryDao {

    private final ObsService obsService;

    @Autowired
    public DischargeSummaryDaoImpl(ObsService obsService) {
        this.obsService = obsService;
    }

    private boolean matchesVisitType(String visitType, Obs obs) {
        return obs.getEncounter().getVisit().getVisitType().getName().equals(visitType);
    }

    @Override
    public List<Obs> getCarePlan(Patient patient, String visit, Date fromDate, Date toDate) {
        final String obsName = "Discharge Summary";
        List<Obs> patientObs = obsService.getObservationsByPerson(patient);

        List<Obs> carePlanObs = patientObs.stream().filter(obs -> matchesVisitType(visit, obs))
                .filter(obs -> obs.getEncounter().getVisit().getStartDatetime().after(fromDate))
                .filter(obs -> obs.getEncounter().getVisit().getStartDatetime().before(toDate))
                .filter(obs -> obsName.equals(obs.getConcept().getName().getName()))
                .filter(obs -> obs.getConcept().getName().getLocalePreferred())
                .collect(Collectors.toList());

        return carePlanObs;
    }
}