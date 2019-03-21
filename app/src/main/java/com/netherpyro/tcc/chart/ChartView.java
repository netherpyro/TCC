package com.netherpyro.tcc.chart;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
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

    private final GraphView graphView = new GraphView(getContext());

    public ChartView(Context context) {
        super(context);
        init();
    }

    public ChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        applyAttrs(attrs);
        init();
    }

    public void setData(List<ChartData> data) {
        graphView.setValues(data.get(0).columnData, data.get(0).rowsData);
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

        titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(chartNameTextColor);
        titlePaint.setTextSize(chartNameSize);

        LinearLayout.LayoutParams graphParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        graphParams.topMargin = chartNameSize + normalSpacing;
        graphParams.leftMargin = normalSpacing;
        graphParams.rightMargin = normalSpacing;
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
        private int rulerGridColor = 0x80909090;
        @ColorInt
        private int rulerValueTextColor = 0xFF909090;
        @Px
        private int rulerValueTextSize = Util.spToPx(DEFAULT_SIZE_SP_TEXT_VALUE);

        private int gridLinePointsInArrayOffset = 4;

        private float rulerMaxYValue = -1f;
        private float rulerMinYValue = -1f;
        private float abscessValueTextMaxWidth;
        private float abscessValueAppropriateSpacing;
        private List<Long> viewedAbscissaValues = new LinkedList<>();
        private Set<ViewedLineModel> viewedLinesData = new ArraySet<>();
        private List<String> rulerAbscissaLabels = new ArrayList<>();
        private List<String> rulerOrdinateLabels = new ArrayList<>();
        private float[] rulerAbscissaLabelXCoordinates = new float[1];
        private float[] rulerOrdinateGridLinesPoints = new float[1];
        private Paint rulerGridPaint;
        private Paint rulerValuePaint;
        private Paint linePaint;
        private Path linePath;

        private boolean initialized = false;

        GraphView(Context context) {
            super(context);

            rulerValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            rulerValuePaint.setColor(rulerValueTextColor);
            rulerValuePaint.setTextSize(rulerValueTextSize);
            abscessValueTextMaxWidth = rulerValuePaint.measureText("WWW 99");
            abscessValueAppropriateSpacing = normalSpacing * 2;

            rulerGridPaint = new Paint();
            rulerGridPaint.setColor(rulerGridColor);

            linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            linePaint.setStrokeWidth(3);
            linePaint.setStyle(Paint.Style.STROKE);
            linePath = new Path();
        }

        void setValues(@NonNull List<Long> xValues, @NonNull Set<GraphLineModel> yValuesSet) {
            if (xValues.isEmpty()) {
                return;
            }

            this.abscissaValues.addAll(xValues);
            this.ordinateValuesSet.addAll(yValuesSet);

            invalidateXValues(-1, -1);
            initChartReferenceSystem();
            invalidateYValues();
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
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int newWidth;
            int newHeight;

            newWidth = getMeasuredWidth();
            newHeight = newWidth;

            setMeasuredDimension(newWidth, newHeight);
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

            for (int i = 0; i < rulerOrdinateLabels.size(); i++) {
                int arrayStartPointer = i * gridLinePointsInArrayOffset;
                canvas.drawText(
                        rulerOrdinateLabels.get(i),
                        rulerOrdinateGridLinesPoints[arrayStartPointer],
                        rulerOrdinateGridLinesPoints[arrayStartPointer + 1] - smallSpacing,
                        rulerValuePaint
                );
            }

            // draw ruler X values
            final float abscissaValueY = rulerOrdinateGridLinesPoints[1] + // y of most bottom line
                    smallSpacing + rulerValueTextSize;

            for (int i = 0; i < rulerAbscissaLabels.size(); i++) {
                if (i == rulerAbscissaLabels.size() - 1) {
                    rulerValuePaint.setTextAlign(Paint.Align.RIGHT);
                } else {
                    rulerValuePaint.setTextAlign(Paint.Align.LEFT);
                }

                canvas.drawText(
                        rulerAbscissaLabels.get(i),
                        rulerAbscissaLabelXCoordinates[i],
                        abscissaValueY,
                        rulerValuePaint
                );
            }

            // draw chart lines
            for (ViewedLineModel line : viewedLinesData) {

                if (!line.enabled) {
                    continue;
                }

                linePath.reset();
                linePaint.setColor(line.color);

                final float initialX = crs.xOfAbscissaValue(viewedAbscissaValues.get(0));
                final float initialY = crs.yOfOrdinateValue(line.values.get(0));
                final float relativeX = crs.rXTranslateInPx(viewedAbscissaValues.get(1) - viewedAbscissaValues.get(0));

                linePath.moveTo(initialX, initialY);

                float currentX = initialX;
                float currentY;
                for (int i = 1; i < line.values.size(); i++) {
                    //relativeY = crs.rYTranslateInPx(line.values.get(i) - line.values.get(i - 1));
                    //linePath.rLineTo(relativeX, relativeY);
                    currentX += relativeX;
                    currentY = crs.yOfOrdinateValue(line.values.get(i));
                    linePath.lineTo(currentX, currentY);
                }

                canvas.drawPath(linePath, linePaint);
            }
        }

        private void initChartReferenceSystem() {
            crs = new ChartReferenceSystem(getWidth(), getHeight(), rulerValueTextSize + normalSpacing);
            crs.setAbscissaWindow(abscissaValues.get(0), abscissaValues.get(abscissaValues.size() - 1));
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

                viewedLinesData.clear();
                for (GraphLineModel lineModel : ordinateValuesSet) {
                    viewedLinesData.add(new ViewedLineModel(lineModel.id, lineModel.values, lineModel.color));
                }

                viewedAbscissaValues = abscissaValues;
            }

            // get abscissa value X positions and quantity
            final float viewWidth = (float) getWidth();
            int pointArraySize = (int) (viewWidth / (abscessValueAppropriateSpacing + abscessValueTextMaxWidth));
            final float step = (viewWidth - abscessValueAppropriateSpacing) / pointArraySize;

            pointArraySize++; // add for the last value

            rulerAbscissaLabelXCoordinates = new float[pointArraySize];
            for (int k = 0; k < pointArraySize; k++) {

                if (k == pointArraySize - 1) {
                    rulerAbscissaLabelXCoordinates[k] = viewWidth; // most end x value
                } else {
                    rulerAbscissaLabelXCoordinates[k] = k * step;
                }
            }

            // get abscissa value labels to be displayed
            final int[] indexes = new int[pointArraySize];
            final int indexStep = viewedAbscissaValues.size() / (pointArraySize - 1); // without last value

            int currentIndex = 0;
            for (int j = 0; j < pointArraySize - 1; j++) { // without last value
                indexes[j] = currentIndex;
                currentIndex += indexStep;
            }

            indexes[indexes.length - 1] = viewedAbscissaValues.size() - 1; // manually put last index as last item in array

            List<Long> rulerAbscissaValues = new ArrayList<>(pointArraySize);
            for (int index : indexes) {
                rulerAbscissaValues.add(viewedAbscissaValues.get(index));
            }

            rulerAbscissaLabels = Util.convertTimestampsToLabels(rulerAbscissaValues);
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

            crs.setOrdinateWindow(rulerMinYValue, rulerMaxYValue);

            rulerOrdinateLabels.clear();
            float step = (maxValue - minValue) / rulerFloors;

            int pointsArraySize = (rulerFloors + 1) * gridLinePointsInArrayOffset; // size: (rulerFloors + base floor) * point qty for every floor
            rulerOrdinateGridLinesPoints = new float[pointsArraySize];

            int i = 0;
            while (i <= rulerFloors) { // include base floor
                float valueToAdd = (minValue + step * i);
                rulerOrdinateLabels.add(String.valueOf(valueToAdd));

                int arrayStartPointer = i * gridLinePointsInArrayOffset;
                rulerOrdinateGridLinesPoints[arrayStartPointer] = 0f;
                rulerOrdinateGridLinesPoints[arrayStartPointer + 1] = rulerOrdinateGridLinesPoints[arrayStartPointer + 3] = crs.yOfOrdinateValue(valueToAdd);
                rulerOrdinateGridLinesPoints[arrayStartPointer + 2] = (float) getWidth();

                i++;
            }
        }
    }
}
