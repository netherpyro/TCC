package com.netherpyro.tcc.chart;

/**
 * @author mmikhailov on 16/03/2019.
 */
final class GraphValue {

    final Long timestamp;
    final String displayString;

    public GraphValue(Long timestamp, String displayString) {
        this.timestamp = timestamp;
        this.displayString = displayString;
    }
}
