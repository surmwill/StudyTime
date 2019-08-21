package com.example.st.views;

import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.example.st.R;

// Remove border when we swap to icon, but keep others
// add customizable center circle size/color + triangle
public class PieChartView extends View {
    public static final int NO_SLICE = -1;

    private static final String PROPERTY_SLICE_SIZE = "SLICE_SIZE";
    private static final float START_ANGLE = 0f;

    private static final int DEFAULT_COLOUR = Color.BLACK;
    private static final int ANIMATION_TIME = 300; //ms
    private static final int ICON_BACK_CIRCLE_PADDING = 20;

    // Properties of the center circle
    private float mControlCircleX, mControlCircleY;
    private float mControlCircleRadius;

    // Properties of the circle boundary with respect to colouring the slices
    private float mViewCircleBoundsRadius = 300f;

    // The dimensions of our view
    private float center;
    private float sliceArea;
    private float cursorX;
    private float cursorY;

    // Properties of icons and their outline
    private float centerIconDim;
    private float iconDim;
    private float iconCircleRadius;
    private float iconCircleOutlineRadius;
    private float extraIconDist;

    private float [] sliceAngleStarts;
    private Point[] iconCoords;

    private int sliceSize;
    private int numSlices;
    private int currSlice = NO_SLICE;
    private int prevSlice = NO_SLICE;

    private int [] sliceColours;
    private int [] sliceOutlineColours;

    private boolean centerTouched = false;

    private PropertyValuesHolder propertySliceSize = PropertyValuesHolder.ofInt(PROPERTY_SLICE_SIZE, 0, (int)mViewCircleBoundsRadius);
    private ValueAnimator animator;

    private Paint mControlCirclePaint;
    private Paint mSlicePaint;
    private Paint mSliceOutlinePaint;
    private Paint mTrianglePaint;
    private Paint mCircleIconBackPaint;
    private Paint mIconCircleOutlinePaint;

    private RectF sliceRectF;

    private Bitmap [] mSliceImages;
    private Bitmap centerIcon;

    private OnSliceSelectedListener mSliceSelectedListener;

    public PieChartView(Context context) {
        super(context);
        init(null);
    }

