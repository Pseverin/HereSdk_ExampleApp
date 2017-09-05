package com.grossum.routingtestapp;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.mapping.MapObject;

/**
 * @author Severyn Parkhomenko <sparkhomenko@grossum.com>
 * @copyright (c) Grossum. (http://www.grossum.com)
 * @project RoutingTestApp
 */
public class Point {
    private GeoCoordinate geoCoordinate;
    private MapObject marker;

    public Point(GeoCoordinate geoCoordinate, MapObject marker) {
        this.geoCoordinate = geoCoordinate;
        this.marker = marker;
    }

    public Point(GeoCoordinate geoCoordinate) {
        this.geoCoordinate = geoCoordinate;
    }

    public GeoCoordinate getGeoCoordinate() {
        return geoCoordinate;
    }

    public MapObject getMarker() {
        return marker;
    }

    public void setGeoCoordinate(GeoCoordinate geoCoordinate) {
        this.geoCoordinate = geoCoordinate;
    }

    public void setMarker(MapObject marker) {
        this.marker = marker;
    }
}
