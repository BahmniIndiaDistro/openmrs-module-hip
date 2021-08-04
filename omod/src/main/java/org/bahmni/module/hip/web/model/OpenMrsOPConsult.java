package org.bahmni.module.hip.web.model;

import lombok.Getter;
import org.openmrs.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class OpenMrsOPConsult {
    private final Encounter encounter;
    private final List<OpenMrsCondition> chiefComplaintConditions;
    private final List<OpenMrsCondition> medicalHistoryConditions;
    private final List<Obs> observations;
    private final Patient patient;
    private final Set<EncounterProvider> encounterProviders;
    private final List<DrugOrder> drugOrders;
    private final Obs procedure;
    private final List<Obs> patientDocuments;

    public OpenMrsOPConsult(Encounter encounter,
                            List<OpenMrsCondition> chiefComplaintConditions,
                            List<OpenMrsCondition> medicalHistoryConditions,
                            List<Obs> observations,
                            Patient patient,
                            Set<EncounterProvider> encounterProviders,
                            List<DrugOrder> drugOrders, Obs procedure, List<Obs> patientDocuments) {
        this.encounter = encounter;
        this.chiefComplaintConditions = chiefComplaintConditions;
        this.medicalHistoryConditions = medicalHistoryConditions;
        this.observations = observations;
        this.patient = patient;
        this.encounterProviders = encounterProviders;
        this.drugOrders = drugOrders;
        this.procedure = procedure;
        this.patientDocuments = patientDocuments;
    }

    public static List<OpenMrsOPConsult> getOpenMrsOPConsultList(Map<Encounter, List<OpenMrsCondition>> encounterChiefComplaintsMap,
                                                                 Map<Encounter, List<OpenMrsCondition>> encounterMedicalHistoryMap,
                                                                 Map<Encounter, List<Obs>> encounterPhysicalExaminationMap,
                                                                 Map<Encounter, DrugOrders> encounteredDrugOrdersMap,
                                                                 Map<Encounter, Obs> encounterProcedureMap,
                                                                 Map<Encounter, List<Obs>> encounterDiagnosticReportsMap,
                                                                 Patient patient) {
        List<OpenMrsOPConsult> openMrsOPConsultList = new ArrayList<>();

        for(Map.Entry<Encounter, DrugOrders> entry : encounteredDrugOrdersMap.entrySet()){
            List<DrugOrder> drugOrdersList = encounteredDrugOrdersMap.get(entry.getKey()).getOpenMRSDrugOrders();
            List<OpenMrsCondition> chiefComplaintList = new ArrayList<>();
            List<OpenMrsCondition> medicalHistoryList = new ArrayList<>();
            List<Obs> physicalExaminationList = new ArrayList<>();
            Obs procedure = null;
            List<Obs> diagnosticReportsList = new ArrayList<>();

            if (encounterDiagnosticReportsMap.containsKey(entry.getKey())) {
                diagnosticReportsList.addAll(encounterDiagnosticReportsMap.get(entry.getKey()));
                encounterDiagnosticReportsMap.remove(entry.getKey());
            }
            if (encounterMedicalHistoryMap.containsKey(entry.getKey())) {
                medicalHistoryList = encounterMedicalHistoryMap.get(entry.getKey());
                encounterMedicalHistoryMap.remove(entry.getKey());
            }
            if (encounterPhysicalExaminationMap.containsKey(entry.getKey())) {
                physicalExaminationList = encounterPhysicalExaminationMap.get(entry.getKey());
                encounterPhysicalExaminationMap.remove(entry.getKey());
            }
            if (encounterChiefComplaintsMap.containsKey(entry.getKey())) {
                chiefComplaintList = encounterChiefComplaintsMap.get(entry.getKey());
                encounterChiefComplaintsMap.remove(entry.getKey());
            }
            if (encounterProcedureMap.containsKey(entry.getKey())) {
                procedure = encounterProcedureMap.get(entry.getKey());
                encounterProcedureMap.remove(entry.getKey());
            }
            openMrsOPConsultList.add(new OpenMrsOPConsult(entry.getKey(), chiefComplaintList, medicalHistoryList, physicalExaminationList, patient, entry.getKey().getEncounterProviders(), drugOrdersList, procedure, diagnosticReportsList));
        }

        for (Map.Entry<Encounter, List<OpenMrsCondition>> entry : encounterChiefComplaintsMap.entrySet()) {
            List<OpenMrsCondition> chiefComplaintList = encounterChiefComplaintsMap.get(entry.getKey());
            List<OpenMrsCondition> medicalHistoryList = new ArrayList<>();
            List<Obs> physicalExaminationList = new ArrayList<>();
            List<DrugOrder> drugOrdersList = new ArrayList<>();
            Obs procedure = null;
            List<Obs> diagnosticReportsList = new ArrayList<>();

            if (encounterDiagnosticReportsMap.containsKey(entry.getKey())) {
                diagnosticReportsList.addAll(encounterDiagnosticReportsMap.get(entry.getKey()));
                encounterDiagnosticReportsMap.remove(entry.getKey());
            }
            if (encounterMedicalHistoryMap.containsKey(entry.getKey())) {
                medicalHistoryList = encounterMedicalHistoryMap.get(entry.getKey());
                encounterMedicalHistoryMap.remove(entry.getKey());
            }
            if (encounterPhysicalExaminationMap.containsKey(entry.getKey())) {
                physicalExaminationList = encounterPhysicalExaminationMap.get(entry.getKey());
                encounterPhysicalExaminationMap.remove(entry.getKey());
            }
            if (encounteredDrugOrdersMap.containsKey(entry.getKey())) {
                drugOrdersList = encounteredDrugOrdersMap.get(entry.getKey()).getOpenMRSDrugOrders();
                encounteredDrugOrdersMap.remove(entry.getKey());
            }
            if (encounterProcedureMap.containsKey(entry.getKey())) {
                procedure = encounterProcedureMap.get(entry.getKey());
                encounterProcedureMap.remove(entry.getKey());
            }

            openMrsOPConsultList.add(new OpenMrsOPConsult(entry.getKey(), chiefComplaintList, medicalHistoryList, physicalExaminationList, patient, entry.getKey().getEncounterProviders(), drugOrdersList, procedure, diagnosticReportsList));
        }

        for (Map.Entry<Encounter, List<OpenMrsCondition>> entry : encounterMedicalHistoryMap.entrySet()) {
            List<OpenMrsCondition> chiefComplaintList = new ArrayList<>();
            List<OpenMrsCondition> medicalHistoryList = encounterMedicalHistoryMap.get(entry.getKey());
            List<Obs> physicalExaminationList = new ArrayList<>();
            List<DrugOrder> drugOrdersList = new ArrayList<>();
            Obs procedure = null;
            List<Obs> diagnosticReportsList = new ArrayList<>();

            if (encounterDiagnosticReportsMap.containsKey(entry.getKey())) {
                diagnosticReportsList.addAll(encounterDiagnosticReportsMap.get(entry.getKey()));
                encounterDiagnosticReportsMap.remove(entry.getKey());
            }
            if (encounterChiefComplaintsMap.containsKey(entry.getKey())) {
                chiefComplaintList = encounterChiefComplaintsMap.get(entry.getKey());
                encounterChiefComplaintsMap.remove(entry.getKey());
            }
            if (encounterPhysicalExaminationMap.containsKey(entry.getKey())) {
                physicalExaminationList = encounterPhysicalExaminationMap.get(entry.getKey());
                encounterPhysicalExaminationMap.remove(entry.getKey());
            }
            if (encounteredDrugOrdersMap.containsKey(entry.getKey())) {
                drugOrdersList = encounteredDrugOrdersMap.get(entry.getKey()).getOpenMRSDrugOrders();
                encounteredDrugOrdersMap.remove(entry.getKey());
            }
            if (encounterProcedureMap.containsKey(entry.getKey())) {
                procedure = encounterProcedureMap.get(entry.getKey());
                encounterProcedureMap.remove(entry.getKey());
            }
            openMrsOPConsultList.add(new OpenMrsOPConsult(entry.getKey(), chiefComplaintList, medicalHistoryList, physicalExaminationList, patient, entry.getKey().getEncounterProviders(), drugOrdersList, procedure, diagnosticReportsList));
        }

        for (Map.Entry<Encounter, List<Obs>> entry : encounterPhysicalExaminationMap.entrySet()) {
            List<OpenMrsCondition> chiefComplaintList = new ArrayList<>();
            List<OpenMrsCondition> medicalHistoryList = new ArrayList<>();
            List<Obs> physicalExaminationList = encounterPhysicalExaminationMap.get(entry.getKey());
            List<DrugOrder> drugOrdersList = new ArrayList<>();
            Obs procedure = null;
            List<Obs> diagnosticReportsList = new ArrayList<>();

            if (encounterDiagnosticReportsMap.containsKey(entry.getKey())) {
                diagnosticReportsList.addAll(encounterDiagnosticReportsMap.get(entry.getKey()));
                encounterDiagnosticReportsMap.remove(entry.getKey());
            }
            if (encounterChiefComplaintsMap.containsKey(entry.getKey())) {
                chiefComplaintList = encounterChiefComplaintsMap.get(entry.getKey());
                encounterChiefComplaintsMap.remove(entry.getKey());
            }
            if (encounterMedicalHistoryMap.containsKey(entry.getKey())) {
                medicalHistoryList = encounterMedicalHistoryMap.get(entry.getKey());
                encounterMedicalHistoryMap.remove(entry.getKey());
            }
            if (encounteredDrugOrdersMap.containsKey(entry.getKey())) {
                drugOrdersList = encounteredDrugOrdersMap.get(entry.getKey()).getOpenMRSDrugOrders();
                encounteredDrugOrdersMap.remove(entry.getKey());
            }
            if (encounterProcedureMap.containsKey(entry.getKey())) {
                procedure = encounterProcedureMap.get(entry.getKey());
                encounterProcedureMap.remove(entry.getKey());
            }
            openMrsOPConsultList.add(new OpenMrsOPConsult(entry.getKey(), chiefComplaintList, medicalHistoryList, physicalExaminationList, patient, entry.getKey().getEncounterProviders(), drugOrdersList, procedure, diagnosticReportsList));
        }

        for (Map.Entry<Encounter, Obs> entry : encounterProcedureMap.entrySet()) {
            List<Obs> diagnosticReportsList = new ArrayList<>();
            if (encounterDiagnosticReportsMap.containsKey(entry.getKey())) {
                diagnosticReportsList.addAll(encounterDiagnosticReportsMap.get(entry.getKey()));
                encounterDiagnosticReportsMap.remove(entry.getKey());
            }
            openMrsOPConsultList.add(new OpenMrsOPConsult(entry.getKey(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), patient, entry.getKey().getEncounterProviders(), new ArrayList<>(), entry.getValue(), diagnosticReportsList));
        }

        for (Map.Entry<Encounter, List<Obs>> entry : encounterDiagnosticReportsMap.entrySet()) {
            openMrsOPConsultList.add(new OpenMrsOPConsult(entry.getKey(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), patient, entry.getKey().getEncounterProviders(), new ArrayList<>(), null, encounterDiagnosticReportsMap.get(entry.getKey())));
        }
        return openMrsOPConsultList;
    }

    @Override
    public String toString() {
        return "OpenMrsOPConsult{" +
                "encounter=" + encounter +
                ", chiefComplaintConditions=" + chiefComplaintConditions +
                ", medicalHistoryConditions=" + medicalHistoryConditions +
                ", observations=" + observations +
                ", patient=" + patient +
                ", encounterProviders=" + encounterProviders +
                '}';
    }
}
