package com.netherpyro.tcc.chart;

/**
 * @author mmikhailov on 17/03/2019.
 */
final class CoordinateResolver {

    private ChartWindow window = new ChartWindow();
    private float width;
    private float height;
    private float drawportTop;
    private float drawportLeft;
    private float topBottomDrawPortPadding;
    private float fromX;
    private float toX;
    final private float xDistanceDefault;

    CoordinateResolver(float fullWidth, float chartHeight, float drawportLeft, float drawportTop, float topBottomDrawPortPadding, int totalCount) {
        this.width = fullWidth;
        this.height = chartHeight;
        this.drawportLeft = drawportLeft;
        this.drawportTop = drawportTop;
        this.topBottomDrawPortPadding = topBottomDrawPortPadding;
        xDistanceDefault = width / totalCount;
    }

    float[] xValuesForScale() {
        int numOfPoints = 0;
        for (float i = 0; i < width; i += xDistanceDefault) {
            if (i != 0 && i >= fromX && i <= toX) {
                numOfPoints++;
            }
        }

        float[] result = new float[numOfPoints]; // addition points: most start and most end values that are beyond visible rect
        float scaledDistance = width / numOfPoints;
        float currentDistance = 0;
        for (int i = 0; i < numOfPoints; i++) {
            result[i] = drawportLeft + currentDistance;
            currentDistance += scaledDistance;
        }

        return result;
    }

    void setXWindow(float fromX, float toX) {
        this.fromX = fromX;
        this.toX = toX;
    }

    void setOrdinateWindow(float fromValue, float toValue) {
        window.bottom = fromValue;
        window.top = toValue;
    }

    float yOfOrdinateValue(float value) {
        final float percent = (value - window.bottom) / window.height();
        return drawportTop + height - topBottomDrawPortPadding + percent * (2 * topBottomDrawPortPadding - height);
    }

    float maxOrdinateValue() {
        return window.top;
    }

    float minOrdinateValue() {
        return window.bottom;
    }

    final class ChartWindow {
        float top = 0f;
        float bottom = 0f;

        ChartWindow() {
        }

        float height() {
            return top - bottom;
        }
    }
}
