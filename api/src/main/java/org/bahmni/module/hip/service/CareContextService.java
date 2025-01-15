package org.bahmni.module.hip.service;

import org.bahmni.module.hip.serializers.NewCareContext;

public interface CareContextService {

    <Type> Type careContextForPatient(String patientUuid);

    NewCareContext newCareContextsForPatient(String patientUuid);

    NewCareContext newCareContextsForPatientByVisitUuid(String patientUuid, String visitUuid);
}
