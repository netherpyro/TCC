package com.netherpyro.tcc.chart;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
public class ChartView extends FrameLayout {

    private final int ATTRS_DEFAULT_SIZE_SP_TEXT_CHART_NAME = 32;
    private final int ATTRS_DEFAULT_COLOR_RES_TEXT_CHART_NAME = R.color.colorAccent;
    private final int DEFAULT_STRING_RES_CHART_NAME = R.string.default_chart_name;
    private final int DEFAULT_DP_NORMAL_SPACING = 16;
    private final int DEFAULT_DP_SMALL_SPACING = 8;
    private final int DEFAULT_DP_XSMALL_SPACING = 4;

    private String chartName = getResources().getString(DEFAULT_STRING_RES_CHART_NAME);
    private Paint titlePaint = null;
    @Px
    private int chartNameSize = Util.spToPx(ATTRS_DEFAULT_SIZE_SP_TEXT_CHART_NAME);
    @Px
    private int normalSpacing = Util.dpToPx(DEFAULT_DP_NORMAL_SPACING);
    @Px
    private int smallSpacing = Util.dpToPx(DEFAULT_DP_SMALL_SPACING);
    @Px
    private int xsmallSpacing = Util.dpToPx(DEFAULT_DP_XSMALL_SPACING);
    @ColorInt
    private int chartNameTextColor = ContextCompat.getColor(getContext(), ATTRS_DEFAULT_COLOR_RES_TEXT_CHART_NAME);

    private final GraphView graphView = new GraphView(getContext());

    private float lastTouchX = 0;

