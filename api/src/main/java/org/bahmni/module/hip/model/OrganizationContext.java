package org.bahmni.module.hip.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Organization;
import org.openmrs.Visit;

@Builder
public class OrganizationContext {
    @Getter @Setter
    private Organization organization;
    @Getter @Setter
    private String webUrl;

    public Class careContextType() {
        //Hardcoded right now. Should also deal with programType, visit or visitType.
        return Visit.class;
    }
}
