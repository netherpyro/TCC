package com.netherpyro.tcc.chart;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
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

    public void setData(ChartData data) {
        graphView.setValues(data.columnData, data.rowsData);

        int i = 0;
        for (final GraphLineModel model : data.rowsData) {
            boolean drawDivider = i != data.rowsData.size() - 1;
            Checkbox cb = new Checkbox(getContext(), model.color, model.name, drawDivider, new CheckListener() {
                @Override
                public void onChecked(boolean checked) {
                    graphView.toggleChartLine(model.id);
                }
            });

            LinearLayout.LayoutParams cbParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            cbParams.topMargin = i == 0 ? smallSpacing : 0;
            cb.setLayoutParams(cbParams);

            addView(cb);
            i++;
        }
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
        setOrientation(LinearLayout.VERTICAL);
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
            abscessValueAppropriateSpacing = normalSpacing + smallSpacing;

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

        void setValues(@NonNull List<Long> xValues, @NonNull List<GraphLineModel> yValuesSet) {
            if (xValues.isEmpty()) {
                return;
            }

            graphWidth = getWidth();

            mainCoordinateResolver = new CoordinateResolver(graphWidth, mainDrawportHeight, 0, 0, rulerValueTextSize + normalSpacing, xValues.size());
            historyCoordinateResolver = new CoordinateResolver(graphWidth, historyDrawportHeight, 0, mainDrawportHeight + spaceHeight, xsmallSpacing, xValues.size());

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

            invalidateHistoryXValues();
            invalidateHistoryYValues(start, end);
            invalidateXValues();
            invalidateYValues();
            invalidateHistoryOverlayValues();

            initialized = true;
            invalidate();
        }

        private void invalidateHistoryXValues() {
            calculateLinePointsXCoordinates(historyCoordinateResolver, true);
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
            } else if (x > graphWidth) {
                rightX = graphWidth;
            } else {
                rightX = x;
            }

            setWindow(windowFromX, rightX);
        }

        void setWindowLeft(float x) {
            float leftX;

            if (x > windowToX - touchSlop) {
                return;
            } else if (x < 0) {
                leftX = 0;
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
                /*if (i == rulerAbscissaLabels.size() - 1) {
                    rulerValuePaint.setTextAlign(Paint.Align.RIGHT);
                } else {
                    rulerValuePaint.setTextAlign(Paint.Align.LEFT);
                }*/

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
            //rulerValuePaint.setTextAlign(Paint.Align.LEFT);

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

                if (lastTouchX == x) {
                    return true;
                }

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
            // get x-coordinates of abscissa values to be displayed
            calculateLinePointsXCoordinates(mainCoordinateResolver, false);

            // get abscissa label x-positions and quantity
            int labelsCount = (int) ((graphWidth - abscessValueTextMaxWidth) / (abscessValueAppropriateSpacing + abscessValueTextMaxWidth));
            final float step = (graphWidth - abscessValueTextMaxWidth) / labelsCount;

            labelsCount++; // add the first label

            rulerAbscissaLabelXCoordinates = new float[labelsCount];
            for (int k = 0; k < labelsCount; k++) {
                rulerAbscissaLabelXCoordinates[k] = k * step;
            }

            // get abscissa labels text to be displayed
            final int[] indexes = new int[labelsCount];
            final int indexStep = mainLinePointsXCoordinates.length / (labelsCount - 1); // without the first label //todo fix divide by zero

            int currentIndex = horizontalFromIndex;
            for (int j = 0; j < labelsCount; j++) {
                indexes[j] = currentIndex;
                currentIndex += indexStep;

                if (currentIndex >= abscissaValues.length) {
                    currentIndex = abscissaValues.length - 1;
                }
            }

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
                rulerOrdinateGridLinesPoints[arrayStartPointer + 1] =
                        rulerOrdinateGridLinesPoints[arrayStartPointer + 3] =
                                mainCoordinateResolver.yOfOrdinateValue(valueToAdd);
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

        private void calculateLinePointsXCoordinates(CoordinateResolver resolver, boolean forHistory) {
            resolver.setXWindow(windowFromX, windowToX);

            float[] pointsXCoordinates = resolver.xValuesForScale();

            if (forHistory) {
                historyLinePointsXCoordinates = pointsXCoordinates;
            } else {
                mainLinePointsXCoordinates = pointsXCoordinates;
            }
        }

        private void calculateLinePointsYCoordinates(CoordinateResolver resolver, float[] pointsXCoordinates, int fromIndex, int toIndex, boolean forHistory) {
            final int viewedPointsCount = toIndex - fromIndex + 1;

            for (final ViewedLineModel viewedLineModel : viewedLinesData) {
                if (!viewedLineModel.enabled) continue;

                final float[] linePointsParts = new float[viewedPointsCount * pointsInArrayOffset];

                for (final GraphLineModel fullLine : ordinateValuesSet) {
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
            leftOverlayRect.right = windowFromX;
            leftOverlayRect.bottom = historyBottom;

            rightOverlayRect.left = windowToX;
            rightOverlayRect.top = historyTop;
            rightOverlayRect.right = graphWidth;
            rightOverlayRect.bottom = historyBottom;

            historyControllerHorizontalLinesPointsCoordinates[0] = windowFromX;
            historyControllerHorizontalLinesPointsCoordinates[1] = historyTop + DEFAULT_HISTORY_CONTROLLER_HORIZONTAL_LINE_WIDTH / 2f;
            historyControllerHorizontalLinesPointsCoordinates[2] = windowToX;
            historyControllerHorizontalLinesPointsCoordinates[3] = historyTop + DEFAULT_HISTORY_CONTROLLER_HORIZONTAL_LINE_WIDTH / 2f;
            historyControllerHorizontalLinesPointsCoordinates[4] = windowFromX;
            historyControllerHorizontalLinesPointsCoordinates[5] = historyBottom - DEFAULT_HISTORY_CONTROLLER_HORIZONTAL_LINE_WIDTH / 2f;
            historyControllerHorizontalLinesPointsCoordinates[6] = windowToX;
            historyControllerHorizontalLinesPointsCoordinates[7] = historyBottom - DEFAULT_HISTORY_CONTROLLER_HORIZONTAL_LINE_WIDTH / 2f;

            historyControllerVerticalLinesPointsCoordinates[0] = windowFromX + DEFAULT_HISTORY_CONTROLLER_VERTICAL_LINE_WIDTH / 2f;
            historyControllerVerticalLinesPointsCoordinates[1] = historyTop + DEFAULT_HISTORY_CONTROLLER_HORIZONTAL_LINE_WIDTH;
            historyControllerVerticalLinesPointsCoordinates[2] = windowFromX + DEFAULT_HISTORY_CONTROLLER_VERTICAL_LINE_WIDTH / 2f;
            historyControllerVerticalLinesPointsCoordinates[3] = historyBottom - DEFAULT_HISTORY_CONTROLLER_HORIZONTAL_LINE_WIDTH;
            historyControllerVerticalLinesPointsCoordinates[4] = windowToX - DEFAULT_HISTORY_CONTROLLER_VERTICAL_LINE_WIDTH / 2f;
            historyControllerVerticalLinesPointsCoordinates[5] = historyTop + DEFAULT_HISTORY_CONTROLLER_HORIZONTAL_LINE_WIDTH;
            historyControllerVerticalLinesPointsCoordinates[6] = windowToX - DEFAULT_HISTORY_CONTROLLER_VERTICAL_LINE_WIDTH / 2f;
            historyControllerVerticalLinesPointsCoordinates[7] = historyBottom - DEFAULT_HISTORY_CONTROLLER_HORIZONTAL_LINE_WIDTH;
        }
    }

    private class Checkbox extends View {

        private final int DEFAULT_SIZE_SP_TEXT = 24;
        private final int DEFAULT_COLOR = Color.BLACK;
        private final int DEFAULT_BACK_COLOR = Color.WHITE;
        private final int ANIMATION_DURATION = 200;
        private final int DEFAULT_CB_SIZE_DP = 24;
        private final int DEFAULT_CB_RADIUS_DP = 4;

        @ColorInt
        private int backgroundColor = DEFAULT_BACK_COLOR;
        @ColorInt
        private int cbColor = DEFAULT_COLOR;
        @ColorInt
        private int textColor = DEFAULT_COLOR;
        @ColorInt
        private int dividerColor = 0xFFE7E8E9;
        @Px
        private int textSize = Util.spToPx(DEFAULT_SIZE_SP_TEXT);
        @Px
        private int cbSize = Util.dpToPx(DEFAULT_CB_SIZE_DP);
        @Px
        private int cbRadius = Util.dpToPx(DEFAULT_CB_RADIUS_DP);

        private String text = "New checkbox";
        private boolean drawDivider = true;
        private boolean checked = true;
        private boolean wasPressedDown = false;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF cbRect = new RectF();
        private final RectF cbRectAnim = new RectF();

        private CheckListener checkListener;
        private ValueAnimator animator = null;

        private float[] checkLines = new float[8];
        private float[] checkLinesAnim = new float[8];

        public Checkbox(
                Context context,
                int cbColor,
                String text,
                boolean drawDivider,
                @Nullable CheckListener checkListener
        ) {
            super(context);

            this.cbColor = cbColor;
            this.text = text;
            this.drawDivider = drawDivider;
            this.checkListener = checkListener;

            checkPaint.setColor(backgroundColor);
            checkPaint.setStyle(Paint.Style.STROKE);
            checkPaint.setStrokeWidth(6);
            checkPaint.setStrokeCap(Paint.Cap.ROUND);
            paint.setTextSize(textSize);
        }

        public Checkbox(Context context) {
            super(context);
        }

        void toggleChecked() {
            checked = !checked;

            if (checkListener != null) {
                checkListener.onChecked(checked);
            }

            if (animator != null) {
                animator.cancel();
            }

            final float cbHalfSize = cbSize / 2f;
            final float centerX = cbRect.left + cbHalfSize;
            final float centerY = cbRect.top + cbHalfSize;
            if (!checked) {
                // uncheck flow
                animator = ValueAnimator.ofFloat(1f, 0f);
                animator.setInterpolator(new AccelerateInterpolator());
                animator.setDuration(ANIMATION_DURATION);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float value = (float) animation.getAnimatedValue();
                        cbRectAnim.left = cbRect.left + cbHalfSize * (value);
                        cbRectAnim.top = cbRect.top + cbHalfSize * (value);
                        cbRectAnim.right = cbRect.right - cbHalfSize * value;
                        cbRectAnim.bottom = cbRect.bottom - cbHalfSize * value;

                        checkLinesAnim[0] = checkLines[0] + (centerX - checkLines[0]) * (1f - value);
                        checkLinesAnim[1] = checkLines[1] + (centerY - checkLines[1]) * (1f - value);
                        checkLinesAnim[2] = checkLinesAnim[4] = checkLines[2] + (centerX - checkLines[2]) * (1f - value);
                        checkLinesAnim[3] = checkLinesAnim[5] = checkLines[3] + (centerY - checkLines[3]) * (1f - value);
                        checkLinesAnim[6] = checkLines[6] + (checkLines[2] - checkLines[6]) * (1f - value);
                        checkLinesAnim[7] = checkLines[7] + (checkLines[3] - checkLines[7]) * (1f - value);

                        invalidate();
                    }
                });
                animator.start();
            } else {
                // check flow
                animator = ValueAnimator.ofFloat(0f, 1f);
                animator.setInterpolator(new AccelerateInterpolator());
                animator.setDuration(ANIMATION_DURATION);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float value = (float) animation.getAnimatedValue();

                        cbRectAnim.left = cbRect.left + cbHalfSize * (value);
                        cbRectAnim.top = cbRect.top + cbHalfSize * (value);
                        cbRectAnim.right = cbRect.right - cbHalfSize * value;
                        cbRectAnim.bottom = cbRect.bottom - cbHalfSize * value;

                        checkLinesAnim[0] = checkLines[0] + (centerX - checkLines[0]) * (1f - value);
                        checkLinesAnim[1] = checkLines[1] + (centerY - checkLines[1]) * (1f - value);
                        checkLinesAnim[2] = checkLinesAnim[4] = checkLines[2] + (centerX - checkLines[2]) * (1f - value);
                        checkLinesAnim[3] = checkLinesAnim[5] = checkLines[3] + (centerY - checkLines[3]) * (1f - value);
                        checkLinesAnim[6] = checkLines[6] + (checkLines[2] - checkLines[6]) * (1f - value);
                        checkLinesAnim[7] = checkLines[7] + (checkLines[3] - checkLines[7]) * (1f - value);

                        invalidate();
                    }
                });
                animator.start();
            }

        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                wasPressedDown = true;
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP && wasPressedDown) {
                wasPressedDown = false;
                toggleChecked();
                return true;
            }

            return super.onTouchEvent(event);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int newWidth;
            int newHeight;

            newWidth = getMeasuredWidth();
            newHeight = textSize + 3 * normalSpacing;

            cbRect.left = normalSpacing;
            cbRect.top = 1.5f * normalSpacing;
            cbRect.right = cbRect.left + cbSize;
            cbRect.bottom = cbRect.top + cbSize;

            checkLinesAnim[0] = checkLines[0] = cbRect.left + (cbSize / 4.5f);
            checkLinesAnim[1] = checkLines[1] = cbRect.top + (cbSize / 1.74f);
            checkLinesAnim[2] = checkLinesAnim[4] = checkLines[2] = checkLines[4] = cbRect.left + (cbSize / 2.57f);
            checkLinesAnim[3] = checkLinesAnim[5] = checkLines[3] = checkLines[5] = cbRect.top + (cbSize / 1.38f);
            checkLinesAnim[6] = checkLines[6] = cbRect.right - (cbSize / 4.9f);
            checkLinesAnim[7] = checkLines[7] = cbRect.top + (cbSize / 3.17f);

            setMeasuredDimension(newWidth, newHeight);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            paint.setColor(cbColor);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setStrokeWidth(4);
            canvas.drawRoundRect(cbRect, cbRadius, cbRadius, paint);

            canvas.drawLines(checkLinesAnim, checkPaint);

            paint.setColor(backgroundColor);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(cbRectAnim, cbRadius, cbRadius, paint);

            paint.setColor(textColor);
            canvas.drawText(text,
                    cbRect.right + normalSpacing + smallSpacing,
                    cbRect.bottom - ((cbSize - textSize)),
                    paint
            );

            if (drawDivider) {
                paint.setColor(dividerColor);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1);
                canvas.drawLine(
                        cbRect.right + normalSpacing,
                        getHeight(),
                        getWidth(),
                        getHeight(),
                        paint
                );
            }
        }
    }
}
