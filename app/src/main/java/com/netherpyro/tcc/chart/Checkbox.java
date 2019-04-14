package com.netherpyro.tcc.chart;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.annotation.Px;

@SuppressLint("ViewConstructor")
class Checkbox extends View {

    private final int DEFAULT_SIZE_SP_TEXT = 24;
    private final int DEFAULT_COLOR = Color.BLACK;
    private final int DEFAULT_BACK_COLOR = Color.WHITE;
    private final int ANIMATION_DURATION = 200;
    private final int DEFAULT_CB_SIZE_DP = 24;
    private final int DEFAULT_CB_RADIUS_DP = 4;

    @ColorInt
    private int backgroundColor = DEFAULT_BACK_COLOR;
    @ColorInt
    private int cbColor;
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

    private final String text;
    private boolean drawDivider;
    private boolean checked = true;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF cbRect = new RectF();
    private final RectF cbRectAnim = new RectF();
    private final int normalSpacing;
    private final int smallSpacing;

    private CheckListener checkListener;
    private ValueAnimator animator = null;

    private float[] checkLines = new float[8];
    private float[] checkLinesAnim = new float[8];

    private GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            toggleChecked();
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    };

    private GestureDetector detector = new GestureDetector(getContext(), gestureListener);

    Checkbox(Context context,
             int cbColor,
             String text,
             boolean drawDivider,
             int normalSpacing,
             int smallSpacing,
             @Nullable CheckListener checkListener) {

        super(context);

        this.cbColor = cbColor;
        this.text = text;
        this.drawDivider = drawDivider;
        this.checkListener = checkListener;
        this.normalSpacing = normalSpacing;
        this.smallSpacing = smallSpacing;

        checkPaint.setColor(backgroundColor);
        checkPaint.setStyle(Paint.Style.STROKE);
        checkPaint.setStrokeWidth(6);
        checkPaint.setStrokeCap(Paint.Cap.ROUND);
        paint.setTextSize(textSize);
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
        if (detector.onTouchEvent(event)) {
            return true;
        } else {
            return super.onTouchEvent(event);
        }
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