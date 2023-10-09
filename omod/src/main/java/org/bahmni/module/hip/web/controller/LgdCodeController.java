package org.bahmni.module.hip.web.controller;

import org.bahmni.module.hip.web.exception.LGDCodeNotFoundException;
import org.bahmni.module.hip.web.service.LgdCodeService;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/hip")
@RestController
public class LgdCodeController {
    private final LgdCodeService lgdCodeService;

    @Autowired
    public LgdCodeController(LgdCodeService lgdCodeService) {
        this.lgdCodeService = lgdCodeService;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/lgdCode", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<?> getExistingPatients(@RequestParam String state,
                                          @RequestParam String district) throws LGDCodeNotFoundException {
        Map<String,Integer> lgdCodes = lgdCodeService.getLGDCode(state, district);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(lgdCodes);
    }
}
