package com.example.demo.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * 中间刻度表（带动画效果）
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
    // 目标速度（最终要显示的速度）
    private float targetSpeed = 15.4f;
    // 动画过渡中的当前速度（用于绘制）
    private float animCurrentSpeed = 0f;
    // 弧形起始角度
    private float startAngle = 135f;
    // 弧形扫过角度
    private float sweepAngle = 270f;

    private RectF arcRect = new RectF();
    private Paint mPaint;
    private Path path;
    // 动画对象
    private ValueAnimator speedAnimator;

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

        // 1. 绘制底部进度环（使用动画过渡的速度值）
        float progressSweep = (animCurrentSpeed / maxSpeed) * sweepAngle;
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

        // 绘制带箭头的指针（使用动画过渡的速度值）
        float pointerAngle = startAngle + (animCurrentSpeed / maxSpeed) * sweepAngle;
        float pointerRadian = (float) Math.toRadians(pointerAngle);

        // 指针尖端坐标
        float pointerLength = radius - 60;
        float pointerEndX = centerX + pointerLength * (float) Math.cos(pointerRadian);
        float pointerEndY = centerY + pointerLength * (float) Math.sin(pointerRadian);

        // 指针尾部两点（宽度30）
        float pointXA = centerX - 10 * (float) Math.sin(pointerRadian);
        float pointYA = centerY + 10 * (float) Math.cos(pointerRadian);

        float pointXB = centerX + 10 * (float) Math.sin(pointerRadian);
        float pointYB = centerY - 10 * (float) Math.cos(pointerRadian);

        // 指针根部两点（宽度10）
        float pointerLength1 = radius - 120;
        float pointerEnd1X = centerX + pointerLength1 * (float) Math.cos(pointerRadian);
        float pointerEnd1Y = centerY + pointerLength1 * (float) Math.sin(pointerRadian);

        float pointXC = pointerEnd1X - 4 * (float) Math.sin(pointerRadian);
        float pointYC = pointerEnd1Y + 4 * (float) Math.cos(pointerRadian);

        float pointXD = pointerEnd1X + 4 * (float) Math.sin(pointerRadian);
        float pointYD = pointerEnd1Y - 4 * (float) Math.cos(pointerRadian);

        // 绘制指针路径
        path.reset();
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

        // 4. 绘制中心速度数字（使用动画过渡的速度值）
        textPaint.setTextSize(30);
        canvas.drawText("当前速度", centerX, centerY + 140, textPaint);
        textPaint.setTextSize(80);
        canvas.drawText(String.valueOf((int) animCurrentSpeed), centerX, centerY + 240, textPaint);
        textPaint.setTextSize(30);
        canvas.drawText("KM/H", centerX, centerY + 280, textPaint);
    }

    // 对外方法：更新速度并执行动画
    public void setSpeed(float speed) {
        // 限制速度范围
        targetSpeed = Math.max(0, Math.min(speed, maxSpeed));
        // 停止之前的动画（避免重复动画）
        if (speedAnimator != null && speedAnimator.isRunning()) {
            speedAnimator.cancel();
        }

        // 创建值动画：从当前动画值过渡到目标速度
        speedAnimator = ValueAnimator.ofFloat(animCurrentSpeed, targetSpeed);
        // 动画时长（可根据需求调整，单位：毫秒）
        speedAnimator.setDuration(800);
        // 减速插值器：动画先快后慢，更贴近真实指针转动效果
        speedAnimator.setInterpolator(new DecelerateInterpolator(1.2f));
        // 监听动画值变化
        speedAnimator.addUpdateListener(animation -> {
            // 更新动画过渡的速度值
            animCurrentSpeed = (float) animation.getAnimatedValue();
            // 重绘视图（触发onDraw）
            invalidate();
        });
        // 启动动画
        speedAnimator.start();
    }

    // 生命周期：销毁时释放动画资源
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (speedAnimator != null) {
            speedAnimator.cancel();
            speedAnimator = null;
        }
    }
}
