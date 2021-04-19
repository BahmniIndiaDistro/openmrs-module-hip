package org.bahmni.module.hip.web.controller;

import org.bahmni.module.hip.web.client.ClientError;
import org.bahmni.module.hip.web.service.ExistingPatientService;
import org.json.JSONObject;
import org.openmrs.Patient;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/hip")
@RestController
public class PatientController {
    private final ExistingPatientService existingPatientService;

    @Autowired
    public PatientController(ExistingPatientService existingPatientService) {
        this.existingPatientService = existingPatientService;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/existingPatients")
    public @ResponseBody
    ResponseEntity<Object> getExistingPatients(@RequestParam(required = false) String patientName, int patientYearOfBirth,
                                               String patientGender) {

        List<Patient> matchingPatients = existingPatientService.filterMatchingPatients(patientName,
                patientYearOfBirth, patientGender);

        if (matchingPatients.size() != 1)
            return ResponseEntity.badRequest().body(ClientError.noPatientFound());
        else {
            JSONObject existingPatientsListObject = existingPatientService.getMatchingPatientDetails(matchingPatients);
            return ResponseEntity.status(HttpStatus.OK).body(existingPatientsListObject.toString());
        }
    }
}
