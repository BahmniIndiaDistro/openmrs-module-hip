package org.bahmni.module.hip.web.service;

import org.bahmni.module.hip.api.dao.OPConsultDao;
import org.bahmni.module.hip.web.model.DateRange;
import org.bahmni.module.hip.web.model.OPConsultBundle;
import org.bahmni.module.hip.web.model.OpenMrsChiefComplaint;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OPConsultService {
    private final FhirBundledOPConsultBuilder fhirBundledOPConsultBuilder;
    private final OPConsultDao opConsultDao;
    private final PatientService patientService;
    private final EncounterService encounterService;
    private final ObsService obsService;

    @Autowired
    public OPConsultService(FhirBundledOPConsultBuilder fhirBundledOPConsultBuilder, OPConsultDao opConsultDao,
                            PatientService patientService, EncounterService encounterService, ObsService obsService) {
        this.fhirBundledOPConsultBuilder = fhirBundledOPConsultBuilder;
        this.opConsultDao = opConsultDao;
        this.patientService = patientService;
        this.encounterService = encounterService;
        this.obsService = obsService;
    }

    public List<OPConsultBundle> getOpConsultsForVisit(String patientUuid, DateRange dateRange, String visitType) {
        Date fromDate = dateRange.getFrom();
        Date toDate = dateRange.getTo();
        Patient patient = patientService.getPatientByUuid(patientUuid);
        List<Integer> obsIds = opConsultDao.getChiefComplaints(patientUuid, visitType, fromDate, toDate);

        List<Obs> obs = obsIds.stream().map(obsService::getObs).collect(Collectors.toList());
        List<OpenMrsChiefComplaint> openMrsChiefComplaints = obs.stream().map(o -> new OpenMrsChiefComplaint(o.getEncounter(), o.getValueCoded().getUuid(),
                o.getValueCoded().getDisplayString(), patient, o.getEncounter().getEncounterProviders())).collect(Collectors.toList());

        return openMrsChiefComplaints.stream().
                map(fhirBundledOPConsultBuilder::fhirBundleResponseFor).
                collect(Collectors.toList());
    }

}
