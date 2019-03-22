package com.netherpyro.tcc.chart;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;

import com.netherpyro.tcc.R;

import java.util.ArrayList;
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

    private ValueAnimator animator = null;

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

        final int size = data.get(0).columnData.size();
        animator = ValueAnimator.ofInt(1, size);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int index = (int) animation.getAnimatedValue();
                graphView.setWindow(0f, index / (size * 1f));
            }
        });
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setDuration(600L);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.start();
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
        private final int pointsInArrayOffset = 4;

        private Long[] abscissaValues;
        private final Set<GraphLineModel> ordinateValuesSet = new ArraySet<>();

        private ChartReferenceSystem crs;

        private int rulerFloors = DEFAULT_QTY_RULER_FLOORS;
        @ColorInt
        private int rulerGridColor = 0x80909090;
        @ColorInt
        private int rulerValueTextColor = 0xFF909090;
        @Px
        private int rulerValueTextSize = Util.spToPx(DEFAULT_SIZE_SP_TEXT_VALUE);

        private float abscessValueTextMaxWidth;
        private float abscessValueAppropriateSpacing;
        private Set<ViewedLineModel> viewedLinesData = new ArraySet<>();
        private List<String> rulerAbscissaLabels = new ArrayList<>();
        private List<String> rulerOrdinateLabels = new ArrayList<>();
        private float[] rulerAbscissaLabelXCoordinates;
        private float[] rulerOrdinateGridLinesPoints;
        private float[] linePointsXCoordinates;
        private Paint rulerGridPaint;
        private Paint rulerValuePaint;
        private Paint linePaint;

        private boolean initialized = false;

        private int horizontalFromIndex;
        private int horizontalToIndex;

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
        }

        private int viewedPointsCount() {
            return horizontalToIndex - horizontalFromIndex + 1;
        }

        void setValues(@NonNull List<Long> xValues, @NonNull Set<GraphLineModel> yValuesSet) {
            if (xValues.isEmpty()) {
                return;
            }

            crs = new ChartReferenceSystem(getWidth(), getHeight(), rulerValueTextSize + normalSpacing);

            abscissaValues = new Long[xValues.size()];
            xValues.toArray(abscissaValues);
            ordinateValuesSet.addAll(yValuesSet);

            horizontalFromIndex = 0;
            horizontalToIndex = abscissaValues.length - 1;

            viewedLinesData.clear();
            for (GraphLineModel lineModel : ordinateValuesSet) {
                viewedLinesData.add(new ViewedLineModel(lineModel.id, lineModel.color));
            }

            invalidateXValues();
            invalidateYValues();

            initialized = true;
            invalidate();
        }

        void setWindow(float windowFromPercent, float windowToPercent) {
            if (!initialized) {
                return;
            }

            horizontalFromIndex = (int) (windowFromPercent * abscissaValues.length);
            horizontalToIndex = (int) (windowToPercent * abscissaValues.length);

            invalidateXValues();
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
                int arrayStartPointer = i * pointsInArrayOffset;
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

                linePaint.setColor(line.color);
                canvas.drawLines(line.linePointsParts, linePaint);
            }
        }

        private void invalidateXValues() {
            // get x-coordinates of abscissa values to be displayed
            crs.setAbscissaWindow(abscissaValues[horizontalFromIndex], abscissaValues[horizontalToIndex]);

            final int viewedPointsCount = viewedPointsCount();

            linePointsXCoordinates = new float[viewedPointsCount];
            for (int i = horizontalFromIndex; i <= horizontalToIndex; i++) {
                linePointsXCoordinates[i] = crs.xOfAbscissaValue(abscissaValues[i]);
            }

            // get abscissa label x-positions and quantity
            final float viewWidth = (float) getWidth();
            int labelsCount = (int) (viewWidth / (abscessValueAppropriateSpacing + abscessValueTextMaxWidth));
            final float step = (viewWidth - abscessValueAppropriateSpacing) / labelsCount;

            labelsCount++; // add the last label

            rulerAbscissaLabelXCoordinates = new float[labelsCount];
            for (int k = 0; k < labelsCount; k++) {

                if (k == labelsCount - 1) {
                    rulerAbscissaLabelXCoordinates[k] = viewWidth; // most end label x value
                } else {
                    rulerAbscissaLabelXCoordinates[k] = k * step;
                }
            }

            // get abscissa labels text to be displayed
            final int[] indexes = new int[labelsCount];
            final int indexStep = viewedPointsCount / (labelsCount - 1); // without the last label

            int currentIndex = horizontalFromIndex;
            for (int j = 0; j < labelsCount - 1; j++) { // without the last label
                indexes[j] = currentIndex;
                currentIndex += indexStep;
            }

            indexes[indexes.length - 1] = viewedPointsCount - 1; // manually put last index as last item in array

            List<Long> rulerAbscissaValues = new ArrayList<>(labelsCount);
            for (int index : indexes) {
                rulerAbscissaValues.add(abscissaValues[index]);
            }

            rulerAbscissaLabels = Util.convertTimestampsToLabels(rulerAbscissaValues);
        }

        private void invalidateYValues() {
            float maxValue = Float.MIN_VALUE;
            float minValue = Float.MAX_VALUE;

            for (ViewedLineModel viewedLineModel : viewedLinesData) {
                if (!viewedLineModel.enabled) continue;

                for (GraphLineModel fullLine : ordinateValuesSet) {
                    if (viewedLineModel.chartId.equals(fullLine.id)) {
                        for (int i = horizontalFromIndex; i <= horizontalToIndex; i++) {
                            float currentValue = fullLine.values[i];

                            if (currentValue > maxValue) {
                                maxValue = currentValue;
                            }

                            if (currentValue < minValue) {
                                minValue = currentValue;
                            }
                        }
                    }
                }
            }

            crs.setOrdinateWindow(minValue, maxValue);

            for (ViewedLineModel viewedLineModel : viewedLinesData) {
                viewedLineModel.linePointsParts = new float[viewedPointsCount() * pointsInArrayOffset];

                for (GraphLineModel fullLine : ordinateValuesSet) {
                    if (viewedLineModel.chartId.equals(fullLine.id)) {

                        int currentLineNumber = 0;
                        for (int i = horizontalFromIndex; i < horizontalToIndex; i++) {

                            int arrayStartPointer = currentLineNumber * pointsInArrayOffset;
                            viewedLineModel.linePointsParts[arrayStartPointer] = linePointsXCoordinates[i];
                            viewedLineModel.linePointsParts[arrayStartPointer + 1] = crs.yOfOrdinateValue(fullLine.values[i]);
                            viewedLineModel.linePointsParts[arrayStartPointer + 2] = linePointsXCoordinates[i + 1];
                            viewedLineModel.linePointsParts[arrayStartPointer + 3] = crs.yOfOrdinateValue(fullLine.values[i + 1]);

                            currentLineNumber++;
                        }
                    }
                }
            }

            rulerOrdinateLabels.clear();
            float step = (maxValue - minValue) / rulerFloors;

            int pointsArraySize = (rulerFloors + 1) * pointsInArrayOffset; // size: (rulerFloors + base floor) * point qty for every floor
            rulerOrdinateGridLinesPoints = new float[pointsArraySize];

            int i = 0;
            while (i <= rulerFloors) { // include base floor
                float valueToAdd = (minValue + step * i);
                rulerOrdinateLabels.add(String.valueOf(valueToAdd));

                int arrayStartPointer = i * pointsInArrayOffset;
                rulerOrdinateGridLinesPoints[arrayStartPointer] = 0f;
                rulerOrdinateGridLinesPoints[arrayStartPointer + 1] = rulerOrdinateGridLinesPoints[arrayStartPointer + 3] = crs.yOfOrdinateValue(valueToAdd);
                rulerOrdinateGridLinesPoints[arrayStartPointer + 2] = (float) getWidth();

                i++;
            }
        }
    }
}
