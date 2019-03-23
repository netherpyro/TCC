package com.netherpyro.tcc.chart;

/**
 * @author mmikhailov on 17/03/2019.
 */
final class CoordinateResolver {

    private ChartWindow window = new ChartWindow();
    private float width;
    private float height;
    private float drawportTop;
    private float topBottomDrawPortPadding;
    private float currentCostOfXValue;
    private float currentCostOfYValue;
    private float availableHeight;

    CoordinateResolver(float fullWidth, float chartHeight, float drawportTop, float topBottomDrawPortPadding) {
        this.width = fullWidth;
        this.height = chartHeight;
        this.drawportTop = drawportTop;
        this.topBottomDrawPortPadding = topBottomDrawPortPadding;
        this.availableHeight = height - topBottomDrawPortPadding * 2;
    }

    void setAbscissaWindow(float fromValue, float toValue) {
        window.left = fromValue;
        window.right = toValue;
        calculateCostInPxOfXValue();
    }

    void setOrdinateWindow(float fromValue, float toValue) {
        window.bottom = fromValue;
        window.top = toValue;
        calculateCostInPxOfYValue();
    }

    float yOfOrdinateValue(float value) {
        final float percent = (value - window.bottom) / window.height();
        return drawportTop + height - topBottomDrawPortPadding + percent * (2 * topBottomDrawPortPadding - height);
    }

    float xOfAbscissaValue(long value) {
        final float percent = (value - window.left) / window.width();
        return width * percent;
    }

    private void calculateCostInPxOfXValue() {
        currentCostOfXValue = width / window.width();
    }

    private void calculateCostInPxOfYValue() {
        currentCostOfYValue = availableHeight / window.height();
    }

    float rXTranslateInPx(float value) {
        return currentCostOfXValue * value;
    }

    float rYTranslateInPx(float value) {
        return currentCostOfYValue * value;
    }

    float maxOrdinateValue() {
        return window.top;
    }

    float minOrdinateValue() {
        return window.bottom;
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

        float width() {
            return right - left;
        }

        float height() {
            return top - bottom;
        }
    }
}