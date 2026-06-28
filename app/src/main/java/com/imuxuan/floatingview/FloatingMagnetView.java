package com.imuxuan.floatingview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class FloatingMagnetView extends FrameLayout {

    private float mOriginalRawX;
    private float mOriginalRawY;
    private float mOriginalX;
    private float mOriginalY;

    public FloatingMagnetView(Context context) {
        this(context, null);
    }

    public FloatingMagnetView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatingMagnetView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) return false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mOriginalX = getX();
                mOriginalY = getY();
                mOriginalRawX = event.getRawX();
                mOriginalRawY = event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                setX(mOriginalX + event.getRawX() - mOriginalRawX);
                setY(mOriginalY + event.getRawY() - mOriginalRawY);
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        return true;
    }
}
