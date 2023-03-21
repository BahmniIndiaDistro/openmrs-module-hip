package org.bahmni.module.hip.web.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@Builder
@Data
public class ValidPatient {
	private final boolean isValidPatient;
	private  String patientUuid;
}
