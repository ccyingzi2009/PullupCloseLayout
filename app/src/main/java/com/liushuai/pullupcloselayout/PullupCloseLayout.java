package com.liushuai.pullupcloselayout;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by liushuai on 2017/12/1.
 */

public class PullupCloseLayout extends ViewGroup implements PullUpOverScrollListerer {

    public final static String TAG = "PullupCloseLayout";
    public final static int SIZE_DEFAULT_HEIGHT = 100;
    public final static int SCROLL_MAX_DURATION_MS = 300;
    public final static int SCROLL_MIN_CLOSE_HEIGHT = 50;
    private boolean mCanClose = false;
    //滑动关闭页面的最大高度
    private int mPullUpViewMaxHeight;
    //滑动关闭阈值
    private int mPullUpCloseHeight;
    // 手势滑动view
    private View mTarget;
    //底部上拉关闭view
    private ViewGroup mPullUpView;
    //当前手势的状态
    private int mCurrentMotionEvent = -1;
    //开始滑动
    private boolean mIsBeingDragged;
    private int mActivePointerId = -1;
    private float mInitialDownY;
    //初始移动位置
    private float mInitialMotionY;
    //设置一个 滚动范围
    private int mTouchSlop = 2;
    //回弹动画
    private ValueAnimator mAnimator;
    public PullupCloseLayout(Context context) {
        this(context, null);
    }

