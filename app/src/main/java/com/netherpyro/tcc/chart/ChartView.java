package com.netherpyro.tcc.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

/**
 * @author mmikhailov on 16/03/2019.
 */
public class ChartView extends LinearLayout {

    private final GraphView graphView = new GraphView(getContext());

    private String chartName = "";
    private int chartNameSize = 12;
    private int normalSpacing = 16;

    public ChartView(Context context) {
        super(context);
        init();
    }

    public ChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setData(ChartData data) {
        graphView.setValues(data.columnData, data.rowsData);
    }

    public void setChartName(String name) {
        chartName = name;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // draw title
        if (!chartName.isEmpty()) {

        }
    }

    private void init() {
        setOrientation(VERTICAL);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) graphView.getLayoutParams();
        params.topMargin = chartNameSize + (normalSpacing * 2);
        graphView.setLayoutParams(params);

        addView(graphView);
    }

    private class GraphView extends View {

        private final List<GraphTimestamp> xValues = new LinkedList<>();
        private final Set<GraphLineModel> yValuesSet = new ArraySet<>();

        private long maxYValue = -1L;
        private long minYValue = -1L;
        private Set<ViewedLineModel> viewedLinesData = null;
        private List<GraphTimestamp> viewedTimestampValues = null;

        GraphView(Context context) {
            super(context);
        }

        void setValues(@NonNull List<GraphTimestamp> xValues, @NonNull Set<GraphLineModel> yValuesSet) {
            this.xValues.addAll(xValues);
            this.yValuesSet.addAll(yValuesSet);

            for (GraphLineModel lineModel : this.yValuesSet) {
                this.viewedLinesData.add(new ViewedLineModel(lineModel.id, lineModel.values));
            }

            this.viewedTimestampValues = xValues;

            invalidate();
        }

        void setWindow(float windowFromPercent, float windowToPercent) {
            int fromIndex = (int) (windowFromPercent * xValues.size());
            int toIndex = (int) (windowToPercent * xValues.size());

            invalidateXValues(fromIndex, toIndex);
            invalidateYValues();
            invalidate();
        }

        void toggleChartLine(String chartId) {
            for (ViewedLineModel lineModel : viewedLinesData) {
                if (chartId.equals(lineModel.chartId)) {
                    lineModel.enabled = !lineModel.enabled;
                    break;
                }
            }

            invalidateYValues();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // draw ruling

        }

        private void invalidateXValues(int fromIndex, int toIndex) {
            viewedTimestampValues = xValues.subList(fromIndex, toIndex);

            for (ViewedLineModel viewedLineModel : viewedLinesData) {
                String curChartId = viewedLineModel.chartId;
                for (GraphLineModel fullLine : yValuesSet) {
                    if (curChartId.equals(fullLine.id)) {
                        viewedLineModel.updateValues(fullLine.values.subList(fromIndex, toIndex));
                    }
                }
            }
        }

        private void invalidateYValues() {
            long maxValue = Long.MIN_VALUE;
            long minValue = Long.MAX_VALUE;

            for (ViewedLineModel lineModel : viewedLinesData) {

                if (lineModel.enabled) {

                    if (lineModel.maxValue > maxValue) {
                        maxValue = lineModel.maxValue;
                    }

                    if (lineModel.minValue < minValue) {
                        minValue = lineModel.minValue;
                    }
                }
            }

            this.maxYValue = maxValue;
            this.minYValue = minValue;
        }
    }
}
