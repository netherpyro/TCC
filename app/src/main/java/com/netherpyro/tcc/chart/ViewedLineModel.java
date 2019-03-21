package com.netherpyro.tcc.chart;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import androidx.annotation.ColorInt;

/**
 * @author mmikhailov on 16/03/2019.
 */
final class ViewedLineModel {

    final String chartId;
    final List<Float> values = new LinkedList<>();

    float maxValue = Float.MIN_VALUE;
    float minValue = Float.MAX_VALUE;
    boolean enabled = true;

    @ColorInt
    final int color;

    ViewedLineModel(String chartId, List<Float> values, @ColorInt int color) {
        this.chartId = chartId;
        this.color = color;

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
