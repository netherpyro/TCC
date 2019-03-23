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

    private final int ATTRS_DEFAULT_SIZE_SP_TEXT_CHART_NAME = 32;
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
            canvas.drawText(chartName, normalSpacing, chartNameSize + normalSpacing + smallSpacing, titlePaint);
        }
    }

    private void init() {
        setOrientation(VERTICAL);
        setWillNotDraw(false);

        titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(chartNameTextColor);
        titlePaint.setTextSize(chartNameSize);

        LinearLayout.LayoutParams graphParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        graphParams.topMargin = chartNameSize + normalSpacing * 2 + smallSpacing;
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

        private CoordinateResolver mainCoordinateResolver;
        private CoordinateResolver historyCoordinateResolver;

        private int rulerFloors = DEFAULT_QTY_RULER_FLOORS;
        @ColorInt
        private int rulerGridColor = 0xFFE7E8E9;
        @ColorInt
        private int rulerValueTextColor = 0xFF94A2AB;
        @ColorInt
        private int historyOverlayColor = 0x4DE7E8E9;
        @ColorInt
        private int historyControllerBorderColor = 0x1A94A2AB;
        @Px
        private int rulerValueTextSize = Util.spToPx(DEFAULT_SIZE_SP_TEXT_VALUE);

        private float abscessValueTextMaxWidth;
        private float abscessValueAppropriateSpacing;
        private Set<ViewedLineModel> viewedLinesData = new ArraySet<>();
        private List<String> rulerAbscissaLabels = new ArrayList<>();
        private List<String> rulerOrdinateLabels = new ArrayList<>();
        private float[] rulerAbscissaLabelXCoordinates;
        private float[] rulerOrdinateGridLinesPoints;
        private float[] mainLinePointsXCoordinates;
        private float[] historyLinePointsXCoordinates;
        private Paint rulerGridPaint;
        private Paint rulerValuePaint;
        private Paint linePaint;

        private int mainDrawportHeight;
        private int spaceHeight;
        private int historyDrawportHeight;

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

            mainCoordinateResolver = new CoordinateResolver(getWidth(), mainDrawportHeight, 0, rulerValueTextSize + normalSpacing);
            historyCoordinateResolver = new CoordinateResolver(getWidth(), historyDrawportHeight, mainDrawportHeight + spaceHeight, 0);

            abscissaValues = new Long[xValues.size()];
            xValues.toArray(abscissaValues);
            ordinateValuesSet.addAll(yValuesSet);

            viewedLinesData.clear();
            for (GraphLineModel lineModel : ordinateValuesSet) {
                viewedLinesData.add(new ViewedLineModel(lineModel.id, lineModel.color));
            }

            final int start = 0;
            final int end = abscissaValues.length - 1;

            horizontalFromIndex = start;
            horizontalToIndex = end;

            invalidateHistoryXValues(start, end);
            invalidateHistoryYValues(start, end);
            invalidateXValues();
            invalidateYValues();

            initialized = true;
            invalidate();
        }

        private void invalidateHistoryXValues(int start, int end) {
            calculateLinePointsXCoordinates(historyCoordinateResolver, start, end, true);
        }

        private void invalidateHistoryYValues(int start, int end) {
            prepareCoordinateResolverOrdinateWindow(historyCoordinateResolver, start, end);
            calculateLinePointsYCoordinates(historyCoordinateResolver, historyLinePointsXCoordinates, start, end, true);
        }

        void setWindow(float windowFromPercent, float windowToPercent) {
            if (!initialized) {
                return;
            }

            horizontalFromIndex = (int) (windowFromPercent * abscissaValues.length);
            horizontalToIndex = (int) (windowToPercent * abscissaValues.length);

            if (horizontalFromIndex < 0) {
                horizontalFromIndex = 0;
            }

            if (horizontalToIndex >= abscissaValues.length) {
                horizontalToIndex = abscissaValues.length - 1;
            }

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

            invalidateHistoryYValues(0, abscissaValues.length - 1);
            invalidateYValues();
            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int newWidth;
            int newHeight;

            newWidth = getMeasuredWidth();
            newHeight = (int) (newWidth * 1.06f);

            mainDrawportHeight = (int) (newHeight * 0.84f);
            spaceHeight = (int) (newHeight * 0.05f);
            historyDrawportHeight = (int) (newHeight * 0.11f);

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

            // draw chart and history lines
            for (ViewedLineModel line : viewedLinesData) {

                if (!line.enabled) {
                    continue;
                }

                linePaint.setColor(line.color);
                canvas.drawLines(line.mainLinePointsParts, linePaint);
                canvas.drawLines(line.historyLinePointsParts, linePaint);
            }

            //draw history overlay
            // todo
        }

        private void invalidateXValues() {
            final int viewedPointsCount = viewedPointsCount();

            // get x-coordinates of abscissa values to be displayed
            calculateLinePointsXCoordinates(mainCoordinateResolver, horizontalFromIndex, horizontalToIndex, false);

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
            final int indexStep = viewedPointsCount / (labelsCount - 1); // without the last label //todo fix divide by zero

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
            // calculate min and max ordinate value
            prepareCoordinateResolverOrdinateWindow(mainCoordinateResolver, horizontalFromIndex, horizontalToIndex);

            // calculate lines coordinates
            calculateLinePointsYCoordinates(mainCoordinateResolver, mainLinePointsXCoordinates, horizontalFromIndex, horizontalToIndex, false);

            // calculate labels & grid lines coordinates
            float maxValue = mainCoordinateResolver.maxOrdinateValue();
            float minValue = mainCoordinateResolver.minOrdinateValue();
            float step = (maxValue - minValue) / rulerFloors;

            int pointsArraySize = (rulerFloors + 1) * pointsInArrayOffset; // size: (rulerFloors + base floor) * point qty for every floor
            rulerOrdinateGridLinesPoints = new float[pointsArraySize];

            rulerOrdinateLabels.clear();

            int i = 0;
            while (i <= rulerFloors) { // include base floor
                float valueToAdd = (minValue + step * i);
                rulerOrdinateLabels.add(String.valueOf(valueToAdd));

                int arrayStartPointer = i * pointsInArrayOffset;
                rulerOrdinateGridLinesPoints[arrayStartPointer] = 0f;
                rulerOrdinateGridLinesPoints[arrayStartPointer + 1] = rulerOrdinateGridLinesPoints[arrayStartPointer + 3] = mainCoordinateResolver.yOfOrdinateValue(valueToAdd);
                rulerOrdinateGridLinesPoints[arrayStartPointer + 2] = (float) getWidth();

                i++;
            }
        }

        private void prepareCoordinateResolverOrdinateWindow(CoordinateResolver resolver, int fromIndex, int toIndex) {
            float maxValue = Float.MIN_VALUE;
            float minValue = Float.MAX_VALUE;

            for (ViewedLineModel viewedLineModel : viewedLinesData) {
                if (!viewedLineModel.enabled) continue;

                for (GraphLineModel fullLine : ordinateValuesSet) {
                    if (viewedLineModel.chartId.equals(fullLine.id)) {
                        for (int i = fromIndex; i <= toIndex; i++) {
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

            resolver.setOrdinateWindow(minValue, maxValue);
        }

        private void calculateLinePointsXCoordinates(CoordinateResolver resolver, int fromIndex, int toIndex, boolean forHistory) {
            final int viewedPointsCount = toIndex - fromIndex + 1;
            resolver.setAbscissaWindow(abscissaValues[fromIndex], abscissaValues[toIndex]);

            float[] pointsXCoordinates = new float[viewedPointsCount];
            for (int i = fromIndex; i <= toIndex; i++) {
                pointsXCoordinates[i] = resolver.xOfAbscissaValue(abscissaValues[i]);
            }

            if (forHistory) {
                historyLinePointsXCoordinates = pointsXCoordinates;
            } else {
                mainLinePointsXCoordinates = pointsXCoordinates;
            }
        }

        private void calculateLinePointsYCoordinates(CoordinateResolver resolver, float[] pointsXCoordinates, int fromIndex, int toIndex, boolean forHistory) {
            final int viewedPointsCount = toIndex - fromIndex + 1;

            for (ViewedLineModel viewedLineModel : viewedLinesData) {
                if (!viewedLineModel.enabled) continue;

                final float[] linePointsParts = new float[viewedPointsCount * pointsInArrayOffset];

                for (GraphLineModel fullLine : ordinateValuesSet) {
                    if (viewedLineModel.chartId.equals(fullLine.id)) {

                        int currentLineNumber = 0;
                        for (int i = fromIndex; i < toIndex; i++) {

                            int arrayStartPointer = currentLineNumber * pointsInArrayOffset;
                            linePointsParts[arrayStartPointer] = pointsXCoordinates[i];
                            linePointsParts[arrayStartPointer + 1] = resolver.yOfOrdinateValue(fullLine.values[i]);
                            linePointsParts[arrayStartPointer + 2] = pointsXCoordinates[i + 1];
                            linePointsParts[arrayStartPointer + 3] = resolver.yOfOrdinateValue(fullLine.values[i + 1]);

                            currentLineNumber++;
                        }
                    }
                }

                if (forHistory) {
                    viewedLineModel.historyLinePointsParts = linePointsParts;
                } else {
                    viewedLineModel.mainLinePointsParts = linePointsParts;
                }
            }
        }
    }
}
