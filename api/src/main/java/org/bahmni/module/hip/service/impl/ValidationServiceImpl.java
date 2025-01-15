package org.bahmni.module.hip.service.impl;

import org.bahmni.module.hip.service.ExistingPatientService;
import org.bahmni.module.hip.service.ValidationService;
import org.openmrs.Patient;
import org.openmrs.Program;
import org.openmrs.Visit;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.VisitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("validationService")
public class ValidationServiceImpl implements ValidationService {
    private final VisitService visitService;
    private final PatientService patientService;
    private final ProgramWorkflowService programWorkflowService;
    private final ExistingPatientService existingPatientService;

    @Autowired
    public ValidationServiceImpl(VisitService visitService, PatientService patientService, ProgramWorkflowService programWorkflowService, ExistingPatientService existingPatientService) {
        this.patientService = patientService;
        this.visitService = visitService;
        this.programWorkflowService = programWorkflowService;
        this.existingPatientService = existingPatientService;
    }

    @Override
    public boolean isValidVisit(String visitUuid) {
        Visit visit = visitService.getVisitByUuid(visitUuid);
        if (visit != null)
            return true;
        return false;
    }

    @Override
    public boolean isValidPatient(String pid) {
        Patient patient = patientService.getPatientByUuid(pid);
        return patient != null;
    }

    @Override
    public boolean isValidProgram(String programName) {
        Program program = programWorkflowService.getProgramByName(programName);
        if (program != null)
            return true;
        return false;
    }

    @Override
    public boolean isValidHealthId(String healthId) {
        Patient patient = null;
        try {
            patient = patientService.getPatientByUuid(existingPatientService.getPatientWithHealthId(healthId));
        } catch (NullPointerException ignored) {

        }
        return patient != null;
    }
}
