package org.bahmni.module.hip.web.service;

import org.bahmni.module.hip.web.exception.LGDCodeNotFoundException;
import org.openmrs.api.context.Context;
import org.openmrs.module.addresshierarchy.AddressField;
import org.openmrs.module.addresshierarchy.AddressHierarchyEntry;
import org.openmrs.module.addresshierarchy.AddressHierarchyLevel;
import org.openmrs.module.addresshierarchy.service.AddressHierarchyService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class LgdCodeService {

    public Map<String,Integer> getLGDCode(String state, String district) throws LGDCodeNotFoundException {
        Map<String,Integer> lgdCodeMap = new HashMap<>();
        AddressHierarchyService addressHierarchyService = Context.getService(AddressHierarchyService.class);
        AddressHierarchyLevel addressHierarchyLevel = addressHierarchyService.getAddressHierarchyLevelByAddressField(AddressField.COUNTY_DISTRICT);
        List<AddressHierarchyEntry> matchingDistrictEntries = addressHierarchyService.getAddressHierarchyEntriesByLevelAndName(addressHierarchyLevel,district);

        for (AddressHierarchyEntry entry : matchingDistrictEntries) {
            AddressHierarchyEntry stateEntry = entry.getParent();

            if (stateEntry.getName().equalsIgnoreCase(state)) {
                lgdCodeMap.put("stateCode", Integer.parseInt(stateEntry.getUserGeneratedId()));
                lgdCodeMap.put("districtCode", Integer.parseInt(entry.getUserGeneratedId()));
                return lgdCodeMap;
            }
        }

        throw new LGDCodeNotFoundException("LGD Code not found for " + district + ", " + state);
    }
}
