package org.bahmni.module.hip.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.bahmni.module.hip.serializers.FhirBundleSerializer;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.hl7.fhir.r4.model.Bundle;

@Builder
@Getter
@Setter
public class PrescriptionBundle {
    private CareContext careContext;

    @JsonSerialize(using = FhirBundleSerializer.class)
    private Bundle bundle;

}
