package org.bahmni.module.hip.model;

import lombok.Getter;
import lombok.Setter;
import org.bahmni.module.hip.serializers.FhirBundleSerializer;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.hl7.fhir.r4.model.Bundle;

@Getter
@Setter
public class ImmunizationRecordBundle {
    private CareContext careContext;

    @JsonSerialize(using = FhirBundleSerializer.class)
    private Bundle bundle;

    public ImmunizationRecordBundle(CareContext careContext, Bundle bundle) {
        this.careContext = careContext;
        this.bundle = bundle;
    }

}
