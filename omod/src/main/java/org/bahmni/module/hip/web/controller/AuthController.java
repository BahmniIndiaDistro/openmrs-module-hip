package org.bahmni.module.hip.web.controller;

import org.bahmni.module.hip.web.service.AuthService;
import org.checkerframework.checker.units.qual.A;
import org.jcodec.common.logging.Logger;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final AuthService authService ;

    @Autowired
    public AuthController(AuthService authService){
        this.authService = authService;
    }


    @Authorized({ PrivilegeConstants.GET_IDENTIFIER_TYPES })
    @RequestMapping(method = RequestMethod.GET, value = "/authValidation", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<?> getExistingPatients(){
        authService.AuthTest();
        Logger.info("Reached OMOD");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(" ");
    }
}