    public PullupCloseLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        //为底部CloseView
        mPullUpView = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.pull_up_close, null);
        addView(mPullUpView);
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mPullUpViewMaxHeight = (int) (SIZE_DEFAULT_HEIGHT * metrics.density);
        mPullUpCloseHeight = (int) (SCROLL_MIN_CLOSE_HEIGHT * metrics.density);

    }

    @Override
    protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (mTarget == null) {
            ensureView();
        }
        if (mTarget == null) {
            return;
        }
        //WebView撑满屏幕
        mTarget.layout(getPaddingLeft(), getPaddingTop(), width - getPaddingRight(), height - getPaddingBottom());
        //CloseView在 Webview底部
        mPullUpView.layout(0, height - getPaddingBottom(), width, height - getPaddingBottom() + mPullUpView.getMeasuredHeight());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            ensureView();
        }
        if (mTarget == null) {
            return;
        }
        //设置Webview的高度撑满全屏
        mTarget.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));
        //设置CloseView 为固定高度
        mPullUpView.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mPullUpViewMaxHeight, MeasureSpec.EXACTLY));
    }

    //初始化内部滚动view， 参考v4 SwipRefreshLayout
    private void ensureView() {
        if (mTarget == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(mPullUpView)) {
                    mTarget = child;
                    //判断滚动的view为自己的实现了onScrollBy方法的 webview则注册该监听
                    if (mTarget instanceof MyWebview) {
                        MyWebview webView = (MyWebview) mTarget;
                        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);//去掉滑到底部的反馈水纹
                        webView.registerOverscrollListener(this);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        mCurrentMotionEvent = action;
        if (canChildScrollUp() || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            return false;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                final float initialDownY = getMotionEventY(ev, mActivePointerId);
                if (initialDownY == -1) {
                    return false;
                }
                //记录按下的位置
                mInitialDownY = initialDownY;
                break;
            case MotionEvent.ACTION_MOVE:
                final float y = getMotionEventY(ev, mActivePointerId);
                if (y == -1) {
                    return false;
                }
                //判断滚动的距离
                final float yDiff = mInitialDownY - y;
                //如果滚动距离>自定义的阈值，则认为需要跟随手势滚动了，此时开始拦截。
                if (yDiff > mTouchSlop && !mIsBeingDragged) {
                    mInitialMotionY = mInitialDownY + mTouchSlop;
                    mIsBeingDragged = true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = -1;
                break;
        }

        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (canChildScrollUp()) {
            return false;
        }
        final int action = MotionEventCompat.getActionMasked(ev);
        int pointerIndex;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                break;
            case MotionEvent.ACTION_MOVE: {
                pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);
                //设置滚动的阻力 0.5倍系数
                final int overscrollTop = (int) ((mInitialMotionY - y) * 0.5);
                if (mIsBeingDragged) {//消费滑动事件
                    if (overscrollTop > 0) {
                        moveSpinner(overscrollTop);
                    } else {
                        return false;
                    }
                }
                break;
            }

            case MotionEventCompat.ACTION_POINTER_DOWN: {
                pointerIndex = MotionEventCompat.getActionIndex(ev);
                if (pointerIndex < 0) {
                    Log.e(TAG, "Got ACTION_POINTER_DOWN event but have an invalid action index.");
                    return false;
                }
                mActivePointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                Log.i(TAG, "ACTION_UP");
                break;
            case MotionEvent.ACTION_UP:
                pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                mIsBeingDragged = false;
                mActivePointerId = -1;
                finishSpinner();
                Log.i(TAG, "ACTION_UP");
                break;
        }
        return true;
    }

    //手势抬起，开始回弹动画并回调是否关闭页面
    private void finishSpinner() {
        if (getScrollY() > 0) {
            scrollBackAnimator(getScrollY());
        }
        //上拉回调。
        if (mPullUpListener != null) {
            mPullUpListener.pullUp(mCanClose);
        }

    }

    // 手势移动，滚动当前view，并切换底部关闭按钮的状态
    private void moveSpinner(int overscrollTop) {
        scrollBy(0, overscrollTop - getScrollY());
        updatePullUpViewState();
    }

    //回弹动画
    private void scrollBackAnimator(final int y) {
        Log.i(TAG, "scrollBackAnimator y =" + y);
        if (y == 0) {
            return;
        }
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
        mAnimator = ValueAnimator.ofFloat(0, 1);
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float f = (float) animation.getAnimatedValue();
                scrollTo(0, (int) (y * (1 - f)));
            }

        });

        //long duration = SCROLL_MAX_DURATION_MS * y / mPullUpViewMaxHeight;
        mAnimator.setDuration(SCROLL_MAX_DURATION_MS);
        mAnimator.start();
    }

    /**
     * 判断childView 是否已经不能正向滑动
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    private boolean canChildScrollUp() {
        return ViewCompat.canScrollVertically(mTarget, 1);
    }

    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
        if (index < 0) {
            return -1;
        }
        return MotionEventCompat.getY(ev, index);
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    //切换底部按钮以及文字状态
    private void updatePullUpViewState() {
        if (getScrollY() > mPullUpCloseHeight) {
            if (!mCanClose && mPullUpView != null) {
                mCanClose = true;
                ((ImageView) mPullUpView.findViewById(R.id.icon)).setImageLevel(1);
                ((TextView) mPullUpView.findViewById(R.id.text)).setText("松手关闭");
            }
        } else {
            if (mCanClose && mPullUpView != null) {
                mCanClose = false;
                ((ImageView) mPullUpView.findViewById(R.id.icon)).setImageLevel(0);
                ((TextView) mPullUpView.findViewById(R.id.text)).setText("松手关闭");
            }
        }
    }

    //===========上拉关闭回调==================
    public PullUpListener mPullUpListener;

    @Override
    public void overScroll(int deltaX, int deltaY, boolean isTouchEvent) {
        if (!mIsBeingDragged && !canChildScrollUp() && deltaY > mTouchSlop && mCurrentMotionEvent != MotionEvent.ACTION_MOVE) {
            //1.5 倍惯性距离, 且最大滚动距离为滑动关闭的阈值
            deltaY = Math.min((int)(deltaY * 1.5), mPullUpCloseHeight);
            scrollBackAnimator((int) (deltaY * 1.5));
        }
    }


    public interface PullUpListener {
        void pullUp(boolean close);
    }

    public void registerPullUpListener(PullUpListener listener) {
        if (listener != null) {
            mPullUpListener = listener;
        }
    }
}
