package com.example.st;

import android.content.Context;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;

public class OnSwipeTouchListener implements View.OnTouchListener {
    private final GestureDetector gestureDetector;

    public OnSwipeTouchListener(Context context) {
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private final class GestureListener extends SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 200;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            boolean result = false;

            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD) {
                        if (diffX > 0) onSwipeRight();
                        else onSwipeLeft();
                        result = true;
                    }
                }
                else if (Math.abs(diffY) > SWIPE_THRESHOLD) {
                    if (diffY > 0) onSwipeBottom();
                    else onSwipeTop();
                    result = true;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }
    }

    public void onSwipeRight() {}
    public void onSwipeLeft() {}
    public void onSwipeTop() {}
    public void onSwipeBottom() {}
}
