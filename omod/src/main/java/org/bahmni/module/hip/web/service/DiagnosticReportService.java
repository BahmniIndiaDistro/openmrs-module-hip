package org.bahmni.module.hip.web.service;

import org.bahmni.module.hip.api.dao.EncounterDao;
import org.bahmni.module.hip.web.model.DateRange;
import org.bahmni.module.hip.web.model.DiagnosticReportBundle;
import org.bahmni.module.hip.web.model.OpenMrsPrescription;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.VisitType;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.openmrs.parameter.EncounterSearchCriteria;
import org.openmrs.parameter.EncounterSearchCriteriaBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DiagnosticReportService {
    private final FhirBundledDiagnosticReportBuilder fhirBundledDiagnosticReportBuilder;
    private final PatientService patientService;
    private final EncounterService encounterService;
    private final VisitService visitService;
    private final EncounterDao encounterDao;

    @Autowired
    public DiagnosticReportService(FhirBundledDiagnosticReportBuilder fhirBundledDiagnosticReportBuilder,
                                   PatientService patientService,
                                   EncounterService encounterService,
                                   VisitService visitService,
                                   EncounterDao encounterDao) {
        this.fhirBundledDiagnosticReportBuilder = fhirBundledDiagnosticReportBuilder;
        this.patientService = patientService;
        this.encounterService = encounterService;
        this.visitService = visitService;
        this.encounterDao = encounterDao;
    }

    public List<DiagnosticReportBundle> getDiagnosticReportsForVisit(String patientUuid, DateRange dateRange, String visitType) {

        Date fromDate = dateRange.getFrom();
        Date toDate = dateRange.getTo();

        Patient patient = patientService.getPatientByUuid(patientUuid);

        List<EncounterType> encounterTypes = new ArrayList<>();
        encounterTypes.add(encounterService.getEncounterType("RADIOLOGY"));
        encounterTypes.add(encounterService.getEncounterType("Patient Document"));

        HashMap<Encounter, List<Obs>> encounterListMap = getAllObservations(fromDate, toDate, patient, encounterTypes, visitType);
        List<OpenMrsPrescription> openMrsPrescriptions = OpenMrsPrescription.fromDiagnosticReport(encounterListMap);

        return openMrsPrescriptions
                .stream()
                .map(fhirBundledDiagnosticReportBuilder::fhirBundleResponseFor)
                .collect(Collectors.toList());
    }

    private HashMap<Encounter, List<Obs>> getAllObservations(Date fromDate, Date toDate,
                                                             Patient patient,
                                                             List<EncounterType> encounterTypes,
                                                             String visitType) {
        HashMap<Encounter, List<Obs>> encounterListMap = new HashMap<>();
        VisitType visitTypeFromService = new VisitType();
        List<VisitType> visitTypes = visitService.getAllVisitTypes();
        for (VisitType vt : visitTypes) {
            if (vt.getName().toLowerCase().equals(visitType.toLowerCase()))
                visitTypeFromService = vt;
        }

        EncounterSearchCriteriaBuilder encounterSearchCriteriaBuilder = new EncounterSearchCriteriaBuilder()
                .setPatient(patient)
                .setFromDate(fromDate)
                .setToDate(toDate)
                .setIncludeVoided(false)
                .setEncounterTypes(encounterTypes)
                .setVisitTypes(Collections.singletonList(visitTypeFromService));

        EncounterSearchCriteria encounterSearchCriteria = encounterSearchCriteriaBuilder.createEncounterSearchCriteria();
        List<Encounter> encounters = encounterService.getEncounters(encounterSearchCriteria);
        Integer[] encounterIds = encounterDao.GetEncounterIdsForVisit(patient.getUuid(),visitType, fromDate, toDate).toArray(new Integer[0]);
        List<Integer> eIds = Arrays.asList(encounterIds);
        encounters.removeIf(e -> !eIds.contains(e.getId()));

        for (Encounter e : encounters) {
            encounterListMap.put(e, new ArrayList<>(e.getAllObs()));
        }
        return encounterListMap;
    }

    public List<DiagnosticReportBundle> getDiagnosticReportsForProgram(String patientUuid,  DateRange dateRange, String programName, String programEnrollmentId) {
        Date fromDate = dateRange.getFrom();
        Date toDate = dateRange.getTo();

        Patient patient = patientService.getPatientByUuid(patientUuid);

        List<EncounterType> encounterTypes = new ArrayList<>();
        encounterTypes.add(encounterService.getEncounterType("RADIOLOGY"));
        encounterTypes.add(encounterService.getEncounterType("Patient Document"));

        HashMap<Encounter, List<Obs>> encounterListMap = getAllObservationsForPrograms(fromDate, toDate, patient, encounterTypes, programName,programEnrollmentId);
        List<OpenMrsPrescription> openMrsPrescriptions = OpenMrsPrescription.fromDiagnosticReport(encounterListMap);

        return openMrsPrescriptions
                .stream()
                .map(fhirBundledDiagnosticReportBuilder::fhirBundleResponseFor)
                .collect(Collectors.toList());

    }
    private HashMap<Encounter, List<Obs>> getAllObservationsForPrograms(Date fromDate, Date toDate,
                                                             Patient patient,
                                                             List<EncounterType> encounterTypes,
                                                             String programName,
                                                             String programEnrollmentId) {
        HashMap<Encounter, List<Obs>> encounterListMap = new HashMap<>();
        EncounterSearchCriteriaBuilder encounterSearchCriteriaBuilder = new EncounterSearchCriteriaBuilder()
                .setPatient(patient)
                .setFromDate(fromDate)
                .setToDate(toDate)
                .setIncludeVoided(false)
                .setEncounterTypes(encounterTypes);

        EncounterSearchCriteria encounterSearchCriteria = encounterSearchCriteriaBuilder.createEncounterSearchCriteria();
        List<Encounter> encounters = encounterService.getEncounters(encounterSearchCriteria);
        Integer[] encounterIds = encounterDao.GetEncounterIdsForProgramForDiagnosticReport(patient.getUuid(),programName ,
                programEnrollmentId, fromDate, toDate).toArray(new Integer[0]);
        List<Integer> encounterIdsForPrograms = Arrays.asList(encounterIds);
        encounters.removeIf(e -> !encounterIdsForPrograms.contains(e.getId()));

        for (Encounter e : encounters) {
            encounterListMap.put(e, new ArrayList<>(e.getAllObs()));
        }
        return encounterListMap;
    }
}
