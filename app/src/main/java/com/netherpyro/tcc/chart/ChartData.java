package com.netherpyro.tcc.chart;

import java.util.List;
import java.util.Set;

/**
 * @author mmikhailov on 16/03/2019.
 */
final class ChartData {

    final List<GraphTimestamp> columnData;
    final Set<GraphLineModel> rowsData;

    public ChartData(List<GraphTimestamp> columnData, Set<GraphLineModel> rowsData) {
        this.columnData = columnData;
        this.rowsData = rowsData;
    }
}
