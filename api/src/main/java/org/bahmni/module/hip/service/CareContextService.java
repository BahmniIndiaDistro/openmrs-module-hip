package org.bahmni.module.hip.service;

import org.bahmni.module.hip.model.CareContext;
import org.bahmni.module.hip.serializers.NewCareContext;
import org.openmrs.Encounter;

public interface CareContextService {
    CareContext careContextFor(Encounter emrEncounter, Class careContextType);

    <Type> Type careContextForPatient(String patientUuid);

    NewCareContext newCareContextsForPatient(String patientUuid);

    NewCareContext newCareContextsForPatientByVisitUuid(String patientUuid, String visitUuid);
}
