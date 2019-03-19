package com.netherpyro.tcc.chart;

import java.util.List;
import java.util.Set;

/**
 * @author mmikhailov on 16/03/2019.
 */
public final class ChartData {

    final List<Long> columnData;
    final Set<GraphLineModel> rowsData;

    public ChartData(List<Long> columnData, Set<GraphLineModel> rowsData) {
        this.columnData = columnData;
        this.rowsData = rowsData;
    }
}
