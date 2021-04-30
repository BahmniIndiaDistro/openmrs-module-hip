package org.bahmni.module.hip.web.controller;

import org.jcodec.common.logging.Logger;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/hip")
@RestController
public class AuthController extends BaseRestController {


    @RequestMapping(method = RequestMethod.GET, value = "/authValidation", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<?> getExistingPatients(){
        Logger.info("Reached OMOD");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(" ");
    }
}
