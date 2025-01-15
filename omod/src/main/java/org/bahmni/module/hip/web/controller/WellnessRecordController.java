package org.bahmni.module.hip.web.controller;

import org.bahmni.module.hip.web.client.ClientError;
import org.bahmni.module.hip.web.model.BundledWellnessResponse;
import org.bahmni.module.hip.model.WellnessRecordBundle;
import org.bahmni.module.hip.service.WellnessRecordService;
import org.bahmni.module.hip.utils.DateUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/hip/wellnessRecord")
@RestController
public class WellnessRecordController {
	private WellnessRecordService wellnessRecordService;
	private final ObjectMapper mapper = new ObjectMapper();
	@Autowired
	public WellnessRecordController (WellnessRecordService wellnessRecordService) {
		this.wellnessRecordService = wellnessRecordService;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/visit", produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody
	ResponseEntity<?> getWellnessForVisit(@RequestParam String patientId,
											   @RequestParam String visitUuid,
											   @RequestParam(required = false) String fromDate,
											   @RequestParam(required = false) String toDate) throws IOException {
		if (visitUuid == null || visitUuid.isEmpty()) {
			return ResponseEntity.badRequest().body(ClientError.noVisitUuidProvided());
		}

		Date fromEncounterDate = null, toEncounterDate = null;
		if (!StringUtils.isEmpty(fromDate)) {
			fromEncounterDate =  DateUtils.validDate(fromDate);
			if (fromEncounterDate == null) {
				return ResponseEntity.badRequest().body(ClientError.invalidStartDate());
			}
		}

		if (!StringUtils.isEmpty(toDate)) {
			toEncounterDate =  DateUtils.validDate(toDate);
			if (toEncounterDate == null) {
				return ResponseEntity.badRequest().body(ClientError.invalidEndDate());
			}
		}

		List<WellnessRecordBundle> wellnessRecordBundle
				= wellnessRecordService.getWellnessForVisit(patientId, visitUuid, fromEncounterDate, toEncounterDate);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.body(mapper.writeValueAsString(new BundledWellnessResponse(wellnessRecordBundle)));
	}
}
