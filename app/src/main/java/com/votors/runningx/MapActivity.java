 package com.votors.runningx;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class MapActivity extends Activity implements SensorEventListener{

    public final static String EXTRA_MESSAGE = "com.votors.runningx.MESSAGE";
    private static final String BC_INTENT = "com.votors.runningx.BroadcastReceiver.location";
    public final static String EXTRA_GpsRec = "GpsRec";

    // The Map Object
    private GoogleMap mMap;

    public static final String TAG = "MapActivity";

    private final LocationReceiver mReceiver = new LocationReceiver();
    private final IntentFilter intentFilter = new IntentFilter(BC_INTENT);
    ArrayList<GpsRec> locations = null;
    float curr_dist = 0;
    float total_dist = 0;
    double center_lat = 0;
    double center_lng = 0;

    int movePointCnt = 0;
    private SensorManager sensorManager;
    //sensor reminder
    int sensor_x;
    int sensor_y;
    int sensor_z;
    private Sensor sensor;
    LatLngBounds.Builder builder = new LatLngBounds.Builder();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Conf.init(getApplicationContext());
        getSensorManager();
        setContentView(R.layout.main_map);
        locations = (ArrayList<GpsRec>)getIntent().getSerializableExtra(EXTRA_MESSAGE);
        for (GpsRec r: locations) total_dist += r.distance;

        registerReceiver(mReceiver, intentFilter);

        Log.i(TAG, "location numbler: " + locations.size());

        // Get Map Object
        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

        final PolylineOptions polylines = new PolylineOptions();
        if(sensor_x<3&&sensor_y<3&&sensor_z<3) {
            polylines.color(Color.BLUE).width(10);

            if (null != mMap && locations != null) {
                // Add a marker for every earthquake
                int cnt = 0;
                // If already run a long way, distance between mark should be larger.
                float mark_distance = Conf.getMarkDistance(total_dist);
                for (GpsRec rec : locations) {
                    Log.i(TAG, rec.toString());
                    cnt++;
                    if (cnt == 1 || cnt == locations.size() || (int) Math.floor(curr_dist / mark_distance) != (int) Math.floor((curr_dist + rec.distance) / mark_distance)) {
                        // Add a new marker
                        MarkerOptions mk = new MarkerOptions()
                                .position(new LatLng(rec.getLat(), rec.getLng()));

                        // Set the title of the Marker's information window
                        if (cnt == 1) {
                            mk.title(String.valueOf(getResources().getString(R.string.start)));
                            mk.icon(BitmapDescriptorFactory.defaultMarker(getMarkerColor(rec.speed)));
                            mMap.addMarker(mk).showInfoWindow();
                        } else if (cnt == locations.size()) {
                            mk.title(String.format("[%s] %.1f%s,%.1f%s",
                                    getResources().getString(R.string.end),
                                    Conf.getDistance(curr_dist + rec.distance),
                                    Conf.getDistanceUnit(),
                                    Conf.getSpeed((curr_dist + rec.distance) / (rec.date.getTime() - locations.get(0).date.getTime()) * 1000, 0),
                                    Conf.getSpeedUnit()));
                            mk.icon(BitmapDescriptorFactory.defaultMarker(getMarkerColor(rec.speed)));
                            mMap.addMarker(mk).showInfoWindow();
                        } else {
                            mk.title(String.format("%.1f%s,%.1f%s",
                                    Conf.getDistance((curr_dist + rec.distance)),
                                    Conf.getDistanceUnit(),
                                    Conf.getSpeed(rec.speed, 0),
                                    Conf.getSpeedUnit()));
                            mk.icon(BitmapDescriptorFactory.defaultMarker(getMarkerColor(rec.speed)));
                            mMap.addMarker(mk).showInfoWindow();
                        }

                        // Set the color for the Marker
                        builder.include(mk.getPosition());
                    }
                    curr_dist += rec.distance;
                    center_lat += rec.getLat();
                    center_lng += rec.getLng();

                    polylines.add(new LatLng(rec.getLat(), rec.getLng()));
                }
            }

            // Center the map, draw the path
            // Should compute map center from the actual data
            mMap.addPolyline(polylines);


            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(center_lat / locations.size(), center_lng / locations.size())));
            mMap.moveCamera(CameraUpdateFactory.zoomTo(15));
        }else {
            polylines.color(Color.RED).width(10);
            if (null != mMap && locations != null) {
                // Add a marker for every earthquake
                int cnt = 0;
                // If already run a long way, distance between mark should be larger.
                float mark_distance = Conf.getMarkDistance(total_dist);
                for (GpsRec rec : locations) {
                    Log.i(TAG, rec.toString());
                    cnt++;
                    if (cnt == 1 || cnt == locations.size() || (int) Math.floor(curr_dist / mark_distance) != (int) Math.floor((curr_dist + rec.distance) / mark_distance)) {
                        // Add a new marker
                        MarkerOptions mk = new MarkerOptions()
                                .position(new LatLng(rec.getLat(), rec.getLng()));

                        // Set the title of the Marker's information window
                        if (cnt == 1) {
                            mk.title(String.valueOf(getResources().getString(R.string.start)));
                            mk.icon(BitmapDescriptorFactory.defaultMarker(getMarkerColor(rec.speed)));
                            mMap.addMarker(mk).showInfoWindow();
                        } else if (cnt == locations.size()) {
                            mk.title(String.format("[%s] %.1f%s,%.1f%s",
                                    getResources().getString(R.string.end),
                                    Conf.getDistance(curr_dist + rec.distance),
                                    Conf.getDistanceUnit(),
                                    Conf.getSpeed((curr_dist + rec.distance) / (rec.date.getTime() - locations.get(0).date.getTime()) * 1000, 0),
                                    Conf.getSpeedUnit()));
                            mk.icon(BitmapDescriptorFactory.defaultMarker(getMarkerColor(rec.speed)));
                            mMap.addMarker(mk).showInfoWindow();
                        } else {
                            mk.title(String.format("%.1f%s,%.1f%s",
                                    Conf.getDistance((curr_dist + rec.distance)),
                                    Conf.getDistanceUnit(),
                                    Conf.getSpeed(rec.speed, 0),
                                    Conf.getSpeedUnit()));
                            mk.icon(BitmapDescriptorFactory.defaultMarker(getMarkerColor(rec.speed)));
                            mMap.addMarker(mk).showInfoWindow();
                        }

                        // Set the color for the Marker
                        builder.include(mk.getPosition());
                    }
                    curr_dist += rec.distance;
                    center_lat += rec.getLat();
                    center_lng += rec.getLng();

                    polylines.add(new LatLng(rec.getLat(), rec.getLng()));
                }
            }

            // Center the map, draw the path
            // Should compute map center from the actual data
            mMap.addPolyline(polylines);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(center_lat / locations.size(), center_lng / locations.size())));
            mMap.moveCamera(CameraUpdateFactory.zoomTo(15));

        }


        // see http://stackoverflow.com/questions/16367556/cameraupdatefactory-newlatlngbounds-is-not-workinf-all-the-time
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                if (locations.size()>0) {
                    LatLngBounds bounds = adjustBoundsForMaxZoomLevel(builder.build());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
                }
            }
        });

        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition arg0) {
//                LatLngBounds bounds = adjustBoundsForMaxZoomLevel(builder.build());
//                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 10));
            }
        });

    }

    @Override
    public void onPostCreate (Bundle bundle) {
        super.onPostCreate(bundle);
    }

    /**
     * see http://stackoverflow.com/questions/15700808/setting-max-zoom-level-in-google-maps-android-api-v2
     * @param bounds
     * @return
     */
    private LatLngBounds adjustBoundsForMaxZoomLevel(LatLngBounds bounds) {
        LatLng sw = bounds.southwest;
        LatLng ne = bounds.northeast;
        double deltaLat = Math.abs(sw.latitude - ne.latitude);
        double deltaLon = Math.abs(sw.longitude - ne.longitude);

        final double zoomN = 0.005; // minimum zoom coefficient
        if (deltaLat < zoomN) {
            sw = new LatLng(sw.latitude - (zoomN - deltaLat / 2), sw.longitude);
            ne = new LatLng(ne.latitude + (zoomN - deltaLat / 2), ne.longitude);
            bounds = new LatLngBounds(sw, ne);
        }
        else if (deltaLon < zoomN) {
            sw = new LatLng(sw.latitude, sw.longitude - (zoomN - deltaLon / 2));
            ne = new LatLng(ne.latitude, ne.longitude + (zoomN - deltaLon / 2));
            bounds = new LatLngBounds(sw, ne);
        }

        return bounds;
    }


    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    // hue: [0,360)
    private float getMarkerColor(float speed) {
        float hue = 0f;
        if (speed < 1) {
            hue = 1;
        } else if (speed > 9) {
            hue = 9;
        }else{
            hue = speed;
        }

        return (36 * hue);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[0];
        float z = event.values[0];
        if(x>30 || y>30 || z>30){
            //sensor > 30，显示为路面坑洼
           sensor_x=3;sensor_y=3;sensor_z=3;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    public void getSensorManager() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        /**
         * 传入的参数决定传感器的类型
         * Senor.TYPE_ACCELEROMETER: 加速度传感器
         * Senor.TYPE_LIGHT:光照传感器
         * Senor.TYPE_GRAVITY:重力传感器
         * SenorManager.getOrientation(); //方向传感器
         */
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }



    public class LocationReceiver extends BroadcastReceiver {
        private final String TAG = "LocationReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            GpsRec rec = (GpsRec)intent.getSerializableExtra(EXTRA_GpsRec);
            Log.i(TAG, "LocationReceiver, location " + rec.toString());

            final PolylineOptions polylines = new PolylineOptions();
            GpsRec last;
            polylines.color(Color.BLUE).width(10);
            if (locations.size()>0) {
                last = locations.get(locations.size() - 1);
                polylines.add(new LatLng(last.getLat(),last.getLng()));
            }

            // If already run a long way, distance between mark should be larger.
            float mark_distance = Conf.getMarkDistance(curr_dist);
            if (movePointCnt == 0 || (int)Math.floor(curr_dist / mark_distance) !=  (int)Math.floor((curr_dist +rec.distance)/ mark_distance)) {
                // Add a new marker
                MarkerOptions mk = new MarkerOptions()
                        .position(new LatLng(rec.getLat(), rec.getLng()));

                // Set the title of the Marker's information window
                //mk.title(String.format("%.0fm,%.1fm/s",Math.floor(curr_dist + rec.distance),rec.speed));
                mk.title(String.format("%.1f%s,%.1f%s",
                        Conf.getDistance(curr_dist + rec.distance),
                        Conf.getDistanceUnit(),
                        Conf.getSpeed(rec.speed, 0),
                        Conf.getSpeedUnit()));

                // Set the color for the Marker
                mk.icon(BitmapDescriptorFactory.defaultMarker(getMarkerColor(rec.speed)));
                mMap.addMarker(mk).showInfoWindow();
                builder.include(mk.getPosition());
            }
            movePointCnt++;
            curr_dist += rec.distance;
            total_dist += rec.distance;
            center_lat += rec.getLat();
            center_lng += rec.getLng();
            locations.add(rec);

            polylines.add(new LatLng(rec.getLat(), rec.getLng()));
            mMap.addPolyline(polylines);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(rec.getLat(), rec.getLng())));
        }

    }
}
