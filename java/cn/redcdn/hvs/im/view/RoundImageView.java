package cn.redcdn.hvs.im.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ImageView;

import cn.redcdn.hvs.R;

/**
 * Desc
 * Created by wangkai on 2017/2/25.
 */

public class RoundImageView extends ImageView {
    private static final String TAG = "RoundImageView";
    /**
     * 图片的类型，圆形or圆角
     */
    private int type;
    public static final int TYPE_CIRCLE = 0;
    public static final int TYPE_ROUND = 1;
    /**
     * 圆角大小的默认值
     */
    private static final int CORNER_RADIUS_DEFAULT = 0;
    /**
     * 圆角的大小
     */
    private int mCornerRadius;

    /**
     * 绘图的Paint
     */
    private Paint mBitmapPaint;

    // 按下状态颜色
    private Paint mPressedColorPaint;
    private int pressedColor;

    /**
     * 圆角的半径
     */
    private int mRadius;
    /**
     * 3x3 矩阵，主要用于缩小放大
     */
    private Matrix mMatrix;
    /**
     * view的宽度
     */
    private int mWidth;
    private RectF mRoundRect;

    public RoundImageView(Context context, AttributeSet attrs) {

        super(context, attrs);
        mMatrix = new Matrix();
        mBitmapPaint = new Paint();
        mBitmapPaint.setAntiAlias(true);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.RoundImageView);

        pressedColor = a.getColor(R.styleable.RoundImageView_pressed_color, -1);
        if (pressedColor != -1) {
            mPressedColorPaint = new Paint();
            mPressedColorPaint.setAntiAlias(true);
            mPressedColorPaint.setColor(pressedColor);
        }

        mCornerRadius = a.getDimensionPixelSize(
                R.styleable.RoundImageView_corner_radius, (int) TypedValue
                        .applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                CORNER_RADIUS_DEFAULT, getResources()
                                        .getDisplayMetrics()));// 默认为3dp
        type = a.getInt(R.styleable.RoundImageView_type, TYPE_ROUND);// 默认为ROUND

        a.recycle();
    }

    public RoundImageView(Context context) {
        this(context, null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        /**
         * 如果类型是圆形，则强制改变view的宽高一致，以小值为准
         */
        if (type == TYPE_CIRCLE) {
            mWidth = Math.min(MeasureSpec.getSize(widthMeasureSpec),
                    MeasureSpec.getSize(heightMeasureSpec));
            mRadius = mWidth / 2;
        }

    }

    /**
     * 初始化BitmapShader
     */
    private void setUpShader() {
        Drawable drawable = getDrawable();
        if (drawable == null) {
            return;
        }

        Bitmap bmp = drawableToBitamp(drawable);
        // 将bmp作为着色器，就是在指定区域内绘制bmp

        // 渲染图像，使用图像为绘制图形着色
        BitmapShader mBitmapShader = new BitmapShader(bmp, Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP);
        float scale = 1.0f;
        if (type == TYPE_CIRCLE) {
            // 拿到bitmap宽或高的小值
            int bSize = Math.min(bmp.getWidth(), bmp.getHeight());
            scale = mWidth * 1.0f / bSize;

        } else if (type == TYPE_ROUND) {
            if (!(bmp.getWidth() == getWidth() && bmp.getHeight() == getHeight())) {
                // 如果图片的宽或者高与view的宽高不匹配，计算出需要缩放的比例；缩放后的图片的宽高，一定要大于我们view的宽高；所以我们这里取大值；
                scale = Math.max(getWidth() * 1.0f / bmp.getWidth(),
                        getHeight() * 1.0f / bmp.getHeight());
            }
        }
        // shader的变换矩阵，我们这里主要用于放大或者缩小
        mMatrix.setScale(scale, scale);
        // 设置变换矩阵
        mBitmapShader.setLocalMatrix(mMatrix);
        // 设置shader
        mBitmapPaint.setShader(mBitmapShader);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getDrawable() == null) {
            return;
        }
        setUpShader();

        if (type == TYPE_ROUND) {
            canvas.drawRoundRect(mRoundRect, mCornerRadius, mCornerRadius,
                    mBitmapPaint);
            if (isPressed() && mPressedColorPaint != null) {
                canvas.drawRoundRect(mRoundRect, mCornerRadius, mCornerRadius,
                        mPressedColorPaint);
            }

        } else {
            canvas.drawCircle(mRadius, mRadius, mRadius, mBitmapPaint);
            if (isPressed() && mPressedColorPaint != null) {
                canvas.drawCircle(mRadius, mRadius, mRadius, mPressedColorPaint);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // 圆角图片的范围
        if (type == TYPE_ROUND) {
            mRoundRect = new RectF(0, 0, w, h);
        }
    }

    /**
     * drawable转bitmap
     */
    private Bitmap drawableToBitamp(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bd = (BitmapDrawable) drawable;
            return bd.getBitmap();
        }
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        return bitmap;
    }

    private static final String STATE_INSTANCE = "state_instance";
    private static final String STATE_TYPE = "state_type";
    private static final String STATE_BORDER_RADIUS = "state_border_radius";
    private static final String STATE_PRESSED_COLOR = "state_pressed_color";

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(STATE_INSTANCE, super.onSaveInstanceState());
        bundle.putInt(STATE_TYPE, type);
        bundle.putInt(STATE_BORDER_RADIUS, mCornerRadius);
        bundle.putInt(STATE_PRESSED_COLOR, pressedColor);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            super.onRestoreInstanceState(((Bundle) state)
                    .getParcelable(STATE_INSTANCE));
            this.type = bundle.getInt(STATE_TYPE);
            this.mCornerRadius = bundle.getInt(STATE_BORDER_RADIUS);
            this.pressedColor = bundle.getInt(STATE_PRESSED_COLOR);
            if (pressedColor != -1) {
                mPressedColorPaint = new Paint();
                mPressedColorPaint.setAntiAlias(true);
                mPressedColorPaint.setColor(pressedColor);
            }
        } else {
            super.onRestoreInstanceState(state);
        }

    }

    public void setType(int type) {
        if (this.type != type) {
            this.type = type;
            if (this.type != TYPE_ROUND && this.type != TYPE_CIRCLE) {
                this.type = TYPE_CIRCLE;
            }
            requestLayout();
        }

    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        // imageView.setClickable(true),或imageView.setOnClickListener时才可触发dispatchSetPressed
        super.dispatchSetPressed(pressed);
        invalidate();
    }
}
