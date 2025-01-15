package org.bahmni.module.hip.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
public class PatientAbhaInfo {
    private String abhaNumber;
    private String abhaAddress;

    public PatientAbhaInfo(@JsonProperty("abhaNumber") String abhaNumber, @JsonProperty("abhaAddress") String abhaAddress) {
        this.abhaNumber = abhaNumber;
        this.abhaAddress = abhaAddress;
    }
}
