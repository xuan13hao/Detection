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

import static com.votors.runningx.MapActivity.EXTRA_GpsRec;

 public class MapActivity extends Activity implements SensorEventListener {

    public final static String EXTRA_MESSAGE = "com.votors.runningx.MESSAGE";
    private static final String BC_INTENT = "com.votors.runningx.BroadcastReceiver.location";
    public final static String EXTRA_GpsRec = "com.votors.runningx.GpsRec";

    private SensorManager sensorManager;
    private Sensor sensor;
    private float sensor_x;
    private float sensor_y;
    private float sensor_z;
    // The Map Object
    private GoogleMap mMap;

    public static TrafficSensor ts=new TrafficSensor();
    public static final String TAG = "MapActivity";

    private final LocationReceiver mReceiver = new LocationReceiver();
    private final IntentFilter intentFilter = new IntentFilter(BC_INTENT);
    ArrayList<GpsRec> locations = null;
    float curr_dist = 0;
    float total_dist = 0;
    double center_lat = 0;
    double center_lng = 0;

    int movePointCnt = 0;

    LatLngBounds.Builder builder = new LatLngBounds.Builder();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Conf.init(getApplicationContext());

        setContentView(R.layout.main_map);

        locations = (ArrayList<GpsRec>)getIntent().getSerializableExtra(EXTRA_MESSAGE);
        for (GpsRec r: locations) total_dist += r.distance;

        registerReceiver(mReceiver, intentFilter);
        //Sensor Test code
        Log.i(TAG, "location numbler: " + locations.size());
        Log.i(TAG, "sensor_x: " + sensor_x);
        Log.i(TAG, "sensor_x: " + sensor_y);
        Log.i(TAG, "sensor_x: " + sensor_z);
        //Log.i(TAG, "sensor_x: " + sensor_x);
        // Get Map Object
        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        getSensorManager();
        //System.out.print(mMap.toString());
        final PolylineOptions polylines = new PolylineOptions();
        System.out.print(sensor_x);
       // this.run();
        //Sensor Judgement



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
       // polylines.color(Color.BLUE).width(10);
        //mMap.addPolyline(polylines);

        if(ts.getX()<15||ts.getY()<15||ts.getZ()<15)
        {
            polylines.color(Color.BLUE).width(10);
            mMap.addPolyline(polylines);
           // mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(center_lat / locations.size(), center_lng / locations.size())));
           // mMap.moveCamera(CameraUpdateFactory.zoomTo(15));

        }else
        {
            polylines.color(Color.RED).width(15);
            mMap.addPolyline(polylines);

        }

        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(center_lat / locations.size(), center_lng / locations.size())));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15));

            // see http://stackoverflow.com/questions/16367556/cameraupdatefactory-newlatlngbounds-is-not-workinf-all-the-time
            mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    if (locations.size() > 0) {
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
        /*
        else
            {
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

                // see http://stackoverflow.com/questions/16367556/cameraupdatefactory-newlatlngbounds-is-not-workinf-all-the-time
                mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        if (locations.size() > 0) {
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
            }*/


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


    /*
    ** sensor code
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        sensor_x = event.values[0];
        sensor_y = event.values[0];
        sensor_z = event.values[0];
        Log.d("MySensorApp", "Raw accelerometer values : " + sensor_x + " : " + sensor_y + " : " + sensor_z);
        if(sensor_x>15|sensor_y>15||sensor_z>15) {
            TrafficSensor ts = new TrafficSensor(sensor_x, sensor_y, sensor_z);
        }
        else
            {
                TrafficSensor ts = new TrafficSensor(sensor_x, sensor_y, sensor_z);
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
    protected void onResume() {
        super.onResume();
        if(sensorManager != null){
            //一般在Resume方法中注册
            /**
             * 第三个参数决定传感器信息更新速度
             * SensorManager.SENSOR_DELAY_NORMAL:一般
             * SENSOR_DELAY_FASTEST:最快
             * SENSOR_DELAY_GAME:比较快,适合游戏
             * SENSOR_DELAY_UI:慢
             */
            sensorManager.registerListener(this,sensor,SensorManager.SENSOR_DELAY_NORMAL);
        }
    }
    protected void onPause() {
        super.onPause();
        if(sensorManager != null){
            //解除注册
            sensorManager.unregisterListener(this,sensor);
        }
    }
/*
     @Override
     public void run() {
         PolylineOptions polylines = new PolylineOptions();
         if(ts.getX()<15||ts.getY()<15||ts.getZ()<15)
         {
             polylines.color(Color.RED).width(10);
             mMap.addPolyline(polylines);
             // mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(center_lat / locations.size(), center_lng / locations.size())));
             // mMap.moveCamera(CameraUpdateFactory.zoomTo(15));

         }else
         {
             polylines.color(Color.BLACK).width(10);
             mMap.addPolyline(polylines);

         }
         mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(center_lat / locations.size(), center_lng / locations.size())));
         mMap.moveCamera(CameraUpdateFactory.zoomTo(15));

     }
*/

     public class LocationReceiver extends BroadcastReceiver {
        private final String TAG = "LocationReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            GpsRec rec = (GpsRec)intent.getSerializableExtra(EXTRA_GpsRec);
            Log.i(TAG, "LocationReceiver, location " + rec.toString());

            final PolylineOptions polylines = new PolylineOptions();
            GpsRec last;
            if(ts.getX()<15||ts.getY()<15||ts.getZ()<15)
            {
            polylines.color(Color.RED).width(15);
            }
            else
                {
                    polylines.color(Color.BLUE).width(10);
                }
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
