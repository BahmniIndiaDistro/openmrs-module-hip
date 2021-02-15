package org.bahmni.module.hip.web.service;

import org.apache.log4j.Logger;
import org.bahmni.module.hip.web.model.DateRange;
import org.bahmni.module.hip.web.model.DiagnosticReportBundle;
import org.bahmni.module.hip.web.model.DrugOrders;
import org.bahmni.module.hip.web.model.OpenMrsPrescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DiagnosticReportService {
    private static final Logger log = Logger.getLogger(PrescriptionService.class);

    private final OpenMRSDrugOrderClient openMRSDrugOrderClient;
    private final FhirBundledDiagnosticReportBuilder fhirBundledDiagnosticReportBuilder;

    @Autowired
    public DiagnosticReportService(OpenMRSDrugOrderClient openMRSDrugOrderClient, FhirBundledDiagnosticReportBuilder fhirBundledDiagnosticReportBuilder) {
        this.openMRSDrugOrderClient = openMRSDrugOrderClient;
        this.fhirBundledDiagnosticReportBuilder = fhirBundledDiagnosticReportBuilder;
    }

    public List<DiagnosticReportBundle> getDiagnosticReports(String patientIdUuid, DateRange dateRange, String visitType) {
        DrugOrders drugOrders = new DrugOrders(openMRSDrugOrderClient.getDrugOrdersByDateAndVisitTypeFor(patientIdUuid, dateRange, visitType));

        if (drugOrders.isEmpty())
            return new ArrayList<>();

        List<OpenMrsPrescription> openMrsPrescriptions = OpenMrsPrescription
                .from(drugOrders.groupByEncounter());

        return openMrsPrescriptions
                .stream()
                .map(fhirBundledDiagnosticReportBuilder::fhirBundleResponseFor)
                .collect(Collectors.toList());
    }
}