    public PieChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public PieChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public PieChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }



    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // slice size is constantly changing size, this is how we animate the filling arc
        sliceRectF.left = center - sliceSize;
        sliceRectF.top = center - sliceSize;
        sliceRectF.bottom = center + sliceSize;
        sliceRectF.right = center + sliceSize;

        for(int i = 0; i < numSlices; i++) {
            float startAngle = sliceAngleStarts[i];

            // Draw the corresponding slice we are on
            if(i == currSlice) {
                // draw slice
                mSlicePaint.setColor(sliceColours[i]);
                mSliceOutlinePaint.setColor(sliceOutlineColours[i]);

                canvas.drawArc(sliceRectF, startAngle, sliceArea, true, mSlicePaint);
                canvas.drawArc(sliceRectF, startAngle, sliceArea, true, mSliceOutlinePaint);
            }

            int iconX = iconCoords[i].x;
            int iconY = iconCoords[i].y;

            // position of the icon circle background
            float iconCircleX = iconX + iconDim/2;
            float iconCircleY = iconY + iconDim/2;

            // draw the icons and their background
            if(centerTouched) {
                canvas.drawCircle(iconCircleX, iconCircleY, iconCircleRadius, mCircleIconBackPaint);
                canvas.drawCircle(iconCircleX, iconCircleY, iconCircleOutlineRadius, mIconCircleOutlinePaint);
                canvas.drawBitmap(mSliceImages[i], iconX, iconY, null);
            }
        }

        // Calculate the pointer triangles dimensions
        Path path = new Path();
        path.moveTo(center + mControlCircleRadius, center);
        path.lineTo(center - mControlCircleRadius, center);
        path.lineTo(cursorX, cursorY);
        path.lineTo(center + mControlCircleRadius, center);
        path.close();

        // draw the triangle
        canvas.drawPath(path, mTrianglePaint);

        // draw black outline
        canvas.drawPath(path, mIconCircleOutlinePaint);

        // draws the center circle from which the triangle attaches to
        canvas.drawCircle(center, center, mControlCircleRadius, mControlCirclePaint);

        // draw outline
        canvas.drawCircle(center, center, mControlCircleRadius+1, mIconCircleOutlinePaint);

        // draw center icon
        canvas.drawBitmap(centerIcon, center - centerIconDim/2, center - centerIconDim/2, null);

        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean value = super.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();
        float relX = x - center;
        float relY = y - center;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                // we have touched the center circle and should display the icons
                if(Math.pow(relX, 2) + Math.pow(relY, 2) < Math.pow(mControlCircleRadius, 2)) {
                    centerTouched = true;
                    invalidate();
                }
                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                // If we have touched the center and are now dragging our finger elsewhere
                if(centerTouched) {
                    double r = Math.sqrt(Math.pow(relX, 2) + Math.pow(relY, 2));

                    // Draw the triangle only if we are touching inside the view circle, this is so
                    // we don't draw a weird overly stretched triangles
                    if(r <= mViewCircleBoundsRadius) {
                        cursorX = x;
                        cursorY = y;
                    }

                    // If we are outside the area of the center circle, the according slice should be coloured in.
                    // Note if we are outside the view circle, we will still fill the according slice
                    // colour based on angle
                    if (r >= mControlCircleRadius + (mControlCircleRadius / 10)) {
                        double trad = Math.atan(relY / relX);
                        double tdeg = Math.toDegrees(trad);

                        // Adjust the degree we calculated so it lines up with the slice angles.
                        // 0 degrees is the left horizontal, and then we go the opposite way
                        // so down is 90 degrees instead of 270 degrees
                        if (x >= center && y >= center) {
                            // do nothing
                        } else if (x <= center && y >= center) {
                            tdeg += 180;
                        } else if (x <= center && y <= center) {
                            tdeg += 180;
                        } else if (x >= center && y <= center) {
                            tdeg += 360;
                        }

                        for (int i = numSlices - 1; i >= 0; i--) {
                            if (tdeg >= sliceAngleStarts[i]) {
                                // If we are on the same slice as before, don't do the fill animation
                                // repeatedly, otherwise it mimics flashing
                                currSlice = i;
                                if(currSlice != prevSlice) animator.start();
                                prevSlice = currSlice;

                                break;
                            }
                        }
                    }
                    else {
                        prevSlice = NO_SLICE;
                        currSlice = NO_SLICE;
                    }
                }
                return true;
            }

            case MotionEvent.ACTION_UP: {
                if(centerTouched) {
                    centerTouched = false;

                    // If we ended the touch outside the center circle, return what slice we ended up on
                    if(Math.sqrt(Math.pow(relX, 2) + Math.pow(relY, 2)) > mControlCircleRadius) {
                        mSliceSelectedListener.onSliceSelected(currSlice);
                    }
                    else {
                        mSliceSelectedListener.onSliceSelected(NO_SLICE);
                    }

                    // reset the 3rd point of the triangle to the center
                    cursorX = center;
                    cursorY = center;

                    // cancel any slice animations
                    animator.cancel();
                    prevSlice = NO_SLICE;
                    currSlice = NO_SLICE;
                }
                return true;
            }
        }

        return value;
    }

    // Resize a bitmap to reqWidth x reqHeight
    private Bitmap getResizedBitmap(Bitmap bitmap, float reqWidth, float reqHeight) {
        // holds a 3x3 matrix for transforming coordinates
        Matrix matrix = new Matrix();

        RectF src = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF dest = new RectF(0, 0, reqWidth, reqHeight);

        // Set the matrix to scale and translate values that map the source rectangle to the destination rectangle
        matrix.setRectToRect(src, dest, Matrix.ScaleToFit.CENTER);

        // returns the bitmap transformed by the matrix
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int viewHeight = MeasureSpec.getSize(widthMeasureSpec);
        int viewWidth = MeasureSpec.getSize(heightMeasureSpec);
        center = Math.min(viewWidth, viewHeight) / 2;

        // center blob
        mControlCircleX = center;
        mControlCircleY = center;

        // triangle extending from blob to where we touch the screen
        cursorX = center;
        cursorY = center;

        // make the circle fill up as much of the view as possible
        mViewCircleBoundsRadius = center;
        propertySliceSize.setIntValues((int)mViewCircleBoundsRadius);

        // Calculate the x and y coords of each icon, each icon is positioned in the middle of a slice
        float startAngle = START_ANGLE;
        for(int i = 0; i < numSlices; i++) {
            sliceAngleStarts[i] = startAngle;

            // draw the icon in the middle of the slice
            double tdeg = startAngle + (sliceArea / 2);
            double trad = Math.toRadians(tdeg);
            float r = (int) (mViewCircleBoundsRadius / 2) + extraIconDist;

            int iconX = (int) (center - iconDim/2 + r * Math.cos(trad));
            int iconY = (int) (center - iconDim/2 + r * Math.sin(trad));

            iconCoords[i].x = iconX;
            iconCoords[i].y = iconY;

            startAngle += sliceArea;
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void init(@Nullable AttributeSet set) {
        // Initialize paint objects
        initPaint();

        // Fetch attributes
        try {
            initAttributes(set);
        }
        catch(PieChartViewLayoutException e) {

        }

        // Define animation
        animator = new ValueAnimator();
        animator.setValues(propertySliceSize);
        animator.setDuration(ANIMATION_TIME);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                sliceSize = (int) animation.getAnimatedValue(PROPERTY_SLICE_SIZE);

                invalidate();
            }
        });

        // Miscellaneous initializations...

        // The rectangle our arc will be drawn in
        sliceRectF = new RectF();

        // Coordinates of where we will draw the icons
        iconCoords = new Point[numSlices];

        // Calculate the area of each slice
        sliceArea = (1f / numSlices) * 360f;

        // Calculate starting angle of each arc and the area arc will sweep
        sliceAngleStarts = new float[numSlices];

        float startAngle = START_ANGLE;
        for(int i = 0; i < numSlices; i++) {
            sliceAngleStarts[i] = startAngle;
            startAngle += sliceArea;

            // extra pre allocation
            iconCoords[i] = new Point();
        }

        // icon properties
        mControlCircleRadius = centerIconDim / 2 + ICON_BACK_CIRCLE_PADDING;
        iconCircleRadius = iconDim / 2 + ICON_BACK_CIRCLE_PADDING;
        iconCircleOutlineRadius = iconCircleRadius + 1;
        extraIconDist = mViewCircleBoundsRadius / 10;
    }

    private void initAttributes(@Nullable AttributeSet set) throws PieChartViewLayoutException {
        if(set == null)
            throw new PieChartViewLayoutException("could not find attributes:\n" +
                    "1.) number of slices \n2.) colour of each slice \n3.) outline colour of each slice" +
                    "\n4.) icon for each slice");

        // Get attributes...
        TypedArray ta = getContext().obtainStyledAttributes(set, R.styleable.PieChartView);

        // Get attribute: number of slices
        numSlices = ta.getInt(R.styleable.PieChartView_num_slices, -1);

        // We need more than one slice for this view to function
        if(numSlices <= 0)
            throw new PieChartViewLayoutException("must have > 0 slices as an attribute");

        sliceColours = new int[numSlices];
        sliceOutlineColours = new int[numSlices];
        mSliceImages = new Bitmap[numSlices];

        // Get the color for the center circle and the triangle that protrudes out from it
        int triangleColor = ta.getColor(R.styleable.PieChartView_triangle_color, DEFAULT_COLOUR);
        mTrianglePaint.setColor(triangleColor);

        // Get center circle color
        int controlCircleColor = ta.getColor(R.styleable.PieChartView_center_circle_color, DEFAULT_COLOUR);
        mControlCirclePaint.setColor(controlCircleColor);

        // Get the size of the center circle and convert from dp to px
        Resources r = getResources();

        centerIconDim = ta.getFloat(R.styleable.PieChartView_center_icon_size, 35f);
        centerIconDim = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, centerIconDim, r.getDisplayMetrics());

        // Get the size of the icon and convert from dp to px
        iconDim = ta.getFloat(R.styleable.PieChartView_icon_size, 35f);
        iconDim = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, iconDim, r.getDisplayMetrics());

        // Fetch the array resource id's we passed as attributes
        int imageCenterCircleID = ta.getResourceId(R.styleable.PieChartView_center_icon, -1);
        int imageArrayResId = ta.getResourceId(R.styleable.PieChartView_icon_array, -1);
        int sliceColorArrayResId = ta.getResourceId(R.styleable.PieChartView_slice_colour_array, -1);
        int sliceColorBorderArrayResId = ta.getResourceId(R.styleable.PieChartView_slice_colour_border_array, -1);

        if(imageCenterCircleID != 0) {
            centerIcon = BitmapFactory.decodeResource(getResources(), imageCenterCircleID);
        }

        // Get attribute: colour array
        if(sliceColorArrayResId != 0) {
            TypedArray colorArray = getResources().obtainTypedArray(sliceColorArrayResId);

            for (int i = 0; i < numSlices; i++) {
                sliceColours[i] = colorArray.getColor(i, DEFAULT_COLOUR);
            }
            colorArray.recycle();
        }
        else { // Bad resource ID
            for(int i = 0; i < numSlices; i++) {
                sliceColours[i] = DEFAULT_COLOUR;
            }
        }

        // Get attribute: colour border array
        if(sliceColorBorderArrayResId != 0) {
            TypedArray borderColorArray = getResources().obtainTypedArray(sliceColorBorderArrayResId);

            for (int i = 0; i < numSlices; i++) {
                sliceOutlineColours[i] = borderColorArray.getColor(i, DEFAULT_COLOUR);
            }
            borderColorArray.recycle();
        }
        else { // Bad resource ID
            for(int i = 0; i < numSlices; i++) {
                sliceOutlineColours[i] = DEFAULT_COLOUR;
            }
        }

        // Get attribute: image icons for the slices
        if(imageArrayResId != 0 ) {
            TypedArray imgs = getResources().obtainTypedArray(imageArrayResId);

            for (int i = 0; i < numSlices; i++) {
                // Fetch the image resource based on it's resource ID in the array
                int imageId = imgs.getResourceId(i, -1);
                if (imageId == -1) {
                    throw new PieChartViewLayoutException("could not load image resource from array");
                }

                mSliceImages[i] = BitmapFactory.decodeResource(getResources(), imageId);
            }

            // Resize the images to our desired dimensions
            getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    centerIcon = getResizedBitmap(centerIcon, centerIconDim, centerIconDim);

                    for (int i = 0; i < numSlices; i++) {
                        mSliceImages[i] = getResizedBitmap(mSliceImages[i], iconDim, iconDim);
                    }
                }
            });

            imgs.recycle();
        }
        else { // Bad resource ID
            throw new PieChartViewLayoutException("must include image array attribute");
        }

        ta.recycle();
    }

    // Initialize Paints
    private void initPaint() {
        mControlCirclePaint = new Paint();
        mControlCirclePaint.setAntiAlias(true);
        mControlCirclePaint.setColor(Color.BLACK);

        mSlicePaint = new Paint();
        mSlicePaint.setAntiAlias(true);
        mSlicePaint.setStyle(Paint.Style.FILL);

        mSliceOutlinePaint = new Paint();
        mSliceOutlinePaint.setAntiAlias(true);
        mSliceOutlinePaint.setStyle(Paint.Style.STROKE);
        mSliceOutlinePaint.setStrokeWidth(2f);

        mTrianglePaint = new Paint();
        mTrianglePaint.setAntiAlias(true);
        mTrianglePaint.setStyle(Paint.Style.FILL);
        mTrianglePaint.setColor(Color.BLACK);

        mCircleIconBackPaint = new Paint();
        mCircleIconBackPaint.setAntiAlias(true);
        mCircleIconBackPaint.setStyle(Paint.Style.FILL);
        mCircleIconBackPaint.setColor(Color.WHITE);

        mIconCircleOutlinePaint = new Paint();
        mIconCircleOutlinePaint.setAntiAlias(true);
        mIconCircleOutlinePaint.setStyle(Paint.Style.STROKE);
        mIconCircleOutlinePaint.setStrokeWidth(2f);
        mIconCircleOutlinePaint.setColor(Color.BLACK);
    }

    // Listeners
    public interface OnSliceSelectedListener {
        public void onSliceSelected(int slice);
    }

    public void addOnSliceSelectedListener(OnSliceSelectedListener onSliceSelectedListener) {
        mSliceSelectedListener = onSliceSelectedListener;
    }

    // Exceptions
    public class PieChartViewLayoutException extends Exception {
        public PieChartViewLayoutException() {
        }

        public PieChartViewLayoutException(String message) {
            super(message);
        }

        public PieChartViewLayoutException(String message, Throwable cause) {
            super(message, cause);
        }

        public PieChartViewLayoutException(Throwable cause) {
            super(cause);
        }
    }

}

