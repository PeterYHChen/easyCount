package com.example.peter.berryestimator;
/**
 * Created by yonghong on 4/25/17.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import org.opencv.features2d.MSER;

public class DragCircleView extends ImageView {

    private Rect mImageBounds;
    private Rect mCircleBounds;
    private boolean mImageBoundsIsCorrect = false;
    private boolean mIsMovingCircleBounds = false;
    private Paint mRectPaint;
    private TextPaint mTextPaint = null;

    private int mPreX;
    private int mPreY;
    private OnUpCallback mCallback = null;

    public interface OnUpCallback {
        void onRectFinished(Rect rect);
    }

    public DragCircleView(final Context context) {
        super(context);
        init();
    }

    public DragCircleView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DragCircleView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Sets callback for up
     *
     */
    public void setOnUpCallback(OnUpCallback callback) {
        mCallback = callback;
    }

    /**
     * Inits internal data
     */
    private void init() {
        mRectPaint = new Paint();
        mRectPaint.setColor(getContext().getResources().getColor(android.R.color.holo_green_light));
        mRectPaint.setStyle(Paint.Style.STROKE);
        mRectPaint.setStrokeWidth(5); // TODO: should take from resources

        mTextPaint = new TextPaint();
        mTextPaint.setColor(getContext().getResources().getColor(android.R.color.holo_green_light));
        mTextPaint.setTextSize(20);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        int action = event.getActionMasked();

        // TODO: be aware of multi-touches
        int x = (int) event.getX();
        int y = (int) event.getY();
        int halfSize = mCircleBounds.width()/2;

        // make sure circle is inside the image
//        if (!mImageBounds.contains(mCircleBounds))
//            mCircleBounds = getSquareRect(mImageBounds);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // skip unnecessary down-touch x, y
                if (mCircleBounds.contains(x, y)) {
                    mPreX = x;
                    mPreY = y;
                    mIsMovingCircleBounds = true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mIsMovingCircleBounds) {
                    int offsetX = x - mPreX;
                    int offsetY = y - mPreY;
                    if (Math.abs(offsetX) > 0 || Math.abs(offsetY) > 0) {
                        if (mCircleBounds.left + offsetX < mImageBounds.left) {
                            offsetX = mImageBounds.left - mCircleBounds.left;
                        } else if (mCircleBounds.right + offsetX > mImageBounds.right) {
                            offsetX = mImageBounds.right - mCircleBounds.right;
                        }

                        if (mCircleBounds.top + offsetY < mImageBounds.top) {
                            offsetY = mImageBounds.top - mCircleBounds.top;
                        } else if (mCircleBounds.bottom + offsetY > mImageBounds.bottom) {
                            offsetY = mImageBounds.bottom - mCircleBounds.bottom;
                        }
                        mCircleBounds.offset(offsetX, offsetY);
                        invalidate();
                    }
                }

                mPreX = x;
                mPreY = y;
                break;

            case MotionEvent.ACTION_UP:
                mIsMovingCircleBounds = false;
//                if (mCallback != null) {
//                    mCallback.onRectFinished(mCircleBounds);
//                }
                break;

            default:
                break;
        }

        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        if (!mImageBoundsIsCorrect || mImageBounds == null) {
            mImageBounds = getBitmapRectInsideImageView(this);
            mCircleBounds = getSquareRect(mImageBounds);
            mImageBoundsIsCorrect = true;
        }

        if (mImageBounds != null && mCircleBounds != null) {
            float centerX = mCircleBounds.centerX();
            float centerY = mCircleBounds.centerY();
            float r = mCircleBounds.width()/2f;
            canvas.drawRect(mImageBounds, mRectPaint);
            canvas.drawCircle(centerX, centerY, r, mRectPaint);
            canvas.drawText("  (centerX: " + mCircleBounds.centerX() + ", centerY: " + mCircleBounds.centerY() + ")",
                    mCircleBounds.centerX(), mCircleBounds.centerY(), mTextPaint);
        }
    }

    private Rect getSquareRect(Rect rect) {
        if (rect == null)
            return null;

        int size = Math.min(rect.width(), rect.height());
        int left = rect.left + (rect.width() - size) / 2;
        int top = rect.top + (rect.height() - size) / 2;
        return new Rect(left, top, left + size, top + size);
    }

    private Rect getBitmapRectInsideImageView(ImageView imageView) {
        if (imageView == null || imageView.getDrawable() == null)
            return null;

        Rect rect = new Rect();

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int actW = Math.round(origW * scaleX);
        final int actH = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - actH)/2;
        int left = (int) (imgViewW - actW)/2;

        rect.set(left, top, left + actW, top + actH);

        return rect;
    }
}