
package com.volchekDan.CampusMap.transformer;

import android.view.View;
import com.volchekDan.CampusMap.LayerTransformer;
import com.volchekDan.CampusMap.utils.Transitions;
import com.volchekDan.CampusMap.SlidingLayer;

public final class SlideJoyTransformer extends LayerTransformer {

    private final float[] mCuePoints = new float[] { 0.7f, 0.9f, 1 };

    private float[] mRotationXValues;
    private float[] mRotationYValues;

    @Override
    protected void onMeasure(View layerView, int screenSide) {

        // Rotation
        float[] rotationXY = rotationValueForScreenSide(-4.75f, screenSide);

        mRotationXValues = new float[] { 0, rotationXY[0], 0 };
        mRotationYValues = new float[] { 0, rotationXY[1], 0 };

        // Pivot
        int[] pivotPosition = pivotPositionForScreenSide(layerView, screenSide);
        layerView.setPivotX(pivotPosition[0]);
        layerView.setPivotY(pivotPosition[1]);
    }

    @Override
    public void transform(View layerView, float previewProgress, float layerProgress) {
    }

    @Override
    protected void internalTransform(View layerView, float previewProgress, float layerProgress, int screenSide) {

        float progressRatioToAnimate = Math.max(previewProgress, layerProgress);

        // Scale
        float scaleValue = Transitions.intermediateValueForCuePoints(progressRatioToAnimate,
                new float[]{0.9f, 1});
        layerView.setScaleX(scaleValue);
        layerView.setScaleY(scaleValue);

        // Rotation
        float rotationX, rotationY;

        rotationX = Transitions.intermediateValueForRange(progressRatioToAnimate, mCuePoints, mRotationXValues);
        layerView.setRotationX(rotationX);

        rotationY = Transitions.intermediateValueForRange(progressRatioToAnimate, mCuePoints, mRotationYValues);
        layerView.setRotationY(rotationY);
    }

    private float[] rotationValueForScreenSide(float value, int screenSide) {

        switch (screenSide) {

        case SlidingLayer.STICK_TO_LEFT:
            return new float[] { 0, value };

        case SlidingLayer.STICK_TO_TOP:
            return new float[] { -value, 0 };

        case SlidingLayer.STICK_TO_RIGHT:
            return new float[] { 0, -value };

        case SlidingLayer.STICK_TO_BOTTOM:
            return new float[] { value, 0 };

        default:
            return new float[] { 0, 0 };
        }
    }

    private int[] pivotPositionForScreenSide(View layerView, int screenSide) {

        switch (screenSide) {

        case SlidingLayer.STICK_TO_LEFT:
            return new int[] { 0, layerView.getMeasuredHeight() / 2 };

        case SlidingLayer.STICK_TO_TOP:
            return new int[] { layerView.getMeasuredWidth() / 2, 0 };

        case SlidingLayer.STICK_TO_RIGHT:
            return new int[] { layerView.getMeasuredWidth(), layerView.getMeasuredHeight() / 2 };

        case SlidingLayer.STICK_TO_BOTTOM:
            return new int[] { layerView.getMeasuredWidth() / 2, layerView.getMeasuredHeight() };

        default:
            return new int[] { 0, 0 };
        }
    }
}
