package org.jp.waveview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by JiangPing on 2015/6/29.
 */
public class WaveView extends View {

    private int mWidth;
    private int mHeight;

    private int mWaveColor = Color.BLUE;

    private boolean mIsRiseUp;//默认水波上升
    private boolean mIsShowPercentText;//显示文本

    private float mWaveLineHeight;//水位线
    private float mWaveCrest = 0f;//波浪起伏幅度
    private float mWaveLength = 0f;//一个完整波波长

    private float mFirstPointX;//水波起始点(最左)
    private float mMoveLen;//水平移动距离

    private float mSpeedX = 2.0f;//水平移动速度
    private float mSpeedY = 0.2f;//水位上升速度

    private List<Point> mPoints;//水波的起始点、控制点、结束点
    private Paint mPaint;//水波画笔
    private Paint mTextPaint;//文本画笔
    private Path mWavePath;//水波的路径
    private boolean mHasMeasured = false;

    private DrawTask mDrawTask;
    private Handler mHandler = new Handler();

    private class DrawTask implements Runnable {
        @Override
        public void run() {
            movePointsAndInvalidate();
            mHandler.postDelayed(mDrawTask, 30);
        }
    }

    private void movePointsAndInvalidate() {
        // 记录平移总位移
        mMoveLen += mSpeedX;
        if (mIsRiseUp) {
            // 水位上升
            mWaveLineHeight -= mSpeedY;
            if (mWaveLineHeight < 0)
                mWaveLineHeight = 0;
        }

        mFirstPointX += mSpeedX;
        // 波形平移
        for (int i = 0; i < mPoints.size(); i++) {
            mPoints.get(i).setX(mPoints.get(i).getX() + mSpeedX);
            switch (i % 4) {
                case 0:
                    mPoints.get(i).setY(mWaveLineHeight);
                case 2:
                    mPoints.get(i).setY(mWaveLineHeight);
                    break;
                case 1:
                    mPoints.get(i).setY(mWaveLineHeight + mWaveCrest);
                    break;
                case 3:
                    mPoints.get(i).setY(mWaveLineHeight - mWaveCrest);
                    break;
            }
        }
        if (mMoveLen >= mWaveLength) {
            // 波形平移超过一个完整波形后复位
            mMoveLen = 0;
            resetPoints();
        }
        postInvalidate();
    }

    private void resetPoints() {
        mFirstPointX = -mWaveLength;
        for (int i = 0; i < mPoints.size(); i++) {
            mPoints.get(i).setX(i * mWaveLength / 4 - mWaveLength);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        mHandler.post(mDrawTask);
    }

    public WaveView(Context context) {
        this(context, null);
    }

    public WaveView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        handleTypeValue(attrs);
        init();
    }

    private void handleTypeValue(AttributeSet attrs) {
        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.WaveView);
        mIsRiseUp = ta.getBoolean(R.styleable.WaveView_isRiseUp, false);
        mIsShowPercentText = ta.getBoolean(R.styleable.WaveView_isShowPercentText, false);
        mWaveCrest = ta.getDimension(R.styleable.WaveView_waveCrest, 0);//波峰
        mWaveLength = ta.getDimension(R.styleable.WaveView_waveLength, 0);//波长
        mWaveLineHeight = ta.getDimension(R.styleable.WaveView_defaultWaveLineHeight, 0);//默认初始水位
        mWaveColor = ta.getColor(R.styleable.WaveView_waveColor, Color.BLUE);
        mSpeedY = ta.getFloat(R.styleable.WaveView_speedY, 0.2f);
        mSpeedX = ta.getFloat(R.styleable.WaveView_speedX, 2.0f);
        ta.recycle();

    }

    private void init() {
        mPoints = new ArrayList<>();

        mDrawTask = new DrawTask();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mWaveColor);

        mTextPaint = new Paint();
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(30);

        mWavePath = new Path();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!mHasMeasured) {
            mHasMeasured = true;
            mWidth = getMeasuredWidth();
            mHeight = getMeasuredHeight();

            //计算水位线高度
            if (mWaveLineHeight <= 0) {
                mWaveLineHeight = mHeight;//水位线在控件底部
            } else if (mWaveLineHeight >= mHeight) {
                mWaveLineHeight = 0;//水位线在控件顶部
            } else {
                mWaveLineHeight = mHeight - mWaveLineHeight;//水位线距离(0,0)点的Y距离
            }

            // 根据View宽度计算波形峰值
            mWaveCrest = mWaveCrest == 0f ? mWidth / 2.5f : mWaveCrest;

            // 波长等于四倍View宽度也就是View中只能看到四分之一个波形，这样可以使起伏更明显
            mWaveLength = mWaveLength == 0f ? mWidth * 4 : mWaveLength;

            // 左边隐藏的距离预留一个波形
            mFirstPointX = -mWaveLength;
            // 这里计算在可见的View宽度中能容纳几个波形，注意n上取整
            int n = (int) Math.round(mWidth / mWaveLength + 0.5);
            // n个波形需要4n+1个点，但是我们要预留一个波形在左边隐藏区域，所以需要4n+5个点
            for (int i = 0; i < (4 * n + 5); i++) {
                // 从P0开始初始化到P4n+4，总共4n+5个点
                float x = i * mWaveLength / 4 - mWaveLength * n;
                float y = 0;
                switch (i % 4) {
                    case 0:
                        y = mWaveLineHeight;
                    case 1:
                        // 往下波动的控制点
                        y = mWaveLineHeight + mWaveCrest;
                        break;
                    case 2:
                        // 零点位于水位线上
                        y = mWaveLineHeight;
                        break;
                    case 3:
                        // 往上波动的控制点
                        y = mWaveLineHeight - mWaveCrest;
                        break;
                }
                mPoints.add(new Point(x, y));
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mWavePath.reset();
        int i = 0;
        //移动到起始点
        mWavePath.moveTo(mPoints.get(0).getX(), mPoints.get(0).getY());
        //波形曲线部分
        for (; i < mPoints.size() - 2; i = i + 2) {
            mWavePath.quadTo(mPoints.get(i + 1).getX(),
                    mPoints.get(i + 1).getY(), mPoints.get(i + 2)
                            .getX(), mPoints.get(i + 2).getY());
        }
        //波形最右点向下到控件底部画线
        mWavePath.lineTo(mPoints.get(i).getX(), mHeight);
        mWavePath.lineTo(mFirstPointX, mHeight);
        //封闭整个水波区域
        mWavePath.close();

        canvas.drawPath(mWavePath, mPaint);

        if (!mIsShowPercentText) {
            return;
        }
        // 绘制百分比 x控件中部，y水位
        canvas.drawText("" + ((int) ((1 - mWaveLineHeight / mHeight) * 100))
                + "%", mWidth / 2, mWaveLineHeight + mWaveCrest
                + (mHeight - mWaveLineHeight - mWaveCrest) / 2, mTextPaint);

    }

    private class Point {
        private float x;
        private float y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public void setX(float x) {
            this.x = x;
        }

        public void setY(float y) {
            this.y = y;
        }
    }

    public void setShowPercentText(boolean flag) {
        mIsShowPercentText = flag;
    }

    public boolean getIsShowPercentText() {
        return mIsShowPercentText;
    }


    public void setWaveLineHeight(float waveLineHeight) {
        mWaveLineHeight = waveLineHeight;
    }
}
