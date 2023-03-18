package org.bahmni.module.hip.web.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Organization;
import org.openmrs.VisitType;

@Builder
public class OrganizationContext {
    @Getter @Setter
    private Organization organization;
    @Getter @Setter
    private String webUrl;

    public Class careContextType() {
        //Hardcoded right now. Should also deal with programType, visit or visitType.
        return VisitType.class;
    }
}
