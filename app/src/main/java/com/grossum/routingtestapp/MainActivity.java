package com.grossum.routingtestapp;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.grossum.routingtestapp.utils.FileManager;
import com.grossum.routingtestapp.utils.PermissionsUtils;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositionSimulator;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.Maneuver;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RoutingError;
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.GeocodeRequest;
import com.here.android.mpa.search.Location;
import com.here.android.mpa.search.ResultListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.et_search)
    EditText etSearch;

    @BindView(R.id.btn_add)
    View btnAdd;

    @BindView(R.id.btn_car)
    View btnCar;
    @BindView(R.id.btn_truck)
    View btnTruck;
    @BindView(R.id.btn_navigate)
    View btnNavigate;


    public final static String TAG = "TAG -> ";
    public PositioningManager posManager;


    private Optional<GeoCoordinate> myCurrentPosition = Optional.empty();
    private MapFragment mapFragment;
    private MapRoute currentMapRoute;
    private Type type = Type.CAR;
    private boolean isNavigation;
    private boolean paused;
    private NavigationManager navigationManager;
    private List<Point> routePoints = new ArrayList<>();
    private Point addPoint;


    public enum Type {
        CAR(RouteOptions.TransportMode.CAR), TRUCK(RouteOptions.TransportMode.TRUCK);

        public RouteOptions.TransportMode transportMode;

        Type(RouteOptions.TransportMode transportMode) {
            this.transportMode = transportMode;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        PermissionsUtils.requestAllPermissions(this);

        mapFragment = (MapFragment)
            getFragmentManager().findFragmentById(R.id.mapfragment);

        mapFragment.init(error -> {
            if (error == OnEngineInitListener.Error.NONE) {
                posManager = PositioningManager.getInstance();
                navigationManager = NavigationManager.getInstance();

                mockRoute();
                setupMapClickListener();
                startPositioning();
            } else {
                Log.e(TAG, error.toString());
            }
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString();
                if (!query.isEmpty()) {

                    ResultListener<List<Location>> listener = new GeocodeListener();
                    GeocodeRequest request = new GeocodeRequest(query).setSearchArea(myCurrentPosition.orElse(new GeoCoordinate(0, 0)), 5000);

                    if (request.execute(listener) != ErrorCode.NONE) {
                        // Handle request error
                    }
                    return false;
                } else {
                    return true;
                }
            }
            return false;
        });
        updateAccordingToType();
    }

    private void startPositioning() {
        posManager.start(
            PositioningManager.LocationMethod.GPS_NETWORK);
    }

    private void setupMapClickListener() {
        mapFragment.getMapGesture().addOnGestureListener(new OnMapClickListener(getMap()) {
            @Override
            public void onMapClick(PointF pointF, GeoCoordinate geoCoordinate) {
                if (addPoint == null) {
                    addPoint = new Point(geoCoordinate);
                } else {
                    addPoint.setGeoCoordinate(geoCoordinate);
                }

                updateMapObjectsAndAddState();
            }
        }, Integer.MAX_VALUE, true);

        posManager.addListener(
            new WeakReference<>(positionListener));
        mapFragment.getPositionIndicator().setVisible(true);

        mapFragment.setMapMarkerDragListener(onDragListener);
        navigationManager.addNewInstructionEventListener(new WeakReference<>(newInstructionEventListener));
        getMap().addSchemeChangedListener(onSchemeChangedListener);
    }

    private void updateMapObjectsAndAddState() {
        runOnUiThread(() -> {
            Stream.of(routePoints).forEach(point -> {
                getMap().removeMapObject(point.getMarker());
                point.setMarker(null);
            });

            if (addPoint != null && addPoint.getMarker() != null) {
                getMap().removeMapObject(addPoint.getMarker());
                addPoint.setMarker(null);
            }
            if (addPoint != null && addPoint.getGeoCoordinate() != null) {
                MapMarker coordinateToAddMarker = new MapMarker();
                coordinateToAddMarker.setCoordinate(addPoint.getGeoCoordinate());
                coordinateToAddMarker.setDraggable(true);
                getMap().addMapObject(coordinateToAddMarker);
                getMap().setCenter(addPoint.getGeoCoordinate(), Map.Animation.LINEAR);
                addPoint.setMarker(coordinateToAddMarker);
            }

            for (int i = 0; i < routePoints.size(); i++) {
                MapMarker mapMarker = new MapMarker();
                mapMarker.setCoordinate(routePoints.get(i).getGeoCoordinate());
                mapMarker.setDraggable(true);

                Image img = new Image();
                Bitmap bitmap;

                int markerSize = getResources().getDimensionPixelSize(R.dimen.finger_size_half);
                if (i == 0) {
                    bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_start), markerSize, markerSize, false);
                } else if (i == routePoints.size() - 1) {
                    bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_finish), markerSize, markerSize, false);
                } else {
                    bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_point), markerSize, markerSize, false);
                }

                img.setBitmap(bitmap);
                mapMarker.setIcon(img);

                getMap().addMapObject(mapMarker);
                routePoints.get(i).setMarker(mapMarker);
            }

            btnAdd.setVisibility(addPoint != null && addPoint.getGeoCoordinate() != null ? View.VISIBLE : View.GONE);
        });
    }

    private void buildRoute() {
        if (routePoints.size() < 2) {
            return;
        }
        if (currentMapRoute != null) {
            getMap().removeMapObject(currentMapRoute);
        }

        CoreRouter router = new CoreRouter();
        RoutePlan routePlan = new RoutePlan();
        Stream.of(routePoints).forEach(point -> routePlan.addWaypoint(new RouteWaypoint(new GeoCoordinate(point.getGeoCoordinate()))));

        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(type.transportMode);
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);

        routePlan.setRouteOptions(routeOptions);

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setIndeterminate(true);
        dialog.show();
        router.calculateRoute(routePlan, new CoreRouter.Listener() {
            @Override
            public void onCalculateRouteFinished(List<RouteResult> list, RoutingError routingError) {
                dialog.dismiss();
                if (routingError == RoutingError.NONE) {
                    // Render the route on the map
                    currentMapRoute = new MapRoute(list.get(0).getRoute());
                    getMap().addMapObject(currentMapRoute);

                    btnNavigate.setVisibility(View.VISIBLE);
                    zoomToRoute();
                } else {
                    // Display a message indicating route calculation failure
                }
            }

            @Override
            public void onProgress(int i) {
                dialog.setMessage("Progress... " + i + "%");
            }
        });

    }

    private void zoomToRoute() {
        if (currentMapRoute != null) {
            getMap().zoomTo(currentMapRoute.getRoute().getBoundingBox(), Map.Animation.LINEAR, Map.MOVE_PRESERVE_ORIENTATION);
        }
    }

    @OnClick({R.id.btn_add, R.id.btn_car, R.id.btn_truck, R.id.btn_navigate})
    public void onBtnClick(View v) {
        switch (v.getId()) {
            case R.id.btn_add:
                routePoints.add(new Point(addPoint.getGeoCoordinate()));
                addPoint.setGeoCoordinate(null);

                updateMapObjectsAndAddState();
                buildRoute();
                etSearch.setText(null);
                break;
            case R.id.btn_car:
                if (type != Type.CAR) {
                    type = Type.CAR;
                    updateAccordingToType();
                }
                break;
            case R.id.btn_truck:
                if (type != Type.TRUCK) {
                    type = Type.TRUCK;
                    updateAccordingToType();
                }
                break;
            case R.id.btn_navigate:
                isNavigation = !isNavigation;
                updateNavigationState();
                break;
        }
    }

    private void updateNavigationState() {
        btnNavigate.setBackgroundColor(isNavigation ? Color.GREEN : Color.WHITE);

        if (isNavigation) {
            getMap().setMapScheme(Map.Scheme.TRUCKNAV_DAY);

            navigationManager.setMap(getMap());

            PositionSimulator positionSimulator = new PositionSimulator();
            positionSimulator.startPlayback(FileManager.creteFakeGPSRoute(this));
            posManager.setLogType(EnumSet.copyOf(new ArrayList<PositioningManager.LogType>() {{
                add(PositioningManager.LogType.DATA_SOURCE);
            }}));

            NavigationManager.Error error = navigationManager.startNavigation(currentMapRoute.getRoute());

            myCurrentPosition.ifPresent(pos -> {
                getMap().setCenter(pos, Map.Animation.LINEAR);
                double maxZoom = getMap().getMaxZoomLevel();
                double minZoom = getMap().getMinZoomLevel();
                getMap().setZoomLevel((maxZoom + minZoom) * 5 / 6);
            });
        } else {
            getMap().setMapScheme(Map.Scheme.NORMAL_DAY);
            navigationManager.stop();
            zoomToRoute();
        }
    }

    private void updateAccordingToType() {
        btnCar.setBackgroundColor(type == Type.CAR ? Color.GREEN : Color.WHITE);
        btnTruck.setBackgroundColor(type == Type.TRUCK ? Color.GREEN : Color.WHITE);

        buildRoute();
    }

    private class GeocodeListener implements ResultListener<List<Location>> {
        @Override
        public void onCompleted(List<Location> data, ErrorCode error) {
            if (error != ErrorCode.NONE) {
                // Handle error
            } else if (data.size() > 0) {
                if (addPoint == null) {
                    addPoint = new Point(data.get(0).getCoordinate());
                } else {
                    addPoint.setGeoCoordinate(data.get(0).getCoordinate());
                }
                updateMapObjectsAndAddState();
            }
        }
    }

    private PositioningManager.OnPositionChangedListener positionListener = new
        PositioningManager.OnPositionChangedListener() {

            public void onPositionUpdated(PositioningManager.LocationMethod method,
                                          GeoPosition position, boolean isMapMatched) {
                myCurrentPosition = Optional.of(position.getCoordinate());
            }

            public void onPositionFixChanged(PositioningManager.LocationMethod method,
                                             PositioningManager.LocationStatus status) {
            }
        };

    private Map.OnSchemeChangedListener onSchemeChangedListener = mapScheme -> {
        Log.i(TAG, "Map scheme changed to " + mapScheme);
    };

    NavigationManager.NewInstructionEventListener newInstructionEventListener = new NavigationManager.NewInstructionEventListener() {
        @Override
        public void onNewInstructionEvent() {
            super.onNewInstructionEvent();

            Maneuver maneuver = navigationManager.getNextManeuver();
            if (maneuver != null) {
                if (maneuver.getAction() == Maneuver.Action.END) {
                    //notify the user that the route is complete
                }

                //display current or next road information
                //display maneuver.getDistanceToNextManeuver()
            }
        }
    };


    MapMarker.OnDragListener onDragListener = new MapMarker.OnDragListener() {
        @Override
        public void onMarkerDrag(MapMarker mapMarker) {

        }

        @Override
        public void onMarkerDragEnd(MapMarker mapMarker) {
            Stream.of(routePoints).filter(point -> point.getMarker().equals(mapMarker))
                .findFirst()
                .ifPresent(point -> {
                    point.setGeoCoordinate(mapMarker.getCoordinate());
                    buildRoute();
                });
            if (addPoint != null && addPoint.getMarker() != null && addPoint.getMarker().equals(mapMarker)) {
                addPoint.setGeoCoordinate(mapMarker.getCoordinate());
            }
            updateMapObjectsAndAddState();
        }

        @Override
        public void onMarkerDragStart(MapMarker mapMarker) {

        }
    };

    public void onResume() {
        super.onResume();
        paused = false;
        if (posManager != null) {
            startPositioning();
        }
    }

    public void onPause() {
        if (posManager != null) {
            posManager.stop();
        }
        super.onPause();
        paused = true;
    }

    public void onDestroy() {
        if (posManager != null) {
            // Cleanup
            posManager.removeListener(
                positionListener);
        }
        super.onDestroy();
    }


    private Map getMap() {
        return mapFragment.getMap();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionsUtils.onRequestPermissionsResult(this, requestCode, permissions, grantResults, true);
    }

    private void mockRoute() {
        routePoints.add(new Point(new GeoCoordinate(39.845096189528704, -86.08396218158305)));
        routePoints.add(new Point(new GeoCoordinate(39.76037451066077, -86.08232527971268)));
        routePoints.add(new Point(new GeoCoordinate(39.75209872238338, -86.03698312304914)));
        routePoints.add(new Point(new GeoCoordinate(39.71890814602375, -86.05102180503309)));
        routePoints.add(new Point(new GeoCoordinate(39.69421179033816, -86.14889376796782)));
        routePoints.add(new Point(new GeoCoordinate(39.69276222400367, -86.1526056099683)));
        routePoints.add(new Point(new GeoCoordinate(39.688103310763836, -86.15635592490435)));
        routePoints.add(new Point(new GeoCoordinate(39.40982638858259, -86.15521917119622)));
        routePoints.add(new Point(new GeoCoordinate(39.407930821180344, -85.994678651914)));
        routePoints.add(new Point(new GeoCoordinate(39.29768783971667, -85.96758104860783)));

        routePoints.add(new Point(new GeoCoordinate(38.958242973312736, -85.83722434937954)));
        routePoints.add(new Point(new GeoCoordinate(38.95695375278592, -85.83463777787983)));
        routePoints.add(new Point(new GeoCoordinate(38.80357917398214, -85.83979767747223)));
        routePoints.add(new Point(new GeoCoordinate(38.7192237842828, -85.78186162747443)));
        routePoints.add(new Point(new GeoCoordinate(38.617277881130576, -85.77563211321831)));
        routePoints.add(new Point(new GeoCoordinate(38.54449354112148, -85.7678993884474)));
        routePoints.add(new Point(new GeoCoordinate(38.391432613134384, -85.75409632176161)));
        routePoints.add(new Point(new GeoCoordinate(38.37157890200615, -85.74640961363912)));
        routePoints.add(new Point(new GeoCoordinate(38.36343923583627, -85.75506719760597)));
        routePoints.add(new Point(new GeoCoordinate(38.345896415412426, -85.74373905546963)));
        routePoints.add(new Point(new GeoCoordinate(38.222097381949425, -85.48810643143952)));

        updateMapObjectsAndAddState();
        buildRoute();
    }
}
