package org.bahmni.module.hip.web.service;

import org.apache.log4j.Logger;
import org.bahmni.module.hip.web.model.DateRange;
import org.bahmni.module.hip.web.model.DiagnosticReportBundle;
import org.bahmni.module.hip.web.model.OpenMrsPrescription;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.parameter.EncounterSearchCriteria;
import org.openmrs.parameter.EncounterSearchCriteriaBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DiagnosticReportService {
    private static final Logger log = Logger.getLogger(PrescriptionService.class);

    private final OpenMRSDrugOrderClient openMRSDrugOrderClient;
    private final FhirBundledDiagnosticReportBuilder fhirBundledDiagnosticReportBuilder;
    private final PatientService patientService;
    private final EncounterService encounterService;



    @Autowired
    public DiagnosticReportService(OpenMRSDrugOrderClient openMRSDrugOrderClient,
                                   FhirBundledDiagnosticReportBuilder fhirBundledDiagnosticReportBuilder,
                                   PatientService patientService,
                                   EncounterService encounterService) {
        this.openMRSDrugOrderClient = openMRSDrugOrderClient;
        this.fhirBundledDiagnosticReportBuilder = fhirBundledDiagnosticReportBuilder;
        this.patientService = patientService;
        this.encounterService = encounterService;
    }

    public List<DiagnosticReportBundle> getDiagnosticReports(String patientUuid, DateRange dateRange, String visitType) {

        Date fromDate = dateRange.getFrom();
        Date toDate = dateRange.getTo();

        Patient patient = patientService.getPatientByUuid(patientUuid);

        EncounterType encounter_Type = encounterService.getEncounterType("RADIOLOGY");

        HashMap<Encounter, List<Obs>> encounterListMap = new HashMap<>();

        if (patient != null) {
            EncounterSearchCriteriaBuilder encounterSearchCriteriaBuilder = new EncounterSearchCriteriaBuilder()
                    .setPatient(patient).setFromDate(fromDate).
                    setToDate(toDate).setIncludeVoided(false);
            if (encounter_Type != null) {
                encounterSearchCriteriaBuilder.setEncounterTypes(Collections.singletonList(encounter_Type));
            }

            EncounterSearchCriteria encounterSearchCriteria = encounterSearchCriteriaBuilder.createEncounterSearchCriteria();

            List<Encounter> encounters = encounterService.getEncounters(encounterSearchCriteria);

            for (Encounter e : encounters) {
                encounterListMap.put(e, new ArrayList<>(e.getAllObs()));
            }
        }
        List<OpenMrsPrescription> openMrsPrescriptions = OpenMrsPrescription.fromDiagnosticReport(encounterListMap);

        return openMrsPrescriptions
                .stream()
                .map(fhirBundledDiagnosticReportBuilder::fhirBundleResponseFor)
                .collect(Collectors.toList());
    }
}
