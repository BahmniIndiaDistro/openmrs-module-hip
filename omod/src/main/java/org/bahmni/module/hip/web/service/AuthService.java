package org.bahmni.module.hip.web.service;

import org.openmrs.annotation.Authorized;
import org.openmrs.api.OpenmrsService;
import org.openmrs.util.PrivilegeConstants;

public class AuthService implements OpenmrsService {

    @Override
    public void onStartup() {

    }

    @Override
    public void onShutdown() {

    }

    @Authorized({ PrivilegeConstants.GET_IDENTIFIER_TYPES })
    public void AuthTest() {
        return;
    }
}
