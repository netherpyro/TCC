package com.netherpyro.tcc.chart;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author mmikhailov on 16/03/2019.
 */
final class ViewedLineModel {

    final String chartId;
    final List<Float> values = new LinkedList<>();

    float maxValue = Float.MIN_VALUE;
    float minValue = Float.MAX_VALUE;
    boolean enabled = true;

    ViewedLineModel(String chartId, List<Float> values) {
        this.chartId = chartId;

        updateValues(values);
    }

    void updateValues(List<Float> values) {
        this.values.clear();
        this.values.addAll(values);

        List<Float> sortedValues = new LinkedList<>(values);
        // todo find better method to find min and max
        Collections.sort(sortedValues);

        this.maxValue = sortedValues.get(sortedValues.size() - 1);
        this.minValue = sortedValues.get(0);
    }
}
