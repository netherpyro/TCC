package com.netherpyro.tcc.chart;

import androidx.annotation.ColorInt;

/**
 * @author mmikhailov on 16/03/2019.
 */
final class ViewedLineModel {

    final String chartId;
    @ColorInt
    final int color;

    float[] mainLinePointsParts;
    float[] historyLinePointsParts;
    boolean enabled = true;

    ViewedLineModel(String chartId, @ColorInt int color) {
        this.chartId = chartId;
        this.color = color;
    }
}
