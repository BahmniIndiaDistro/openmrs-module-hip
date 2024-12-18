package org.bahmni.module.hip.service;

import org.bahmni.module.hip.model.OrganizationContext;
import org.bahmni.module.hip.service.impl.OrganizationContextServiceImpl;
import org.openmrs.Location;

import java.util.Optional;

public interface OrganizationContextService {
    String ABDM_HFR_SYSTEM = "https://facility.abdm.gov.in/";

    static Optional<Location> findOrganization(Location location) {
        Optional<Location> orgLocation = OrganizationContextServiceImpl.identifyLocationByTag(location, OrganizationContextServiceImpl.ORGANIZATION_LOCATION_TAG);
        if (!orgLocation.isPresent()) {
            return OrganizationContextServiceImpl.identifyLocationByTag(location, OrganizationContextServiceImpl.VISIT_LOCATION_TAG);
        } else {
            return orgLocation;
        }
    }

    OrganizationContext buildContext(Optional<Location> org);
}
