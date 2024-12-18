package org.bahmni.module.hip.service;

import org.bahmni.module.hip.exception.LGDCodeNotFoundException;

import java.util.Map;

public interface LgdCodeService {
    Map<String, Integer> getLGDCode(String state, String district) throws LGDCodeNotFoundException;
}
