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
import android.graphics.RectF;
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
    private boolean mIsZoomingCircleBounds = false;

    private Paint mRectPaint;
    private Paint mCirclePaint;
    private Paint mZoomingCirclePaint;
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
        mRectPaint.setColor(getContext().getResources().getColor(android.R.color.holo_blue_light));
        mRectPaint.setStyle(Paint.Style.STROKE);
        mRectPaint.setStrokeWidth(5); // TODO: should take from resources

        mCirclePaint = new Paint();
        mCirclePaint.setColor(getContext().getResources().getColor(android.R.color.holo_green_light));
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setStrokeWidth(5); // TODO: should take from resources

        mZoomingCirclePaint = new Paint();
        mZoomingCirclePaint.setColor(getContext().getResources().getColor(android.R.color.holo_orange_light));
        mZoomingCirclePaint.setStyle(Paint.Style.STROKE);
        mZoomingCirclePaint.setStrokeWidth(5); // TODO: should take from resources

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
        int radius = mCircleBounds.width()/2;

        // make sure circle is inside the image
        if (!mImageBounds.contains(mCircleBounds))
            mCircleBounds = getSquareRect(mImageBounds);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // skip unnecessary down-touch x, y
                if (onCircleEdge(mCircleBounds, x, y))
                    mIsZoomingCircleBounds = true;
                else if (mCircleBounds.contains(x, y)) {
                        mIsMovingCircleBounds = true;
                }
                invalidate();
                break;

            case MotionEvent.ACTION_MOVE:

                if (mIsZoomingCircleBounds) {
                    int centerX = mCircleBounds.centerX();
                    int centerY = mCircleBounds.centerY();
                    int zoomedRadius = (int) getPointDistance(x, y, centerX, centerY);

                    // set miminum radius of the circle and skip unacceptable radius
                    if (zoomedRadius > 150 && zoomedRadius <= Math.min(mImageBounds.width(), mImageBounds.height())/2) {
                        // pos0 = left, pos1 = top, pos2 = right, pos3 = bottom
                        int[] pos = {mCircleBounds.left, mCircleBounds.top, mCircleBounds.right, mCircleBounds.bottom};
                        boolean[] changed = {false, false, false, false};

                        int offset = zoomedRadius - radius;
                        boolean isNegative = false;

                        // get the minimum distance we can expand
                        int remainDist;
                        // changed pos left
                        if (x <= centerX) {
                            changed[0] = true;
                            remainDist = pos[0] - mImageBounds.left;
                            if (offset > remainDist)
                                offset = remainDist;
                        }
                        // changed pos top
                        if (y <= centerY) {
                            changed[1] = true;
                            remainDist = pos[1] - mImageBounds.top;
                            if (offset > remainDist)
                                offset = remainDist;
                        }
                        // changed pos right
                        if (x >= centerX) {
                            changed[2] = true;
                            remainDist = mImageBounds.right - pos[2];
                            if (offset > remainDist)
                                offset = remainDist;
                        }
                        // changed pos bottom
                        if (y >= centerY) {
                            changed[3] = true;
                            remainDist = mImageBounds.bottom - pos[3];
                            if (offset > remainDist)
                                offset = remainDist;
                        }

                        for (int i = 0; i < changed.length; i++)
                            if (changed[i])
                                if (i <= 1)
                                    pos[i] -= offset;
                                else
                                    pos[i] += offset;

                        mCircleBounds.set(pos[0], pos[1], pos[2], pos[3]);
                        invalidate();
                    }

                } else if (mIsMovingCircleBounds) {
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
                break;

            case MotionEvent.ACTION_UP:
                mIsMovingCircleBounds = false;
                mIsZoomingCircleBounds = false;
                invalidate();
//                if (mCallback != null) {
//                    mCallback.onRectFinished(mCircleBounds);
//                }
                break;

            default:
                break;
        }

        mPreX = x;
        mPreY = y;

        return true;
    }

    private boolean onCircleEdge(Rect circle, int x, int y) {
        double r = circle.width() / 2.0;
        double dist = getPointDistance(circle.centerX(), circle.centerY(), x, y);
        return Math.abs(dist - r) < 35;
    }

    private double getPointDistance(int x1, int y1, int x2, int y2) {
        int distX = x1 - x2;
        int distY = y1 - y2;
        return Math.sqrt(distX*distX + distY*distY);
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
            if (mIsZoomingCircleBounds)
                canvas.drawCircle(centerX, centerY, r, mZoomingCirclePaint);
            else
                canvas.drawCircle(centerX, centerY, r, mCirclePaint);

            canvas.drawText("  (left " + mCircleBounds.left + ", top: " + mCircleBounds.top +
                            ",centerX: " + mCircleBounds.centerX() + ", centerY: " + mCircleBounds.centerY() + ")",
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

        // Get the drawable
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

    public Bitmap getCroppedCircleBitmap() {
        if (getDrawable() == null)
            return null;

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        getImageMatrix().getValues(f);

        // Extract the scale values and reverse-scaled the circle region to bitmap
        // (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = 1/f[Matrix.MSCALE_X];
        final float scaleY = 1/f[Matrix.MSCALE_Y];

        RectF cropCircle = new RectF(getRelativeCircleRegion());

        //Create a matrix and apply the scale factors
        Matrix m = new Matrix();
        m.postScale(scaleX, scaleY);
        m.mapRect(cropCircle);

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        Bitmap bm = ((BitmapDrawable) getDrawable()).getBitmap();
        bm = Bitmap.createBitmap(bm, (int)(cropCircle.left), (int)(cropCircle.top),
                (int)(cropCircle.width()), (int)(cropCircle.height()));

        return MyUtils.transformSquareToCircleBitmap(bm);
    }

    // perform relative offset based on imageBounds
    private Rect getRelativeCircleRegion() {
        Rect rt = new Rect(mCircleBounds);
        rt.offset(-mImageBounds.left, -mImageBounds.top);
        return rt;
    }
}