package com.netherpyro.tcc.chart;

import java.util.List;

/**
 * @author mmikhailov on 16/03/2019.
 */
public final class ChartData {

    final List<Long> columnData;
    final List<GraphLineModel> rowsData;

    public ChartData(List<Long> columnData, List<GraphLineModel> rowsData) {
        this.columnData = columnData;
        this.rowsData = rowsData;
    }
}
