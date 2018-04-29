package com.kunxun.intervalalarmclock;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.widget.RelativeLayout;

public class SlideBar extends RelativeLayout {
//    private static final String TAG = "SlideBar";
//    private static final boolean DEBUG = false;

    GradientView mGradientView ;
    private int gradientViewStartX;
    private float mEventDownX;
    private float mGradientViewIndicateLeft;
    private OnTriggerListener mOnTriggerListener;
    private VelocityTracker mVelocityTracker = null;
    private int mMinVelocityXToUnlock;
    private int mMinDistanceToUnlock;
    private int mLeftAnimationDuration;
    private int mRightAnimationDuration;
//    private ObjectAnimator animLeftMoveAnimator;
//    private ObjectAnimator animRightMoveAnimator;
    private static final int MaxDistance = 800;

    public interface OnTriggerListener {
        void onTrigger();
    }

    public SlideBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        gradientViewStartX = context.getResources().
                getDimensionPixelSize(R.dimen.gradient_view_margin_left) + 8;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SlideBar);
        mMinVelocityXToUnlock = a.getInt(R.styleable.SlideBar_MinVelocityXToUnlock,2000) ;
        mMinDistanceToUnlock = a.getInt(R.styleable.SlideBar_MinDistanceToUnlock,300) ;
        mLeftAnimationDuration = a.getInt(R.styleable.SlideBar_LeftAnimationDuratioin,250) ;
        mRightAnimationDuration = a.getInt(R.styleable.SlideBar_RightAnimationDuratioin,300) ;
        a.recycle();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event){
        final int action = event.getActionMasked();
        boolean handled = false;

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }

        mVelocityTracker.addMovement(event);

        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN:
//                if (DEBUG) Log.v(TAG, "*** DOWN ***");
                handleDown(event);
                handled = true;
                break;

            case MotionEvent.ACTION_MOVE:
//                if (DEBUG) Log.v(TAG, "*** MOVE ***");
                handleMove(event);
                handled = true;
                break;

            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
//                if (DEBUG) Log.v(TAG, "*** UP ***");
                handleUp();
                handled = true;
                break;

            case MotionEvent.ACTION_CANCEL:
//                if (DEBUG) Log.v(TAG, "*** CANCEL ***");
                handled = true;
                break;

        }
        invalidate();
        return handled || super.onTouchEvent(event);
    }

    private void handleUp() {

//        Log.v(TAG, "handleUp,mIndicateLeft:" + mGradientViewIndicateLeft);
        //1. if user slide some distance, unlock
        if(mGradientViewIndicateLeft >= mMinDistanceToUnlock){
            unlockSuccess();
            return;
        }
        //2. if user slide very fast, unlock
        if(velocityTrigUnlock()){
            return;
        }
        //otherwise reset the controls
        resetControls();
    }

    private void handleMove(MotionEvent event) {

        mGradientViewIndicateLeft = event.getX() - mEventDownX + gradientViewStartX;
        if(mGradientViewIndicateLeft <= gradientViewStartX){
            mGradientViewIndicateLeft = gradientViewStartX;
        }
        mGradientView.setX(mGradientViewIndicateLeft);
    }

    private void handleDown(MotionEvent event) {
        mEventDownX = event.getX();
        if(mGradientView == null){
            mGradientView = findViewById(R.id.gradientView);
        }
        mGradientView.stopAnimatorAndChangeColor();

    }

    private void unlockSuccess() {
        mOnTriggerListener.onTrigger();
        ObjectAnimator animRightMoveAnimator = ObjectAnimator.ofFloat(mGradientView, "x",mGradientView.getX(), MaxDistance)
                .setDuration(mRightAnimationDuration);
        animRightMoveAnimator.start();
    }

    /**
     * another way to unlock, if user slide very fast
     */
    private boolean velocityTrigUnlock() {
        final VelocityTracker velocityTracker = mVelocityTracker;
        velocityTracker.computeCurrentVelocity(1000);

        int velocityX = (int) velocityTracker.getXVelocity();

//        Log.v(TAG, "velocityX:" + velocityX);

        if(velocityX > mMinVelocityXToUnlock){
            unlockSuccess();
            return true;
        }

        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
        return false;
    }

    private void resetControls() {
        mGradientView.startAnimator();
        ObjectAnimator animLeftMoveAnimator = ObjectAnimator.ofFloat(mGradientView, "x",
                mGradientView.getX(), gradientViewStartX).setDuration(mLeftAnimationDuration);
        animLeftMoveAnimator.start();
    }

    public void setOnTriggerListener(OnTriggerListener listener) {
        mOnTriggerListener = listener;
    }
}
