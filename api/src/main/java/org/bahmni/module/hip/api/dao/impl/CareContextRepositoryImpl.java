package org.bahmni.module.hip.api.dao.impl;

import org.bahmni.module.hip.api.dao.CareContextRepository;
import org.bahmni.module.hip.api.dao.EncounterDao;
import org.bahmni.module.hip.model.HiType;
import org.bahmni.module.hip.model.PatientCareContext;
import org.bahmni.module.hip.service.*;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.Visit;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.VisitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.bahmni.module.hip.api.dao.Constants.PROGRAM;
import static org.bahmni.module.hip.api.dao.Constants.VISIT_TYPE;

@Repository
public class CareContextRepositoryImpl implements CareContextRepository {
    private SessionFactory sessionFactory;
    private PatientService  patientService;
    private VisitService visitService;
    private ProgramWorkflowService programWorkflowService;
    private  EncounterDao encounterDao;

    private PrescriptionService prescriptionService;

    private DiagnosticReportService diagnosticReportService;

    private ImmunizationRecordService immunizationRecordService;

    private OPConsultService opConsultService;

    private DischargeSummaryService dischargeSummaryService;

    private HealthDocumentRecordService healthDocumentRecordService;

    private WellnessRecordService wellnessRecordService;


    @Autowired
    public CareContextRepositoryImpl(SessionFactory sessionFactory, PatientService patientService,
                                     VisitService visitService, ProgramWorkflowService programWorkflowService,
                                     EncounterDao encounterDao, PrescriptionService prescriptionService, DiagnosticReportService diagnosticReportService,
                                     ImmunizationRecordService immunizationRecordService, OPConsultService opConsultService,
                                     DischargeSummaryService dischargeSummaryService, HealthDocumentRecordService healthDocumentRecordService,
                                     WellnessRecordService wellnessRecordService) {
        this.sessionFactory = sessionFactory;
        this.patientService = patientService;
        this.visitService = visitService;
        this.programWorkflowService = programWorkflowService;
        this.encounterDao = encounterDao;
        this.prescriptionService = prescriptionService;
        this.diagnosticReportService = diagnosticReportService;
        this.immunizationRecordService = immunizationRecordService;
        this.opConsultService = opConsultService;
        this.dischargeSummaryService = dischargeSummaryService;
        this.healthDocumentRecordService = healthDocumentRecordService;
        this.wellnessRecordService = wellnessRecordService;

    }

    @Override
    public List<PatientCareContext> getPatientCareContext(String patientUuid) {
        List<PatientCareContext> careContexts = new ArrayList<>();
        Patient patient = patientService.getPatientByUuid(patientUuid);
        List<Visit> visits = getAllVisitForPatient(patient);
        List<PatientProgram> patientPrograms = getAllPrograms(patient);
        for (Visit visit: visits) {
            careContexts.add(getPatientCareContext(visit));
        }
        for (PatientProgram program: patientPrograms) {
            careContexts.add(getPatientCareContext(program));
        }
        return careContexts;
    }


    @Override
    public List<PatientCareContext> getNewPatientCareContext(Patient patient) {
        List<PatientCareContext> careContexts = new ArrayList<>();
        List<Visit> visits = getAllVisitForPatient(patient);
        List<PatientProgram> patientPrograms = getAllPrograms(patient);
        Visit visit = !visits.isEmpty() ? visits.get(0) : null;
        PatientProgram program = !patientPrograms.isEmpty() ? patientPrograms.get(0) : null;
        if(visit == null && program != null)
            careContexts.add(getPatientCareContext(program));
        else if(visit != null && program == null)
            careContexts.add(getPatientCareContext(visit));
        else if(visit != null && program != null) {
            if (program.getDateCreated().before(visit.getStartDatetime()))
                careContexts.add(getPatientCareContext(visit));
            else
                careContexts.add(getPatientCareContext(program));
        }
        return careContexts;
    }

