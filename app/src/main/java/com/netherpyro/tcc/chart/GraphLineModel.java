package com.netherpyro.tcc.chart;

import java.util.LinkedList;
import java.util.List;

import androidx.annotation.ColorRes;

/**
 * @author mmikhailov on 16/03/2019.
 */
final class GraphLineModel {

    final String id;
    final String name;
    final List<Float> values = new LinkedList<>();

    @ColorRes
    final int colorRes;

    public GraphLineModel(String id, String name, List<Float> values, @ColorRes int colorRes) {
        this.id = id;
        this.name = name;
        this.values.addAll(values);
        this.colorRes = colorRes;
    }
}
