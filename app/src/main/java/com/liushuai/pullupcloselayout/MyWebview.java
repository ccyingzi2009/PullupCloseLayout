package com.liushuai.pullupcloselayout;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

/**
 * Created by liushuai on 2017/12/4.
 */

public class MyWebview extends WebView {
    public MyWebview(Context context) {
        super(context);
    }

    public MyWebview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        if (mOverscrollListener != null) {
            mOverscrollListener.overScroll(deltaX, deltaY,isTouchEvent);
        }
        return super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX, scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);
    }

    private PullUpOverScrollListerer mOverscrollListener;

    public void registerOverscrollListener (PullUpOverScrollListerer listener) {
        if (listener != null) {
            mOverscrollListener = listener;
        }
    }
    public void unRegisterOverscrollListener () {
        mOverscrollListener = null;
    }

}
