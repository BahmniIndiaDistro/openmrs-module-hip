package org.bahmni.module.hip.serializers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.bahmni.module.hip.model.PatientCareContext;

import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class NewCareContext {
    String patientName;
    String healthId;
    String patientReferenceNumber;
    String mobile;
    List<PatientCareContext> careContexts;
}