    private PatientCareContext getPatientCareContext(Visit visit) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        return new PatientCareContext(VISIT_TYPE,
                "Visit on ".concat(dateFormat.format(visit.getStartDatetime()))
                        .concat(" with ").concat(visit.getCreator().getPersonName().getFullName()),
                VISIT_TYPE.concat(":").concat(visit.getUuid()), getHiTypesForVisit(visit));
    }

    private List<HiType> getHiTypesForVisit(Visit visit) {
        List<HiType> hiTypes = new ArrayList<>();
        String visitUuid = visit.getUuid();
        String patientUuid = visit.getPatient().getUuid();
        Date visitStartDate = visit.getStartDatetime();
        Date currentDate = new Date();
        try {
            if(!prescriptionService.getPrescriptions(patientUuid, visitUuid, visitStartDate, currentDate).isEmpty()){
                hiTypes.add(HiType.Prescription);
            }

            if (!diagnosticReportService.getDiagnosticReportsForVisit(patientUuid, visitUuid, visitStartDate, currentDate).isEmpty() ||
                    !diagnosticReportService.getLabResultsForVisits(patientUuid, visitUuid, new SimpleDateFormat("yyyy-MM-dd").format(visitStartDate), new SimpleDateFormat("yyyy-MM-dd").format(currentDate)).isEmpty()) {
                hiTypes.add(HiType.DiagnosticReport);
            }

            if (!immunizationRecordService.getImmunizationRecordsForVisit(patientUuid, visitUuid, visitStartDate, currentDate).isEmpty()) {
                hiTypes.add(HiType.ImmunizationRecord);
            }
            if (!opConsultService.getOpConsultsForVisit(patientUuid, visitUuid, visitStartDate, currentDate).isEmpty()) {
                hiTypes.add(HiType.OPConsultation);
            }
            if (!dischargeSummaryService.getDischargeSummaryForVisit(patientUuid, visitUuid, visitStartDate, currentDate).isEmpty()) {
                hiTypes.add(HiType.DischargeSummary);
            }
            if (!healthDocumentRecordService.getDocumentsForVisit(patientUuid, visitUuid, visitStartDate, currentDate).isEmpty()) {
                hiTypes.add(HiType.HealthDocumentRecord);
            }
            if (!wellnessRecordService.getWellnessForVisit(patientUuid, visitUuid, visitStartDate, currentDate).isEmpty()) {
                hiTypes.add(HiType.WellnessRecord);
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return hiTypes;
    }

    @Override
    public List<PatientCareContext> getPatientCareContextByVisitUuid(String visitUuid) {
        Visit visit = visitService.getVisitByUuid(visitUuid);
        List<PatientCareContext> careContexts = new ArrayList<>();
        careContexts.add(getPatientCareContext(visit));
        return careContexts;
    }
    private PatientCareContext getPatientCareContext(PatientProgram program) {
        return new PatientCareContext(PROGRAM,
                program.getProgram().getName(),
                getProgramEnrollementId(program.getPatientProgramId()).get(0), Arrays.asList(HiType.values()));
    }

    private List<Integer> getEpisodeIds() {
        Query query = this.sessionFactory.getCurrentSession().createSQLQuery("select\n" +
                "\t\tepisode_id\n" +
                "\tfrom\n" +
                "\t\tepisode_encounter\n");
        return query.list();
    }

    private List<String> getProgramEnrollementId(Integer patientProgramId) {
        Query query = this.sessionFactory.getCurrentSession().createSQLQuery("SELECT\n" +
                "    value_reference FROM patient_program_attribute WHERE patient_program_id = :patientProgramId\n");
        query.setParameter("patientProgramId", patientProgramId);
        return query.list();
    }

    private List<Visit> getAllVisitForPatient(Patient patient){
        List<Visit> visits = new ArrayList<>();
        for (Visit visit: visitService.getVisitsByPatient(patient)) {
            Set<Encounter> encounters = visit.getEncounters().stream()
                    .filter(encounter -> !encounterDao.getEpisodeEncounterIds().contains(encounter.getEncounterId()))
                    .collect(Collectors.toSet());
            if(!encounters.isEmpty())
                visits.add(visit);
        }
        return visits;
    }

    private List<PatientProgram> getAllPrograms(Patient patient){
        List<PatientProgram> programs = new ArrayList<>();
        List<Integer> episodeIds = getEpisodeIds();
        Set<PatientProgram> patientPrograms = new HashSet<>(programWorkflowService.getPatientPrograms(patient, null, null, null, null, null, false));
        for (PatientProgram program: patientPrograms) {
            if(episodeIds.contains(program.getId()))
                programs.add(program);
        }
        return programs;
    }

}
