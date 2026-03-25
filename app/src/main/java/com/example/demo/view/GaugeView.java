package com.example.demo.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * 中间刻度表
 */
public class GaugeView extends View {
    // 底部进度环
    private Paint arcPaint;
    // 刻度线
    private Paint scalePaint;
    // 刻度线
    private Paint whitePaint;
    // 指针
    private Paint pointerPaint;
    // 文字
    private Paint textPaint;
    // 最大速度
    private float maxSpeed = 50f;
    // 当前速度
    private float currentSpeed = 15.4f;
    // 弧形起始角度
    private float startAngle = 135f;
    // 弧形扫过角度
    private float sweepAngle = 270f;

    private RectF arcRect = new RectF();

    private Paint mPaint;
    private Path path;

    public GaugeView(Context context) {
        super(context);
        init();
    }

    public GaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 初始化进度环（蓝色渐变）
        arcPaint = new Paint();
        arcPaint.setColor(Color.parseColor("#4285F4"));
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(40);
        arcPaint.setAntiAlias(true);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        // 刻度线
        scalePaint = new Paint();
        scalePaint.setColor(Color.parseColor("#666666"));
        scalePaint.setStrokeWidth(4);
        scalePaint.setAntiAlias(true);

        // 指针
        pointerPaint = new Paint();
        pointerPaint.setColor(Color.parseColor("#3366CC"));
        pointerPaint.setStrokeWidth(12);
        pointerPaint.setAntiAlias(true);
        pointerPaint.setStrokeCap(Paint.Cap.ROUND);

        // 白圈
        whitePaint = new Paint();
        whitePaint.setColor(Color.WHITE);
        whitePaint.setStrokeWidth(4);
        whitePaint.setAntiAlias(true);

        // 文字
        textPaint = new Paint();
        textPaint.setColor(Color.parseColor("#222222"));
        textPaint.setTextSize(80);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);

        mPaint = new Paint();
        mPaint.setColor(Color.parseColor("#3366CC"));
        pointerPaint.setStrokeWidth(4);
        pointerPaint.setAntiAlias(true);
        pointerPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        path = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldh, oldw);
        int padding = 50;
        arcRect.set(padding, padding, w - padding, h - padding);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        float radius = (getWidth() - 100) >> 1;

        // 1. 绘制底部进度环
        float progressSweep = (currentSpeed / maxSpeed) * sweepAngle;
        canvas.drawArc(arcRect, startAngle, progressSweep, false, arcPaint);

        // 2. 绘制刻度线
        for (int i = 0; i <= maxSpeed; i += 10) {
            float angle = startAngle + (i / maxSpeed) * sweepAngle;
            float radian = (float) Math.toRadians(angle);

            float startX = centerX + (radius - 50) * (float) Math.cos(radian);
            float startY = centerY + (radius - 50) * (float) Math.sin(radian);
            float endX = centerX + radius * (float) Math.cos(radian);
            float endY = centerY + radius * (float) Math.sin(radian);

            canvas.drawLine(startX, startY, endX, endY, scalePaint);

            // 绘制刻度数字
            float textX = centerX + (radius - 90) * (float) Math.cos(radian);
            float textY = centerY + (radius - 90) * (float) Math.sin(radian);
            textPaint.setTextSize(40);
            canvas.drawText(String.valueOf(i), textX, textY, textPaint);
        }
        // 绘制一个带箭头的指针
        // 1. 计算指针角度
        float pointerAngle = startAngle + (currentSpeed / maxSpeed) * sweepAngle;
        float pointerRadian = (float) Math.toRadians(pointerAngle);

        // 2. 指针尖端坐标
        float pointerLength = radius - 60;
        float pointerEndX = centerX + pointerLength * (float) Math.cos(pointerRadian);
        float pointerEndY = centerY + pointerLength * (float) Math.sin(pointerRadian);

        // 3. 指针尾部两点（宽度30）
        float pointXA = centerX - 10 * (float) Math.sin(pointerRadian);
        float pointYA = centerY + 10 * (float) Math.cos(pointerRadian); // 修复：centerX → centerY

        float pointXB = centerX + 10 * (float) Math.sin(pointerRadian);
        float pointYB = centerY - 10 * (float) Math.cos(pointerRadian); // 修复：centerX → centerY

        // 4. 指针根部两点（宽度10）
        float pointerLength1 = radius - 120;
        float pointerEnd1X = centerX + pointerLength1 * (float) Math.cos(pointerRadian);
        float pointerEnd1Y = centerY + pointerLength1 * (float) Math.sin(pointerRadian);

        float pointXC = pointerEnd1X - 4 * (float) Math.sin(pointerRadian);
        float pointYC = pointerEnd1Y + 4 * (float) Math.cos(pointerRadian);

        float pointXD = pointerEnd1X + 4 * (float) Math.sin(pointerRadian);
        float pointYD = pointerEnd1Y - 4 * (float) Math.cos(pointerRadian);

        // 5. 绘制指针路径
        path.reset(); // 清空之前的路径
        path.moveTo(pointXA, pointYA);
        path.lineTo(pointXC, pointYC);
        path.lineTo(pointerEndX, pointerEndY);
        path.lineTo(pointXD, pointYD);
        path.lineTo(pointXB, pointYB);
        path.close();

        canvas.drawPath(path, pointerPaint);

        // 指针中心圆点
        canvas.drawCircle(centerX, centerY, 26, pointerPaint);
        // 绘制中间白色圆圈
        canvas.drawCircle(centerX, centerY, 15, whitePaint);

        // 4. 绘制中心速度数字

        textPaint.setTextSize(30);
        canvas.drawText("当前速度", centerX, centerY + 140, textPaint);
        textPaint.setTextSize(80);
        canvas.drawText(String.valueOf((int) currentSpeed), centerX, centerY + 240, textPaint);
        textPaint.setTextSize(30);
        canvas.drawText("KM/H", centerX, centerY + 280, textPaint);
    }

    // 对外方法：更新速度并刷新视图
    public void setSpeed(float speed) {
        this.currentSpeed = Math.max(0, Math.min(speed, maxSpeed));
        invalidate();
    }

}
