package com.example.demo.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.demo.R;

import java.util.ArrayList;
import java.util.List;

public class MotionCurveView extends View {
    private final float PADDING = 40;
    private Paint linePaint;
    private Paint fillPaint;
    private Paint pointPaint;
    private Paint axisPaint;
    private Paint gridPaint;
    private Path curvePath;
    private Path fillPath;

    // 所有原始数据
    private List<Float> dataList = new ArrayList<>();
    // 实际用来画的点（屏幕坐标）
    private List<PointF> pointList = new ArrayList<>();

    private int viewWidth;
    private int viewHeight;
    private float drawWidth;
    private float drawHeight;

    // 最多显示多少个点【可动态修改】
    private int maxShowCount = 10;
    private int curveColor;
    private int curveStartColor;
    private int curveEndColor;

    public MotionCurveView(Context context) {
        this(context, null);
    }

    public MotionCurveView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public MotionCurveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // 初始化属性
        initCustomAttrs(context, attrs);

        // 初始化画布
        init();
    }

    private void initCustomAttrs(Context context, AttributeSet attrs) {
        // 获取自定义值
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.MotionCurveView);
//        maxShowCount = (int) array.getFloat(R.styleable.MotionCurveView_maxCount, 20);
        curveColor = array.getColor(R.styleable.MotionCurveView_curveColor, Color.parseColor("#FF4081"));
        curveStartColor = array.getColor(R.styleable.MotionCurveView_shadowStartColor, Color.parseColor("#66FF4081"));
        curveEndColor = array.getColor(R.styleable.MotionCurveView_shadowStartColor, Color.parseColor("#08FF4081"));
        array.recycle();
    }

    private void init() {
        // 曲线
        linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setColor(curveColor);
        linePaint.setStrokeWidth(3);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        // 填充
        fillPaint = new Paint();
        fillPaint.setAntiAlias(true);
        fillPaint.setStyle(Paint.Style.FILL);

        // 数据点
        pointPaint = new Paint();
        pointPaint.setAntiAlias(true);
        pointPaint.setColor(Color.parseColor("#00ffffff"));
        pointPaint.setStyle(Paint.Style.FILL);

        // 坐标轴
        axisPaint = new Paint();
        axisPaint.setAntiAlias(true);
        axisPaint.setColor(Color.parseColor("#00ffffff"));
        axisPaint.setStrokeWidth(0);

        // 网格
        gridPaint = new Paint();
        gridPaint.setAntiAlias(true);
        gridPaint.setColor(Color.parseColor("#EEEEEE"));
        gridPaint.setStrokeWidth(0.5f);

        curvePath = new Path();
        fillPath = new Path();
    }

    /**
     * 动态设置最多显示点数
     */
    public void setMaxShowCount(int count) {
        this.maxShowCount = Math.max(count, 2);
        mapPoints();
        postInvalidate();
    }

    /**
     * 追加一个数据（实时刷新）
     */
    public void addData(float value) {
        dataList.add(value);
        mapPoints();
        postInvalidate();
    }

    /**
     * 重置所有数据
     */
    public void clearData() {
        dataList.clear();
        pointList.clear();
        postInvalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
        drawWidth = viewWidth - 2 * PADDING;
        drawHeight = viewHeight - 2 * PADDING;
        mapPoints();
    }

    /**
     * 只取最后 maxShowCount 个点进行绘制
     */
    private void mapPoints() {
        if (dataList.isEmpty()) return;

        pointList.clear();
        int start = Math.max(0, dataList.size() - maxShowCount);
        List<Float> showData = new ArrayList<>();
        for (int i = start; i < dataList.size(); i++) {
            showData.add(dataList.get(i));
        }

        float max = getMaxValue(showData);
        float min = getMinValue(showData);
        float range = max - min <= 0 ? 1 : max - min;
        int count = showData.size();
        float stepX = count > 1 ? drawWidth / (count - 1f) : 0;

        for (int i = 0; i < count; i++) {
            float x = PADDING + i * stepX;
            float value = showData.get(i);
            float y = PADDING + drawHeight - (value - min) / range * drawHeight;
            pointList.add(new PointF(x, y));
        }

        buildSmoothPath();
    }

    /**
     * 构建平滑曲线
     */
    private void buildSmoothPath() {
        if (pointList.size() < 2) return;

        curvePath.reset();
        fillPath.reset();
        PointF first = pointList.get(0);
        curvePath.moveTo(first.x, first.y);

        int size = pointList.size();
        for (int i = 1; i < size - 1; i++) {
            PointF p0 = pointList.get(i - 1);
            PointF p1 = pointList.get(i);
            PointF p2 = pointList.get(i + 1);

            float c1x = p1.x + (p2.x - p0.x) / 6f;
            float c1y = p1.y + (p2.y - p0.y) / 6f;

            if (i + 2 < size) {
                PointF p3 = pointList.get(i + 2);
                float c2x = p2.x - (p3.x - p1.x) / 6f;
                float c2y = p2.y - (p3.y - p1.y) / 6f;
                curvePath.cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y);
            } else {
                curvePath.quadTo(c1x, c1y, p2.x, p2.y);
            }
        }

        // 封闭填充
        fillPath.set(curvePath);
        PointF last = pointList.get(size - 1);
        fillPath.lineTo(last.x, viewHeight - PADDING);
        fillPath.lineTo(first.x, viewHeight - PADDING);
        fillPath.close();

        Shader shader = new LinearGradient(0, PADDING, 0, viewHeight - PADDING,
                curveStartColor, curveEndColor, Shader.TileMode.CLAMP);
        fillPaint.setShader(shader);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (pointList.isEmpty()) return;

        drawGridAndAxis(canvas);
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(curvePath, linePaint);
        drawDataPoints(canvas);
    }

    private void drawGridAndAxis(Canvas canvas) {
        canvas.drawLine(PADDING, viewHeight - PADDING, viewWidth - PADDING, viewHeight - PADDING, axisPaint);
        canvas.drawLine(PADDING, PADDING, PADDING, viewHeight - PADDING, axisPaint);

        for (int i = 1; i < 4; i++) {
            float y = PADDING + drawHeight / 4 * i;
            canvas.drawLine(PADDING, y, viewWidth - PADDING, y, gridPaint);
        }
    }

    private void drawDataPoints(Canvas canvas) {
        for (PointF p : pointList) {
            canvas.drawCircle(p.x, p.y, 4, pointPaint);
        }
    }

    private float getMaxValue(List<Float> list) {
        float max = list.get(0);
        for (float f : list) if (f > max) max = f;
        return max;
    }

    private float getMinValue(List<Float> list) {
        float min = list.get(0);
        for (float f : list) if (f < min) min = f;
        return min;
    }
}