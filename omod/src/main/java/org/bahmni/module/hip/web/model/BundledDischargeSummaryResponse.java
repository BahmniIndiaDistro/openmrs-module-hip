package org.bahmni.module.hip.web.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bahmni.module.hip.model.DischargeSummaryBundle;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class BundledDischargeSummaryResponse {
    private List<DischargeSummaryBundle> dischargeSummary;
}