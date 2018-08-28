package com.kpo.mcu;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.kpo.mcu.Util.MapFixUtil;
import com.kpo.mcu.gpssmooth.GeoTrackFilter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private final String mTag = "KPOCOM_GPS";
    Button bt;
    private TextView mGpSNmea, mfixStatusTv;
    TextView tv;
    boolean start = false;
    File SavaFile = null;
    RandomAccessFile raf = null;
    Timer timer = null;
    SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA);

    /*GPSConstantPermission*/
    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;

    /*Position*/
    private static final int MINIMUM_TIME = 1000;//1s
    private static final int MINIMUM_DISTANCE = 10;//10m
    private  String		    mtime;			//时间	yyyy-MM-dd HH:mm:ss
    /*GPS*/
    private LocationManager mLocationManager;
    private Location mLocInfo = null;
    /*
    Initialising values in Kalman filter
    */
    private boolean mTrackingStarted = false;
    private Location lastLocation;
    private long lastLocationEntry = -1;
    private boolean mLastLocation = false;
    private long idTrace = -1;
    private long minimumAccuracy;
    private GeoTrackFilter geoTrackFilter;

    LocationListener locationListener = new LocationListener(){
        @Override
        public void onLocationChanged(Location location) {
            mLocInfo = location;
            if(start) {
                refreshNmeaView(mfixStatusTv, "Init_Lat:" + location.getLatitude() + " Init_Lon:" + location.getLongitude() + " Accuracy:"+ location.getAccuracy()+"\n");
                if (mLocInfo.getAccuracy() <= minimumAccuracy) {
                    GetCorPoint();
                }
            }
            if (location.hasAccuracy() && location.getAccuracy() <= minimumAccuracy) {
                long difftime = 0l;
                double speed;
                if(mLastLocation) {
                    difftime = location.getTime() - lastLocation.getTime();
                }

                if(!mLastLocation) {
                    mLastLocation = true;
                    //Add first values to kalman filter
                    geoTrackFilter.update_velocity2d(location.getLatitude(), location.getLongitude(), location.getTime());
                    if(start) {
                        refreshNmeaView(mfixStatusTv, "Newfirt_Lat:" + geoTrackFilter.get_lat_long()[0] + " Newfirst_Lon: " + geoTrackFilter.get_lat_long()[1] + "\n");
                    }
                } else {
                    //Kalman filter
                    geoTrackFilter.update_velocity2d(location.getLatitude(), location.getLongitude(), difftime/1000);
                    double[] latLon = geoTrackFilter.get_lat_long();
                    speed = geoTrackFilter.get_speed();
                    location.setLatitude(latLon[0]);
                    location.setLongitude(latLon[1]);
                    location.setSpeed((float) speed);
                    if(start) {
                        //refreshNmeaView(mfixStatusTv, "New_Latitude " + latLon[0] + " New_Longitude " + latLon[1]+"\n");
                    }
                }
            }
            lastLocation = location;
            if(start) {
                Log.d(mTag, "KalMan时间:" + location.getTime() + " 经度:" + location.getLatitude() + "纬度:" + location.getLongitude() + "\n");
                refreshNmeaView(mfixStatusTv, "last时间:" + location.getTime() + " 经度:" + location.getLatitude() + "纬度:" + location.getLongitude() + "\n");
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            //TODO Auto-generated method stub
            //定位提供者如果關閉時會回呼，並將關閉的提供者傳至provider字串中
        }

        @Override
        public void onProviderEnabled(String provider) {
            //TODO Auto-generated method stub
            //定位提供者如果開啟時會回呼，並將開啟的提供者傳至provider字串中
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras){
            //TODO Auto-generated method stub
            Log.d("GPS-NMEA", provider + "");
            //GPS狀態提供，這只有提供者為gps時才會動作
            switch(status) {
                case LocationProvider.OUT_OF_SERVICE:
                    Log.d("GPS-NMEA","OUT_OF_SERVICE");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.d("GPS-NMEA","TEMPORARILY_UNAVAILABLE");
                    break;
                case LocationProvider.AVAILABLE:
                    Log.d("GPS-NMEA",""+ provider + "");
                    break;
            }
        }
    };
    GpsStatus.NmeaListener nmeaListener = new GpsStatus.NmeaListener() {

        @Override
        public void onNmeaReceived(long timestamp, String nmea) {
            //check nmea's checksum
            Log.d("GPS-NMEA", nmea);
            if(start){
                try{
                    raf.seek(SavaFile.length());
                    raf.write((formatter.format(System.currentTimeMillis())+" ").getBytes());
                    raf.write(nmea.getBytes());
                }catch(IOException e){
                    e.printStackTrace();
                }
                refreshNmeaView(mGpSNmea,(formatter.format(System.currentTimeMillis())+" ")+nmea);
            }
        }
    };

    void refreshNmeaView(TextView textView,String msg){
        textView.append(msg);
        int offset=textView.getLineCount()*textView.getLineHeight();
        if(offset>textView.getHeight()){
            textView.scrollTo(0,offset-textView.getHeight());
        }
    }

    public void startNewTraining(){
        mTrackingStarted = true;
        mLastLocation = false;
        minimumAccuracy = Long.parseLong("10");
        //Init kalman filter
        //double kalman_speed = Double.parseDouble("1.0");
        float kalman_speed = Float.parseFloat("1.0");
        geoTrackFilter = new GeoTrackFilter(kalman_speed);
    }

    public void stopTraining(){
        if(mTrackingStarted) {
            mTrackingStarted = false;
            //Reset variables
            if(mLastLocation)
                lastLocation.reset();
            idTrace = -1;
            lastLocationEntry = -1;
        }
    }

    public void GetCorPoint() {
        double lon =0;
        double lat =0;
        double correctLon =0;
        double correctLat =0;
        Calendar cal = Calendar.getInstance();
        mtime = "2015-01-01 00:00:00";

        if (mLocInfo != null) {
            // 纠偏前的经度
            lon = mLocInfo.getLongitude();
            // 纠偏前的纬度
            lat = mLocInfo.getLatitude();
            double fixpoint[] = MapFixUtil.transform(lat, lon);
            // 纠偏后的经度
            correctLon = fixpoint[1];
            // 纠偏后的纬度
            correctLat = fixpoint[0];
        }
        Log.d(mTag, "纠偏前的经度：" + lon + ",纠偏前的纬度：" + lat);
        Log.d(mTag, "纠偏后的经度：" + correctLon + ",纠偏后的纬度：" + correctLat);
        // 时间BCD[6] yyy-MM-dd HH:mm:ss
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);
        int sec = cal.get(Calendar.SECOND);
        mtime = String.format("%d-%02d-%02d %02d:%02d:%02d", year,month, day, hour, min, sec);
        refreshNmeaView(mfixStatusTv,"Mapfix时间:"+mtime +" 经度:"+correctLat+ "纬度:"+correctLon+"\n");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGpSNmea = (TextView) findViewById(R.id.tv_gpsnmea);
        mfixStatusTv = (TextView) findViewById(R.id.tv_status);
        mGpSNmea.setMovementMethod(ScrollingMovementMethod.getInstance());
        mfixStatusTv.setMovementMethod(ScrollingMovementMethod.getInstance());
        startNewTraining();
        bt = (Button)findViewById(R.id.bt);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(start){
                    start = false;
                    bt.setText("开始");
                }else{
                    bt.setText("停止");
                    start = true;

                    if(timer == null) {
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tv.setText(getConvertFileSize(SavaFile.length()));
                                    }
                                });
                            }
                        }, 0, 1000);
                    }
                }
            }
        });
        tv = (TextView)findViewById(R.id.tv);

        try{
            String path = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/gps.txt";
            SavaFile = new File(path);
            if(SavaFile.exists()){
                tv.setText(path+"："+getConvertFileSize(SavaFile.length()));
            }
            raf = new RandomAccessFile(SavaFile, "rw");
        }catch(IOException e){
            e.printStackTrace();
        }

        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //API23:we have to check if ACCESS_FINE_LOCATION and/or ACCESS_COARSE_LOCATION permission are granted
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                ||ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MINIMUM_TIME, MINIMUM_DISTANCE, locationListener);
            mLocationManager.addNmeaListener(nmeaListener);
        }else{
            //The ACCESS_COARSE_LOCATION is denied, then I request it and manage the result in
            //onRequestPermissionsResult() using the constant MY_PERMISSION_ACCESS_FINE_LOCATION
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_ACCESS_COARSE_LOCATION);
            }
            //The ACCESS_FINE_LOCATION is denied, then I request it and manage the result in
            //onRequestPermissionsResult() using the constant MY_PERMISSION_ACCESS_FINE_LOCATION
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_FINE_LOCATION);
            }
        }
    }

    @Override
    protected void onStop() {
        start = false;
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
        stopTraining();
        mLocationManager.removeUpdates(locationListener);
        mLocationManager.removeNmeaListener(nmeaListener);

        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_ACCESS_COARSE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permissionwasgranted
                } else {
                    //permissiondenied
                }
                break;

            case MY_PERMISSION_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permissionwasgranted
                } else {
                    //permissiondenied
                }
                break;
        }
    }

    public static String getConvertFileSize(long size) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;

        if (size >= gb) {
            return String.format("%.1f GB", (float) size / gb);
        } else if (size >= mb) {
            float f = (float) size / mb;
            return String.format(f > 100 ? "%.0f MB" : "%.1f MB", f);
        } else if (size >= kb) {
            float f = (float) size / kb;
            return String.format(f > 100 ? "%.0f KB" : "%.1f KB", f);
        } else
            return String.format("%d B", size);
    }
}
