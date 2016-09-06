package com.volchekDan.CampusMap;

import android.view.View;


public abstract class LayerTransformer {


    protected void onMeasure(View layerView, int screenSide) {
    }

    protected void internalTransform(View layerView, float previewProgress, float layerProgress, int screenSide) {
        transform(layerView, previewProgress, layerProgress);
    }

    public abstract void transform(View layerView, float previewProgress, float layerProgress);
}
