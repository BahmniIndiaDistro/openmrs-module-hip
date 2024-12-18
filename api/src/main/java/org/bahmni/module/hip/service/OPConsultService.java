package org.bahmni.module.hip.service;
import org.bahmni.module.hip.model.OPConsultBundle;
import org.bahmni.module.hip.model.DateRange;
import org.bahmni.module.hip.model.OpenMrsCondition;
import org.bahmni.module.hip.model.DrugOrders;
import org.bahmni.module.hip.model.OpenMrsOPConsult;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OPConsultService {

    private final FhirBundledOPConsultBuilder fhirBundledOPConsultBuilder;
    private final PatientService patientService;
    private final OpenMRSDrugOrderClient openMRSDrugOrderClient;
    private final ConsultationService consultationService;
    private final VisitService visitService;

    @Autowired
    public OPConsultService(FhirBundledOPConsultBuilder fhirBundledOPConsultBuilder,
                            PatientService patientService,
                            OpenMRSDrugOrderClient openMRSDrugOrderClient,
                            ConsultationService consultationService, VisitService visitService) {
        this.fhirBundledOPConsultBuilder = fhirBundledOPConsultBuilder;
        this.patientService = patientService;
        this.openMRSDrugOrderClient = openMRSDrugOrderClient;
        this.consultationService = consultationService;
        this.visitService = visitService;
    }

    public List<OPConsultBundle> getOpConsultsForVisit(String patientUuid, String visitUuid, Date fromDate, Date toDate) throws ParseException {
        Visit visit = visitService.getVisitByUuid(visitUuid);


        Patient patient = patientService.getPatientByUuid(patientUuid);
        Map<Encounter, DrugOrders> encounteredDrugOrdersMap = openMRSDrugOrderClient.getDrugOrdersByDateAndVisitTypeFor(visit, fromDate, toDate);
        Map<Encounter, List<OpenMrsCondition>> encounterChiefComplaintsMap = consultationService.getEncounterChiefComplaintsMap(visit, fromDate, toDate);
        Map<Encounter, List<OpenMrsCondition>> encounterMedicalHistoryMap = consultationService.getEncounterMedicalHistoryConditionsMap(visit, fromDate, toDate);
        Map<Encounter, List<Obs>> encounterPhysicalExaminationMap = consultationService.getEncounterPhysicalExaminationMap(visit, fromDate, toDate);
        Map<Encounter, List<Obs>> encounterProcedureMap = consultationService.getEncounterProcedureMap(visit, fromDate, toDate);
        Map<Encounter, List<Obs>> encounterPatientDocumentsMap = consultationService.getEncounterPatientDocumentsMap(visit, fromDate, toDate, AbdmConfig.HiTypeDocumentKind.OP_CONSULT);
        Map<Encounter, List<Order>> encounterOrdersMap = consultationService.getEncounterOrdersMap(visit, fromDate, toDate);
        Map<Encounter, List<Obs>> encounterOtherObsMap = consultationService.getEncounterOtherObsMap(visit, fromDate, toDate, AbdmConfig.OpConsultAttribute.OTHER_OBSERVATIONS);

        List<OpenMrsOPConsult> openMrsOPConsultList = OpenMrsOPConsult.getOpenMrsOPConsultList(encounterChiefComplaintsMap,
                encounterMedicalHistoryMap, encounterPhysicalExaminationMap, encounteredDrugOrdersMap, encounterProcedureMap,
                encounterPatientDocumentsMap, encounterOrdersMap, encounterOtherObsMap, patient);

        return openMrsOPConsultList.stream().
                map(fhirBundledOPConsultBuilder::fhirBundleResponseFor).collect(Collectors.toList());


    }

    public List<OPConsultBundle> getOpConsultsForProgram(String patientUuid, DateRange dateRange, String programName,String programEnrollmentId) {
        Date fromDate = dateRange.getFrom();
        Date toDate = dateRange.getTo();
        Patient patient = patientService.getPatientByUuid(patientUuid);

        Map<Encounter, List<OpenMrsCondition>> encounterChiefComplaintsMap = consultationService. getEncounterChiefComplaintsMapForProgram(programName,fromDate,toDate,patient);
        Map<Encounter, List<OpenMrsCondition>> encounterMedicalHistoryMap = consultationService.getEncounterMedicalHistoryConditionsMapForProgram(programName,fromDate,toDate,patient);
        Map<Encounter, List<Obs>> encounterPhysicalExaminationMap = consultationService.getEncounterPhysicalExaminationMapForProgram(programName,fromDate,toDate,patient);
        Map<Encounter, DrugOrders> encounteredDrugOrdersMap = openMRSDrugOrderClient.getDrugOrdersByDateAndProgramFor(patientUuid, dateRange,programName,programEnrollmentId);
        Map<Encounter, List<Obs>> encounterProcedureMap = consultationService.getEncounterProcedureMapForProgram(programName,fromDate,toDate,patient);
        Map<Encounter, List<Obs>> encounterPatientDocumentsMap = consultationService.getEncounterPatientDocumentsMapForProgram(programName,fromDate,toDate,patient,programEnrollmentId);
        Map<Encounter, List<Order>> encounterOrdersMap = consultationService.getEncounterOrdersMapForProgram(programName,fromDate,toDate,patient);
        Map<Encounter, List<Obs>> encounterOtherObsMap = consultationService.getEncounterOtherObsMapForProgram(programName, fromDate, toDate, patient, AbdmConfig.OpConsultAttribute.OTHER_OBSERVATIONS);

        List<OpenMrsOPConsult> openMrsOPConsultList = OpenMrsOPConsult.getOpenMrsOPConsultList(encounterChiefComplaintsMap,
                encounterMedicalHistoryMap, encounterPhysicalExaminationMap, encounteredDrugOrdersMap, encounterProcedureMap,
                encounterPatientDocumentsMap, encounterOrdersMap, encounterOtherObsMap, patient);

        return openMrsOPConsultList.stream().
                map(fhirBundledOPConsultBuilder::fhirBundleResponseFor).collect(Collectors.toList());
    }


}
