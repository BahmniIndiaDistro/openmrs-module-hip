package org.bahmni.module.hip.web.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bahmni.module.hip.model.HealthDocumentRecordBundle;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class BundledHealthDocumentRecordResponse {
    private List<HealthDocumentRecordBundle> healthDocumentRecord;
}
