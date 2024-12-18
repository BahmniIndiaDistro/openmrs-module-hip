package org.bahmni.module.hip.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CareContext {
    String careContextReference;
    String careContextType;
}
