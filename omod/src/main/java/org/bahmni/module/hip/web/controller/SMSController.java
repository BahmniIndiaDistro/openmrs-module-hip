package org.bahmni.module.hip.web.controller;

import org.bahmni.module.hip.notification.SMSSender;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/send")
public class SMSController {
    private final SMSSender smsSender;

    @Autowired
    public SMSController(SMSSender smsSender){
        this.smsSender = smsSender;
    }


    @RequestMapping(method = RequestMethod.POST, value = "/sms")
    public @ResponseBody
    ResponseEntity.BodyBuilder sendSMS(@Valid @RequestBody String phoneNumber, String message)  {
        smsSender.send("Bahmni",phoneNumber,message);
        return ResponseEntity.ok();
    }
}
