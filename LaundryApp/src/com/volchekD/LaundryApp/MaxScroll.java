package com.volchekD.LaundryApp;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ScrollView;

public class MaxScroll extends ScrollView {

    public static int WITHOUT_MAX_HEIGHT_VALUE = -1;

    private int maxHeight = 930;

    public MaxScroll(Context context) {
        super(context);
    }

    public MaxScroll(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MaxScroll(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        try {
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);
            if (maxHeight != WITHOUT_MAX_HEIGHT_VALUE
                    && heightSize > maxHeight) {
                heightSize = maxHeight;
            }
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.AT_MOST);
            getLayoutParams().height = heightSize;
        } catch (Exception e) {
           Log.e("Error forcing height", e.getMessage());
        } finally {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

}
