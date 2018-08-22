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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.kpo.libmcu.SerialPort;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    Button bt;
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

    /*GPS*/
    private LocationManager mLocationManager;
    LocationListener locationListener = new LocationListener(){

        @Override
        public void onLocationChanged(Location loc) {
            //TODO Auto-generated method stub
            //定位資料更新時會回呼
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

                SendCmd(nmea.getBytes());
            }
        }
    };

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
