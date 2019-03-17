package com.netherpyro.tcc.chart;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.netherpyro.tcc.R;

import java.util.ArrayList;
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

    private final int ATTRS_DEFAULT_SIZE_SP_TEXT_CHART_NAME = 18;
    private final int ATTRS_DEFAULT_COLOR_RES_TEXT_CHART_NAME = R.color.colorAccent;
    private final int DEFAULT_STRING_RES_CHART_NAME = R.string.default_chart_name;
    private final int DEFAULT_DP_NORMAL_SPACING = 16;
    private final int DEFAULT_DP_SMALL_SPACING = 8;

    private final GraphView graphView = new GraphView(getContext());

    private String chartName = getResources().getString(DEFAULT_STRING_RES_CHART_NAME);
    private Paint titlePaint = null;
    @Px
    private int chartNameSize = Util.spToPx(ATTRS_DEFAULT_SIZE_SP_TEXT_CHART_NAME);
    @Px
    private int normalSpacing = Util.dpToPx(DEFAULT_DP_NORMAL_SPACING);
    @Px
    private int smallSpacing = Util.dpToPx(DEFAULT_DP_SMALL_SPACING);
    @ColorInt
    private int chartNameTextColor = ContextCompat.getColor(getContext(), ATTRS_DEFAULT_COLOR_RES_TEXT_CHART_NAME);

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

        MarginLayoutParams thisParams = (MarginLayoutParams) this.getLayoutParams();
        thisParams.topMargin = normalSpacing;
        thisParams.leftMargin = normalSpacing;
        thisParams.rightMargin = normalSpacing;
        this.setLayoutParams(thisParams);

        LinearLayout.LayoutParams graphParams = (LinearLayout.LayoutParams) graphView.getLayoutParams();
        graphParams.topMargin = chartNameSize + normalSpacing;
        graphView.setLayoutParams(graphParams);

        addView(graphView);
    }

    private void applyAttrs(@Nullable AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ChartView);

            try {
                if (a.hasValue(R.styleable.ChartView_chartName)) {
                    chartName = a.getString(R.styleable.ChartView_chartName);
                }

                chartNameSize = a.getDimensionPixelSize(R.styleable.ChartView_chartNameTextSize, ATTRS_DEFAULT_SIZE_SP_TEXT_CHART_NAME);
                chartNameTextColor = a.getColor(R.styleable.ChartView_chartNameTextColor, ContextCompat.getColor(getContext(), ATTRS_DEFAULT_COLOR_RES_TEXT_CHART_NAME));
            } finally {
                a.recycle();
            }
        }
    }

    private class GraphView extends View {

        private final int DEFAULT_SIZE_SP_TEXT_VALUE = 16;
        private final int DEFAULT_QTY_RULER_FLOORS = 5;

        private final List<Long> abscissaValues = new LinkedList<>();
        private final Set<GraphLineModel> ordinateValuesSet = new ArraySet<>();

        private ChartReferenceSystem crs;

        private int rulerFloors = DEFAULT_QTY_RULER_FLOORS;
        @ColorInt
        private int rulerGridColor = 0x80F1F1F1;
        @ColorInt
        private int rulerValueTextColor = 0xF1F1F1;
        @Px
        private int rulerValueTextSize = Util.spToPx(DEFAULT_SIZE_SP_TEXT_VALUE);

        private int gridLinePointsInArrayOffset = 4;

        private float rulerMaxYValue = -1f;
        private float rulerMinYValue = -1f;
        private float abscessValueTextMaxWidth;
        private float abscessValueAppropriateSpacing;
        private List<Long> viewedAbscissaValues = null;
        private Set<ViewedLineModel> viewedLinesData = null;
        private List<String> rulerAbscissaValues = new ArrayList<>();
        private List<String> rulerOrdinateValues = new ArrayList<>();
        private float[] rulerAbscissaValuesXCoordinates = new float[1];
        private float[] rulerOrdinateGridLinesPoints = new float[1];
        private Paint rulerGridPaint;
        private Paint rulerValuePaint;

        private boolean initialized = false;

        GraphView(Context context) {
            super(context);

            rulerValuePaint = new Paint();
            rulerValuePaint.setColor(rulerValueTextColor);
            rulerValuePaint.setTextSize(rulerValueTextSize);
            abscessValueTextMaxWidth = rulerValuePaint.measureText("WWW 99");
            abscessValueAppropriateSpacing = normalSpacing * 2;

            rulerGridPaint = new Paint();
            rulerGridPaint.setColor(rulerGridColor);
        }

        void setValues(@NonNull List<Long> xValues, @NonNull Set<GraphLineModel> yValuesSet) {
            if (xValues.isEmpty()) {
                return;
            }

            this.abscissaValues.addAll(xValues);
            this.ordinateValuesSet.addAll(yValuesSet);

            invalidateXValues(-1, -1);
            invalidateYValues();
            initChartReferenceSystem();
            initialized = true;

            invalidate();
        }

        void setWindow(float windowFromPercent, float windowToPercent) {
            if (!initialized) {
                return;
            }

            int fromIndex = (int) (windowFromPercent * abscissaValues.size());
            int toIndex = (int) (windowToPercent * abscissaValues.size());

            crs.setAbscissaWindow(abscissaValues.get(fromIndex), abscissaValues.get(toIndex));

            invalidateXValues(fromIndex, toIndex);
            invalidateYValues();

            invalidate();
        }

        void toggleChartLine(String chartId) {
            if (!initialized) {
                return;
            }

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
            if (!initialized) {
                return;
            }

            // draw ruler lines
            canvas.drawLines(rulerOrdinateGridLinesPoints, rulerGridPaint);

            // draw ruler Y values
            rulerValuePaint.setTextAlign(Paint.Align.LEFT);

            for (int i = 0; i <= rulerOrdinateValues.size(); i++) {
                int arrayStartPointer = i * gridLinePointsInArrayOffset;
                canvas.drawText(
                        rulerOrdinateValues.get(i),
                        rulerOrdinateGridLinesPoints[arrayStartPointer],
                        rulerOrdinateGridLinesPoints[arrayStartPointer + 1] - smallSpacing,
                        rulerValuePaint
                );
            }

            // draw ruler X values
            float abscissaValueY = rulerOrdinateGridLinesPoints[rulerOrdinateGridLinesPoints.length - 1] +
                    smallSpacing +
                    rulerValueTextSize;

            int i = 0;
            while (i < rulerAbscissaValues.size()) {
                if (i == rulerAbscissaValues.size() - 1) {
                    rulerValuePaint.setTextAlign(Paint.Align.RIGHT);
                } else {
                    rulerValuePaint.setTextAlign(Paint.Align.LEFT);
                }

                canvas.drawText(
                        rulerAbscissaValues.get(i),
                        rulerAbscissaValuesXCoordinates[i],
                        abscissaValueY,
                        rulerValuePaint
                );

                i++;
            }

            // draw chart lines
            // todo
        }

        private void initChartReferenceSystem() {
            crs = new ChartReferenceSystem(getWidth(), getHeight(), rulerFloors);
            crs.setAbscissaWindow(abscissaValues.get(0), abscissaValues.get(abscissaValues.size() - 1));
            crs.setOrdinateWindow(rulerMinYValue, rulerMaxYValue);
        }

        private void invalidateXValues(int fromIndex, int toIndex) {
            // get abscissa values to be displayed
            if (fromIndex > 0 && toIndex > 0) {
                viewedAbscissaValues = abscissaValues.subList(fromIndex, toIndex);

                for (ViewedLineModel viewedLineModel : viewedLinesData) {
                    for (GraphLineModel fullLine : ordinateValuesSet) {
                        if (viewedLineModel.chartId.equals(fullLine.id)) {
                            viewedLineModel.updateValues(fullLine.values.subList(fromIndex, toIndex));
                        }
                    }
                }
            } else {

                for (GraphLineModel lineModel : ordinateValuesSet) {
                    viewedLinesData.clear();
                    viewedLinesData.add(new ViewedLineModel(lineModel.id, lineModel.values));
                }

                viewedAbscissaValues = abscissaValues;
            }

            // get abscissa value labels to be displayed
            rulerAbscissaValues = Util.convertTimestampsToLabels(viewedAbscissaValues);

            // get abscissa value X positions
            final float viewWidth = (float) getWidth();

            int pointArraySize = viewedAbscissaValues.size();
            int spacing;
            int i = 0;

            do {
                int divider = i == 0 ? 1 : i * 2;
                spacing = (int) (viewWidth / (pointArraySize /= divider) - abscessValueTextMaxWidth);
                i++;
            } while (spacing < abscessValueAppropriateSpacing);

            float step = viewWidth / (pointArraySize - 1);
            rulerAbscissaValuesXCoordinates = new float[pointArraySize];

            for (int k = 0; k < pointArraySize; k++) {
                if (k == pointArraySize - 1) {
                    rulerAbscissaValuesXCoordinates[k] = viewWidth; // most end x value
                } else {
                    rulerAbscissaValuesXCoordinates[k] = k * step;
                }
            }
        }

        private void invalidateYValues() {
            float maxValue = Float.MIN_VALUE;
            float minValue = Float.MAX_VALUE;

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

            rulerMaxYValue = maxValue;
            rulerMinYValue = minValue;

            rulerOrdinateValues.clear();
            float step = (maxValue - minValue) / rulerFloors;

            int pointsArraySize = (rulerFloors + 1) * gridLinePointsInArrayOffset; // size: (rulerFloors + base floor) * point qty for every floor
            rulerOrdinateGridLinesPoints = new float[pointsArraySize];

            int i = 0;
            while (i <= rulerFloors) { // include base floor
                float valueToAdd = (minValue + step * i);
                rulerOrdinateValues.add(String.valueOf(valueToAdd));

                int arrayStartPointer = i * gridLinePointsInArrayOffset;
                rulerOrdinateGridLinesPoints[arrayStartPointer] = 0f;
                rulerOrdinateGridLinesPoints[arrayStartPointer + 1] = rulerOrdinateGridLinesPoints[arrayStartPointer + 3] = crs.yOfOrdinateValue(valueToAdd);
                rulerOrdinateGridLinesPoints[arrayStartPointer + 2] = (float) getWidth();

                i++;
            }
        }
    }
}
