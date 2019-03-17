package com.netherpyro.tcc.chart;

/**
 * @author mmikhailov on 16/03/2019.
 */
final class GraphTimestamp {

    final long timestamp;
    final String displayedName;

    public GraphTimestamp(Long timestamp, String displayedName) {
        this.timestamp = timestamp;
        this.displayedName = displayedName;
    }
}
