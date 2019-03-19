package com.netherpyro.tcc.chart;

/**
 * @author mmikhailov on 17/03/2019.
 */
final class ChartReferenceSystem {

    private ChartWindow window = new ChartWindow();
    private float width;
    private float height;
    private int floors;
    private float textSizeWithSpacing;

    ChartReferenceSystem(float chartWidth, float chartHeight, int rulerFloors, float textSizeWithSpacing) {
        this.width = chartWidth;
        this.height = chartHeight;
        this.floors = rulerFloors;
        this.textSizeWithSpacing = textSizeWithSpacing;
    }

    void setAbscissaWindow(float fromValue, float toValue) {
        window.left = fromValue;
        window.right = toValue;
    }

    void setOrdinateWindow(float fromValue, float toValue) {
        window.bottom = fromValue;
        window.top = toValue;
    }

    float yOfOrdinateValue(float value) {
        final float percent = (value - window.bottom) / (window.top - window.bottom);
        final float refinedHeight = height - textSizeWithSpacing;
        return (height - textSizeWithSpacing * (1 - percent)) - (refinedHeight * percent);
    }

    final class ChartWindow {
        float left = 0f;
        float top = 0f;
        float right = 0f;
        float bottom = 0f;

        ChartWindow() {
        }

        ChartWindow(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }
}
