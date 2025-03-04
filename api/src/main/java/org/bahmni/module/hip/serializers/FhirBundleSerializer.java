package org.bahmni.module.hip.serializers;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.hl7.fhir.r4.model.Bundle;

import java.io.IOException;

public class FhirBundleSerializer extends JsonSerializer<Bundle> {
    @Override
    public void serialize(Bundle bundle, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        jsonGenerator.writeRaw(":" + BundleSerializer.serializeBundle(bundle));
    }
}
