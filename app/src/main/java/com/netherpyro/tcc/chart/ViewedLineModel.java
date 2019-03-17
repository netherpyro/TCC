package com.netherpyro.tcc.chart;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author mmikhailov on 16/03/2019.
 */
class ViewedLineModel {

    final String chartId;
    final List<Long> values = new LinkedList<>();

    long maxValue = Long.MIN_VALUE;
    long minValue = Long.MAX_VALUE;
    boolean enabled = true;

    public ViewedLineModel(String chartId, List<Long> values) {
        this.chartId = chartId;

        updateValues(values);
    }

    void updateValues(List<Long> values) {
        this.values.clear();
        this.values.addAll(values);

        List<Long> sortedValues = new LinkedList<>(values);
        Collections.sort(sortedValues);

        this.maxValue = sortedValues.get(sortedValues.size() - 1);
        this.minValue = sortedValues.get(0);
    }
}
