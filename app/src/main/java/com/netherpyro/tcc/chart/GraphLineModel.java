package com.netherpyro.tcc.chart;

import java.util.LinkedList;
import java.util.List;

import androidx.annotation.ColorInt;

/**
 * @author mmikhailov on 16/03/2019.
 */
public final class GraphLineModel {

    final String id;
    final String name;
    final List<Float> values = new LinkedList<>();

    @ColorInt
    final int color;

    public GraphLineModel(String id, String name, List<Float> values, @ColorInt int color) {
        this.id = id;
        this.name = name;
        this.values.addAll(values);
        this.color = color;
    }
}
