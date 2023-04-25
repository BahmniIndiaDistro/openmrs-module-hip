package org.bahmni.module.hip.web.model;

import lombok.Getter;
import lombok.Setter;
import org.bahmni.module.hip.web.model.serializers.FhirBundleSerializer;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.hl7.fhir.r4.model.Bundle;

@Getter
@Setter
public class WellnessRecordBundle {
	private CareContext careContext;

	@JsonSerialize(using = FhirBundleSerializer.class)
	private Bundle bundle;

	public WellnessRecordBundle(CareContext careContext, Bundle bundle) {
		this.careContext = careContext;
		this.bundle = bundle;
	}
}
