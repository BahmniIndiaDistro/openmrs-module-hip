package org.bahmni.module.hip.web.service;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.openmrs.VisitType;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BundleMedicationRequestService {

    private MedicationRequestService medicationRequestService;
    private BundleService bundleService;
    private VisitService visitService;

    @Autowired
    public BundleMedicationRequestService(MedicationRequestService medicationRequestService, BundleService bundleService,VisitService visitService) {
        this.medicationRequestService = medicationRequestService;
        this.bundleService = bundleService;
        this.visitService = visitService;
    }

    public Bundle bundleMedicationRequestsFor(String patientId, String byVisitType) {

        List<MedicationRequest> medicationRequests = medicationRequestService.medicationRequestFor(patientId, byVisitType);

        return bundleService.bundleMedicationRequests(medicationRequests);
    }
    public boolean isValidVisit(String visitType) {
        List<VisitType> visitTypes = visitService.getAllVisitTypes();
        for(VisitType vType:visitTypes){
            if(vType.getName().equals(visitType)){
                return true;
            }
        }
        return false;
    }
}
