package org.bahmni.module.hip.web.controller;

import org.bahmni.module.hip.web.client.ClientError;
import org.bahmni.module.hip.web.model.BundledOPConsultResponse;
import org.bahmni.module.hip.web.model.DateRange;
import org.bahmni.module.hip.web.model.OPConsultBundle;
import org.bahmni.module.hip.web.service.OPConsultService;
import org.bahmni.module.hip.web.service.ValidationService;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.List;

import static org.bahmni.module.hip.web.utils.DateUtils.parseDate;

@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/hip/opConsults")
@RestController
public class OPConsultController extends BaseRestController {
    private final OPConsultService opConsultService;
    private final ValidationService validationService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public OPConsultController(OPConsultService opConsultService, ValidationService validationService) {
        this.opConsultService = opConsultService;
        this.validationService = validationService;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/visit", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<?> getOpconsultForVisit(@RequestParam String patientId,
                          @RequestParam String visitUuid,
                          @RequestParam String fromDate,
                          @RequestParam String toDate) throws ParseException, IOException {
        if (patientId == null || patientId.isEmpty())
            return ResponseEntity.badRequest().body(ClientError.noPatientIdProvided());
        if (visitUuid == null || visitUuid.isEmpty())
            return ResponseEntity.badRequest().body(ClientError.noVisitUuidProvided());
        if (!validationService.isValidVisit(visitUuid))
            return ResponseEntity.badRequest().body(ClientError.invalidVisitUuid());
        if (!validationService.isValidPatient(patientId))
            return ResponseEntity.badRequest().body(ClientError.invalidPatientId());
        List<OPConsultBundle> opConsultBundle = opConsultService.getOpConsultsForVisit(patientId,visitUuid, fromDate, toDate);


        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(mapper.writeValueAsString(new BundledOPConsultResponse(opConsultBundle)));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/program", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<?> getOpConsultForProgram(
            @RequestParam String patientId,
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam String programName,
            @RequestParam String programEnrollmentId)
            throws ParseException, IOException {
        programName = URLDecoder.decode(programName, "UTF-8");
        if (patientId == null || patientId.isEmpty())
            return ResponseEntity.badRequest().body(ClientError.noPatientIdProvided());
        if (programName == null || programName.isEmpty())
            return ResponseEntity.badRequest().body(ClientError.noProgramNameProvided());
        if (!validationService.isValidProgram(programName))
            return ResponseEntity.badRequest().body(ClientError.invalidProgramName());
        if (!validationService.isValidPatient(patientId))
            return ResponseEntity.badRequest().body(ClientError.invalidPatientId());
        List<OPConsultBundle> opConsultBundle =
                opConsultService.getOpConsultsForProgram(patientId, new DateRange(parseDate(fromDate), parseDate(toDate)), programName, programEnrollmentId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(mapper.writeValueAsString(new BundledOPConsultResponse(opConsultBundle)));
    }
}
