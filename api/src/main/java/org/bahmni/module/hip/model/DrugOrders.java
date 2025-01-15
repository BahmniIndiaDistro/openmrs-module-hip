package org.bahmni.module.hip.model;

import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DrugOrders {

    private List<DrugOrder> openMRSDrugOrders;

    public DrugOrders(@NotNull List<DrugOrder> openMRSDrugOrders) {
        this.openMRSDrugOrders = openMRSDrugOrders;
    }

    public Boolean isEmpty(){
        return CollectionUtils.isEmpty(openMRSDrugOrders);
    }

    public int size() {
        return openMRSDrugOrders.size();
    }

    Stream<DrugOrder> stream(){
        return openMRSDrugOrders.stream();
    }

    public List<DrugOrder> getOpenMRSDrugOrders() {
        return openMRSDrugOrders;
    }
}
