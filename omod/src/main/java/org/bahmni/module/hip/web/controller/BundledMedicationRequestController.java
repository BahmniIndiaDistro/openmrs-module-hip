package org.bahmni.module.hip.web.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bahmni.module.hip.web.exception.RequestParameterMissingException;
import org.bahmni.module.hip.web.models.ErrorResponse;
import org.bahmni.module.hip.web.service.BundleMedicationRequestService;
import org.hl7.fhir.r4.model.Bundle;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Validated
@RestController
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/hip")
public class BundledMedicationRequestController {

    private BundleMedicationRequestService bundleMedicationRequestService;

    @Autowired
    public BundledMedicationRequestController(BundleMedicationRequestService bundleMedicationRequestService) {
        this.bundleMedicationRequestService = bundleMedicationRequestService;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/medication", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<String> getBundledMedicationRequestFor(@RequestParam(required = false) String patientId,
                                                          @RequestParam(required = false) String visitType) {

        if (patientId == null || patientId.equals("''"))
            throw new RequestParameterMissingException("patientId");

        if (visitType == null || visitType.equals("''"))
            throw new RequestParameterMissingException("visitType");

        Bundle bundle = bundleMedicationRequestService.bundleMedicationRequestsFor(patientId, visitType);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(serializeBundle(bundle));
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(RequestParameterMissingException.class)
    public String missingRequestParameter(HttpServletRequest req, Exception ex) throws JsonProcessingException {
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage());
        return  new ObjectMapper().writeValueAsString(errorResponse);
    }

    private String serializeBundle(Bundle bundle) {
        FhirContext ctx = FhirContext.forR4();
        IParser parser = ctx.newJsonParser();
        return parser.encodeResourceToString(bundle);
    }
}