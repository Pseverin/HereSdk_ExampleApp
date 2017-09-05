package com.grossum.routingtestapp;

import android.graphics.PointF;
import android.support.annotation.Nullable;

import com.annimon.stream.Optional;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.ViewObject;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapGesture;

import java.util.List;

/**
 * @author Severyn Parkhomenko <sparkhomenko@grossum.com>
 * @copyright (c) Grossum. (http://www.grossum.com)
 * @project RoutingTestApp
 */
public abstract class OnMapClickListener implements MapGesture.OnGestureListener {

    private Optional<Map> map;

    public OnMapClickListener(@Nullable Map map) {
        this.map = Optional.ofNullable(map);
    }

    @Override
    public void onPanStart() {

    }

    @Override
    public void onPanEnd() {

    }

    @Override
    public void onMultiFingerManipulationStart() {

    }

    @Override
    public void onMultiFingerManipulationEnd() {

    }

    @Override
    public boolean onMapObjectsSelected(List<ViewObject> list) {
        return false;
    }

    @Override
    public boolean onTapEvent(PointF pointF) {
        map.ifPresent(m -> onMapClick(pointF, m.pixelToGeo(pointF)));
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(PointF pointF) {
        return false;
    }

    @Override
    public void onPinchLocked() {

    }

    @Override
    public boolean onPinchZoomEvent(float v, PointF pointF) {
        return false;
    }

    @Override
    public void onRotateLocked() {

    }

    @Override
    public boolean onRotateEvent(float v) {
        return false;
    }

    @Override
    public boolean onTiltEvent(float v) {
        return false;
    }

    @Override
    public boolean onLongPressEvent(PointF pointF) {
        return false;
    }

    @Override
    public void onLongPressRelease() {

    }

    @Override
    public boolean onTwoFingerTapEvent(PointF pointF) {
        return false;
    }

    public abstract void onMapClick(PointF pointF, GeoCoordinate geoCoordinate);
}