    private boolean touchingLeft;
    private boolean touchingRight;
    private boolean touchingCenter;

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
            canvas.drawText(chartName, normalSpacing, chartNameSize + normalSpacing + smallSpacing, titlePaint);
        }
    }

    private void init() {
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
        private final int TOUCH_SLOP_VALUE = 16;
        private final int DEFAULT_QTY_RULER_FLOORS = 5;
        private final int pointsInArrayOffset = 4;
        private final int DEFAULT_HISTORY_CONTROLLER_HORIZONTAL_LINE_WIDTH = 3;
        private final int DEFAULT_HISTORY_CONTROLLER_VERTICAL_LINE_WIDTH = DEFAULT_HISTORY_CONTROLLER_HORIZONTAL_LINE_WIDTH * 4;

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
        private int historyOverlayColor = 0xB3E7E8E9;
        @ColorInt
        private int historyControllerColor = 0x4D94A2AB;
        @Px
        private int rulerValueTextSize = Util.spToPx(DEFAULT_SIZE_SP_TEXT_VALUE);
        private final int touchSlop = Util.dpToPx(TOUCH_SLOP_VALUE);

        private int graphWidth;
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
        private Paint historyOverlayPaint;
        private Paint historyControllerPaint;

        private int mainDrawportHeight;
        private int spaceHeight;
        private int historyDrawportHeight;

        private boolean initialized = false;

        private int horizontalFromIndex;
        private int horizontalToIndex;

        final private RectF leftOverlayRect = new RectF();
        final private RectF rightOverlayRect = new RectF();
        final private float[] historyControllerHorizontalLinesPointsCoordinates = new float[pointsInArrayOffset * 2];
        final private float[] historyControllerVerticalLinesPointsCoordinates = new float[pointsInArrayOffset * 2];

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
            linePaint.setStrokeCap(Paint.Cap.ROUND);
            linePaint.setStrokeJoin(Paint.Join.ROUND);

            historyOverlayPaint = new Paint();
            historyOverlayPaint.setColor(historyOverlayColor);

            historyControllerPaint = new Paint();
            historyControllerPaint.setColor(historyControllerColor);
        }

        private int viewedPointsCount() {
            return horizontalToIndex - horizontalFromIndex + 1;
        }

        void setValues(@NonNull List<Long> xValues, @NonNull Set<GraphLineModel> yValuesSet) {
            if (xValues.isEmpty()) {
                return;
            }

            graphWidth = getWidth();

            mainCoordinateResolver = new CoordinateResolver(graphWidth, mainDrawportHeight, 0, rulerValueTextSize + normalSpacing);
            historyCoordinateResolver = new CoordinateResolver(graphWidth, historyDrawportHeight, mainDrawportHeight + spaceHeight, xsmallSpacing);

            abscissaValues = new Long[xValues.size()];
            xValues.toArray(abscissaValues);
            ordinateValuesSet.addAll(yValuesSet);

            viewedLinesData.clear();
            for (GraphLineModel lineModel : ordinateValuesSet) {
                viewedLinesData.add(new ViewedLineModel(lineModel.id, lineModel.color));
            }

            final int start = 0;
            final int end = abscissaValues.length - 1;
            windowFromX = 0f;
            windowToX = graphWidth;

            horizontalFromIndex = start;
            horizontalToIndex = end;

            invalidateHistoryXValues(start, end);
            invalidateHistoryYValues(start, end);
            invalidateXValues();
            invalidateYValues();
            invalidateHistoryOverlayValues();

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

        private float windowFromX;
        private float windowToX;

        void setWindow(float fromX, float toX) {
            if (!initialized) {
                return;
            }

            windowFromX = fromX;
            windowToX = toX;

            horizontalFromIndex = (int) (windowFromX / graphWidth * abscissaValues.length);
            horizontalToIndex = (int) (windowToX / graphWidth * abscissaValues.length);

            if (horizontalFromIndex < 0) {
                horizontalFromIndex = 0;
            }

            if (horizontalToIndex >= abscissaValues.length) {
                horizontalToIndex = abscissaValues.length - 1;
            }

            invalidateXValues();
            invalidateYValues();
            invalidateHistoryOverlayValues();

            invalidate();
        }

        void setWindowRight(float x) {
            float rightX;

            if (x < windowFromX + touchSlop) {
                return;
            } else {
                rightX = x;
            }

            setWindow(windowFromX, rightX);
        }

        void setWindowLeft(float x) {
            float leftX;

            if (x > windowToX - touchSlop) {
                return;
            } else {
                leftX = x;
            }
            setWindow(leftX, windowToX);
        }

        void moveWindow(float deltaX) {
            float leftX = windowFromX + deltaX;
            float rightX = windowToX + deltaX;

            if (leftX < 0) {
                leftX = 0;
                rightX = windowToX;
            } else if (rightX > graphWidth) {
                rightX = graphWidth;
                leftX = windowFromX;
            }

            if (leftX == windowFromX && rightX == windowToX) return;

            setWindow(leftX, rightX);
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
            spaceHeight = (int) (newHeight * 0.03f);
            historyDrawportHeight = (int) (newHeight * 0.13f);

            setMeasuredDimension(newWidth, newHeight);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (!initialized) {
                return;
            }

            // draw ruler lines
            canvas.drawLines(rulerOrdinateGridLinesPoints, rulerGridPaint);

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

            //draw history overlay
            canvas.drawRect(leftOverlayRect, historyOverlayPaint);
            canvas.drawRect(rightOverlayRect, historyOverlayPaint);

            historyControllerPaint.setStrokeWidth(DEFAULT_HISTORY_CONTROLLER_HORIZONTAL_LINE_WIDTH);
            canvas.drawLines(historyControllerHorizontalLinesPointsCoordinates, historyControllerPaint);

            historyControllerPaint.setStrokeWidth(DEFAULT_HISTORY_CONTROLLER_VERTICAL_LINE_WIDTH);
            canvas.drawLines(historyControllerVerticalLinesPointsCoordinates, historyControllerPaint);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (touchingLeft || touchingRight || touchingCenter) {
                    return true;
                }

                lastTouchX = event.getX();
                float y = event.getY();

                if (lastTouchX > (historyControllerHorizontalLinesPointsCoordinates[0] + touchSlop) &&
                        lastTouchX < (historyControllerHorizontalLinesPointsCoordinates[2] - touchSlop) &&
                        y > (historyControllerHorizontalLinesPointsCoordinates[1] - touchSlop) &&
                        y < (historyControllerHorizontalLinesPointsCoordinates[7] + touchSlop)) {

                    touchingRight = false;
                    touchingLeft = false;
                    touchingCenter = true;
                    Log.d("Dbg.", "touching center");
                    return true;
                }

                if (lastTouchX > (historyControllerHorizontalLinesPointsCoordinates[2] - touchSlop) &&
                        lastTouchX < (historyControllerHorizontalLinesPointsCoordinates[2] + touchSlop) &&
                        y > (historyControllerHorizontalLinesPointsCoordinates[1] - touchSlop) &&
                        y < (historyControllerHorizontalLinesPointsCoordinates[7] + touchSlop)) {

                    touchingRight = true;
                    touchingLeft = false;
                    touchingCenter = false;
                    Log.d("Dbg.", "touching right");
                    return true;
                }

                if (lastTouchX > (historyControllerHorizontalLinesPointsCoordinates[0] - touchSlop) &&
                        lastTouchX < (historyControllerHorizontalLinesPointsCoordinates[0] + touchSlop) &&
                        y > (historyControllerHorizontalLinesPointsCoordinates[1] - touchSlop) &&
                        y < (historyControllerHorizontalLinesPointsCoordinates[7] + touchSlop)) {

                    touchingRight = false;
                    touchingLeft = true;
                    touchingCenter = false;
                    Log.d("Dbg.", "touching left");
                    return true;
                }
            }

            if (event.getAction() == MotionEvent.ACTION_UP) {
                touchingRight = false;
                touchingLeft = false;
                touchingCenter = false;
                Log.d("Dbg.", "touching nothing");
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                final float x = event.getX();
                final float y = event.getY();

                if (touchingRight) {
                    setWindowRight(x);
                } else if (touchingLeft) {
                    setWindowLeft(x);
                } else if (touchingCenter) {
                    moveWindow(x - lastTouchX);
                }

                lastTouchX = x;

                return true;
            }

            return super.onTouchEvent(event);
        }

        private void invalidateXValues() {
            final int viewedPointsCount = viewedPointsCount();

            // get x-coordinates of abscissa values to be displayed
            calculateLinePointsXCoordinates(mainCoordinateResolver, horizontalFromIndex, horizontalToIndex, false);

            // get abscissa label x-positions and quantity
            int labelsCount = (int) (graphWidth / (abscessValueAppropriateSpacing + abscessValueTextMaxWidth));
            final float step = (graphWidth - abscessValueAppropriateSpacing) / labelsCount;

            labelsCount++; // add the last label

            rulerAbscissaLabelXCoordinates = new float[labelsCount];
            for (int k = 0; k < labelsCount; k++) {

                if (k == labelsCount - 1) {
                    rulerAbscissaLabelXCoordinates[k] = graphWidth; // most end label x value
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
                rulerOrdinateGridLinesPoints[arrayStartPointer + 2] = (float) graphWidth;

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
            for (int i = 0; i < viewedPointsCount; i++) {
                pointsXCoordinates[i] = resolver.xOfAbscissaValue(abscissaValues[i + fromIndex]);
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

                        for (int i = 0; i < pointsXCoordinates.length - 1; i++) {
                            int arrayStartPointer = i * pointsInArrayOffset;
                            linePointsParts[arrayStartPointer] = pointsXCoordinates[i];
                            linePointsParts[arrayStartPointer + 1] = resolver.yOfOrdinateValue(fullLine.values[i + fromIndex]);
                            linePointsParts[arrayStartPointer + 2] = pointsXCoordinates[i + 1];
                            linePointsParts[arrayStartPointer + 3] = resolver.yOfOrdinateValue(fullLine.values[i + fromIndex + 1]);
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

        private void invalidateHistoryOverlayValues() {
            float historyTop = mainDrawportHeight + spaceHeight;
            float historyBottom = historyTop + historyDrawportHeight;

            leftOverlayRect.left = 0;
            leftOverlayRect.top = historyTop;
            leftOverlayRect.right = historyLinePointsXCoordinates[horizontalFromIndex];
            leftOverlayRect.bottom = historyBottom;

            rightOverlayRect.left = historyLinePointsXCoordinates[horizontalToIndex];
            rightOverlayRect.top = historyTop;
            rightOverlayRect.right = graphWidth;
            rightOverlayRect.bottom = historyBottom;

            historyControllerHorizontalLinesPointsCoordinates[0] = historyLinePointsXCoordinates[horizontalFromIndex];
            historyControllerHorizontalLinesPointsCoordinates[1] = historyTop + DEFAULT_HISTORY_CONTROLLER_HORIZONTAL_LINE_WIDTH / 2f;
            historyControllerHorizontalLinesPointsCoordinates[2] = historyLinePointsXCoordinates[horizontalToIndex];
            historyControllerHorizontalLinesPointsCoordinates[3] = historyTop + DEFAULT_HISTORY_CONTROLLER_HORIZONTAL_LINE_WIDTH / 2f;
            historyControllerHorizontalLinesPointsCoordinates[4] = historyLinePointsXCoordinates[horizontalFromIndex];
            historyControllerHorizontalLinesPointsCoordinates[5] = historyBottom - DEFAULT_HISTORY_CONTROLLER_HORIZONTAL_LINE_WIDTH / 2f;
            historyControllerHorizontalLinesPointsCoordinates[6] = historyLinePointsXCoordinates[horizontalToIndex];
            historyControllerHorizontalLinesPointsCoordinates[7] = historyBottom - DEFAULT_HISTORY_CONTROLLER_HORIZONTAL_LINE_WIDTH / 2f;

            historyControllerVerticalLinesPointsCoordinates[0] = historyLinePointsXCoordinates[horizontalFromIndex] + DEFAULT_HISTORY_CONTROLLER_VERTICAL_LINE_WIDTH / 2f;
            historyControllerVerticalLinesPointsCoordinates[1] = historyTop + DEFAULT_HISTORY_CONTROLLER_HORIZONTAL_LINE_WIDTH;
            historyControllerVerticalLinesPointsCoordinates[2] = historyLinePointsXCoordinates[horizontalFromIndex] + DEFAULT_HISTORY_CONTROLLER_VERTICAL_LINE_WIDTH / 2f;
            historyControllerVerticalLinesPointsCoordinates[3] = historyBottom - DEFAULT_HISTORY_CONTROLLER_HORIZONTAL_LINE_WIDTH;
            historyControllerVerticalLinesPointsCoordinates[4] = historyLinePointsXCoordinates[horizontalToIndex] - DEFAULT_HISTORY_CONTROLLER_VERTICAL_LINE_WIDTH / 2f;
            historyControllerVerticalLinesPointsCoordinates[5] = historyTop + DEFAULT_HISTORY_CONTROLLER_HORIZONTAL_LINE_WIDTH;
            historyControllerVerticalLinesPointsCoordinates[6] = historyLinePointsXCoordinates[horizontalToIndex] - DEFAULT_HISTORY_CONTROLLER_VERTICAL_LINE_WIDTH / 2f;
            historyControllerVerticalLinesPointsCoordinates[7] = historyBottom - DEFAULT_HISTORY_CONTROLLER_HORIZONTAL_LINE_WIDTH;
        }
    }
}
