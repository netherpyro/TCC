package com.netherpyro.tcc.chart;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.netherpyro.tcc.R;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.collection.ArraySet;
import androidx.core.content.ContextCompat;

/**
 * @author mmikhailov on 16/03/2019.
 */
public class ChartView extends LinearLayout {

    private final static int ATTRS_CHART_NAME_TEXT_SIZE_SP_DEFAULT = 18;
    private final static int ATTRS_CHART_NAME_TEXT_COLOR_DEFAULT = R.color.colorAccent;
    private final static int SPACING_DP_DEFAULT = 16;

    private final GraphView graphView = new GraphView(getContext());

    private String chartName = "";
    private Paint titlePaint = null;
    @Px
    private int chartNameSize = Util.spToPx(ATTRS_CHART_NAME_TEXT_SIZE_SP_DEFAULT);
    @Px
    private int normalSpacing = Util.dpToPx(SPACING_DP_DEFAULT);
    @ColorInt
    private int chartNameTextColor = ContextCompat.getColor(getContext(), ATTRS_CHART_NAME_TEXT_COLOR_DEFAULT);

    public ChartView(Context context) {
        super(context);
        init();
    }

    public ChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        applyAttrs(attrs);
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
            canvas.drawText(chartName, 0, 0, titlePaint);
        }
    }

    private void init() {
        setOrientation(VERTICAL);

        titlePaint = new Paint();
        titlePaint.setColor(chartNameTextColor);
        titlePaint.setTextSize(chartNameSize);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) graphView.getLayoutParams();
        params.topMargin = chartNameSize + (normalSpacing * 2);
        graphView.setLayoutParams(params);

        addView(graphView);
    }

    private void applyAttrs(@Nullable AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ChartView);

            if (a.hasValue(R.styleable.ChartView_chartName)) {
                chartName = a.getString(R.styleable.ChartView_chartName);
            }

            chartNameSize = a.getDimensionPixelSize(R.styleable.ChartView_chartNameTextSize, ATTRS_CHART_NAME_TEXT_SIZE_SP_DEFAULT);
            chartNameTextColor = a.getColor(R.styleable.ChartView_chartNameTextColor, ContextCompat.getColor(getContext(), ATTRS_CHART_NAME_TEXT_COLOR_DEFAULT));
        }
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
