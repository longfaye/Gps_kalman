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
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.kpo.libmcu.SerialPort;
import com.kpo.mcu.Util.GpsPointInfo;
import com.kpo.mcu.Util.MapFixUtil;
import com.kpo.mcu.Util.Writelog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private final String mTag = "KPOCOM_GPS";
    Button bt;
    TextView tv;
    boolean start = false;
    File SavaFile = null;
    RandomAccessFile raf = null;
    Timer timer = null;
    SimpleDateFormat formatter = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA);

    private TextView mGpSNmea, mfixStatusTv;
    private String mStatusStr = "";

    /*GPSConstantPermission*/
    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;

    /*Position*/
    private static final int MINIMUM_TIME = 1000;//1s
    private static final int MINIMUM_DISTANCE = 10;//10m

    /*GPS*/
    private LocationManager mLocationManager;
    private Location mLocInfo = null;

    private  int			mlatitude;		//纬度	1/10000分
    private  int			mlongitude;		//经度	1/10000分
    private  int			mspeed;			//速度	1/10KM/H
    private  int 		    maltitude;		//高度	海拔高度，单位米
    private  int			mdirection;		//方向	0~359°，正北为0,顺时针
    private  String		    mtime;			//时间	yyyy-MM-dd HH:mm:ss

    LocationListener locationListener = new LocationListener(){

        @Override
        public void onLocationChanged(Location loc) {
            //TODO Auto-generated method stub
            //定位資料更新時會回呼
            if (loc==null) {
                mConnected = false;
                Log.i(mTag, "GPSStatus:" + false);
            }else {
                mConnected = true;
            }
            mLocInfo = loc;
            GetfixPoint();
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
                //updateNmea2TXT(nmea);
                refreshNmeaView(mGpSNmea,(formatter.format(System.currentTimeMillis())+" ")+nmea);
                //SendCmd(nmea.getBytes());
                //GetfixPoint();
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

    /**
     * 更新监控台的输出NMEA信息
     * @param content 更新内容
     */
    private void updateNmea2TXT(String content) {
        mStatusStr += content + "\n";
        if (mGpSNmea != null) {
            mGpSNmea.setText(mStatusStr);
        }
    }

    private boolean mConnected = false;
    public boolean getConnected() {
        return mConnected;
    }

    public boolean isGPSEnable() {
        String str = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        // Writelog.v(mTag, str);
        if (str != null) {
            return str.contains("gps");
        } else {
            return false;
        }
    }
    public float getSpeed() {
        float i = 0.00f;

        if (mLocInfo == null) {
            i = 0.00f;
        } else {
            i = (mLocInfo.getSpeed() * 36 / 10);
        }
        return i;
    }

    public float getBearing() {
        if (mLocInfo == null)
            return 0.00f;
        else
            return mLocInfo.getBearing();
    }

    public void GetfixPoint() {
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
        //Writelog.i(mTag, "经度:%f,纬度:%f,方向:%d,时间:%s", mlongitude, mlatitude, mdirection, mtime);
        refreshNmeaView(mfixStatusTv,"时间:"+mtime +" 经度:"+correctLat+ "纬度:"+correctLon+"\n");
    }

    public GpsPointInfo GetGpsPoint() {
        double latitude = 0;
        double longitude = 0;
        int direction = 0;
        Calendar cal = Calendar.getInstance();
        GpsPointInfo point = new GpsPointInfo();
        point.time = "2015-01-01 00:00:00";
        if (mLocInfo != null) {
            // double d;
            latitude = mLocInfo.getLatitude();
            longitude = mLocInfo.getLongitude();
            direction  = (int) mLocInfo.getBearing();
            point.latitude = (int) (latitude * 600000.00f);
            point.longitude = (int) (longitude * 600000.00f);

            // 经度 1/10000分
            if (mConnected) {
                point.speed = (int) (mLocInfo.getSpeed() * 36.00f);
            } else {
                point.speed = 0;
            }
            // 高度 UINT16 海拔高度，单位米
            point.altitude = (int) (mLocInfo.getAltitude());
            // 方向0—178,刻度为2度，正北为0，顺时针
            point.direction = direction/2;
        }

        // 时间BCD[6] yyy-MM-dd HH:mm:ss
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);
        int sec = cal.get(Calendar.SECOND);
        point.time = String.format("%d-%02d-%02d %02d:%02d:%02d", year,month, day, hour, min, sec);
        Writelog.i(mTag, "经度:%f,纬度:%f,方向:%d,时间:%s", longitude, latitude, direction, point.time);
        return point;
    }

    private SerialPort mSerialPort = null;
    private InputStream mInputStream;
    private OutputStream mOutputStream;

    String path = "/dev/ttyMT0";
    int baudrate = 115200;
    int databit = 0;
    int stopbit = 0;
    int parity = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGpSNmea = (TextView) findViewById(R.id.tv_gpsnmea);
        mfixStatusTv = (TextView) findViewById(R.id.tv_status);
        mGpSNmea.setMovementMethod(ScrollingMovementMethod.getInstance());
        mfixStatusTv.setMovementMethod(ScrollingMovementMethod.getInstance());

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

                    getSerialPort(path, baudrate, databit, stopbit, parity);

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

            //One or both permissions are denied.
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

        mLocationManager.removeUpdates(locationListener);
        mLocationManager.removeNmeaListener(nmeaListener);

        super.onStop();
        Comm_Exit();
    }

    public boolean getSerialPort(String path, int baudrate, int databit, int stopbit, int parity) {
        if (mSerialPort != null) {
            Comm_Exit();
        }

        try{
            //mSerialPort = new SerialPort(path, baudrate, 0);
            mSerialPort = new SerialPort(path, baudrate, databit, stopbit, parity);
            mInputStream = mSerialPort.getInputStream();
            mOutputStream = mSerialPort.getOutputStream();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Toast.makeText(getBaseContext(), "The serial port can not be opened for an unknown reason.",Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    public void Comm_Exit() {
        if (mSerialPort != null) {
            mSerialPort.ReleasePort();
            mSerialPort = null;
        }
    }

    public void SendCmd(byte[] _cmd) {
        try {
            if(mOutputStream != null) mOutputStream.write(_cmd);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
