package com.kpo.mcu.mcucontrols;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import com.kpo.libmcu.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by kp on 2016/5/13.
 */
public class Watchdog {
    private static Watchdog dog;

    private static String DEVICE_PATH = "/dev/mcu2ttyS8";
    private static int DEVICE_RATE = 115200;
    int databit = 0;
    int stopbit = 0;
    int parity = 0;

    private Context mContext;
    private Handler mHandler;

    private SerialPort mSerialPort = null;
    private OutputStream mOutputStream;

    public static Watchdog getInstance(Context context){
        if(dog == null){
            dog = new Watchdog(context);
        }

        return dog;
    }

    private Watchdog(Context context){
        this.mContext = context;
    }

    public void setHandler(Handler handler){
        this.mHandler = handler;
    }

    public Handler getHandler(){
        return this.mHandler;
    }

    public void Open() {
        if (mSerialPort != null) {
            Comm_Exit();
        }

        try{
            //mSerialPort = new SerialPort(DEVICE_PATH, DEVICE_RATE, 0);
            mSerialPort = new SerialPort(DEVICE_PATH, DEVICE_RATE, databit, stopbit, parity);
            mOutputStream = mSerialPort.getOutputStream();/*
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            Toast.makeText(getBaseContext(), R.string.error_security,Toast.LENGTH_SHORT).show();*/
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Toast.makeText(mContext, "The serial port can not be opened for an unknown reason", Toast.LENGTH_SHORT).show();
        }
    }

    public void Feed() {
        try {
            if(mOutputStream != null) mOutputStream.write(" ".getBytes());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void Comm_Exit() {
        if (mSerialPort != null) {
            mSerialPort.ReleasePort();
            mSerialPort = null;
        }
    }
}
