package com.kpo.mcu.mcucontrols;

import android.content.Context;
import android.widget.Toast;

import com.kpo.libmcu.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by kp on 2016/5/13.
 */
public class GpioPin {
    private static GpioPin pin;

    private static String DEVICE_PATH = "/dev/mcu2ttyS6";
    private static int DEVICE_RATE = 115200;
    int databit = 0;
    int stopbit = 0;
    int parity = 0;

    private Context mContext;

    private byte[] ba = new byte[1024];
    private byte[] bb = new byte[32];
    private byte[] value = null;
    private int num = 0;
    private int positon = 0;
    private int limit = 0;


    private SerialPort mSerialPort = null;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private ReadThread mReadThread;
    private static byte[] BUFFER = new byte[1024];

    public static GpioPin getInstance(Context context){
        if(pin == null){
            pin = new GpioPin(context);
        }

        return pin;
    }

    private GpioPin(Context context){
        this.mContext = context;
    }

    public void Open() {
        if (mSerialPort != null) {
            Comm_Exit();
        }

        try{
            //mSerialPort = new SerialPort(DEVICE_PATH, DEVICE_RATE, 0);
            mSerialPort = new SerialPort(DEVICE_PATH, DEVICE_RATE, databit, stopbit, parity);
            mInputStream = mSerialPort.getInputStream();
            mOutputStream = mSerialPort.getOutputStream();

            mReadThread = new ReadThread();
            mReadThread.start();/*
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            Toast.makeText(getBaseContext(), R.string.error_security,Toast.LENGTH_SHORT).show();*/
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Toast.makeText(mContext, "The serial port can not be opened for an unknown reason", Toast.LENGTH_SHORT).show();
        }
    }

    public int getGPIOnum(){
        return num;
    }

    public byte[] getGPIOarray(){
        return value;
    }

    public byte getGPIOvalue(int index){
        if(value!=null && num>0) {
            if(index>=0 && index<num) {
                return value[index];
            }
        }

        return -1;
    }

    private class ReadThread extends Thread {
        private Object mPauseLock ;
        private boolean mPauseFlag ;

        public ReadThread(){
            mPauseLock = new Object() ;
            mPauseFlag = true ;
        }

        public void onPause(){
            synchronized (mPauseLock) {
                mPauseFlag = true;
            }
        }

        public void onResume(){
            synchronized (mPauseLock) {
                if(mPauseFlag) {
                    mPauseFlag = false;
                    mPauseLock.notifyAll();
                }
            }
        }

        private void pauseThread(){
            synchronized (mPauseLock) {
                if(mPauseFlag){
                    try{
                        mPauseLock.wait() ;
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void run() {
            super.run();

            if (mInputStream == null)
                return;

            while (!isInterrupted()) {
                try {
                    Thread.sleep(200);

                    int size = mInputStream.read(BUFFER);
                    if (size > 0) {
                        System.arraycopy(BUFFER, 0, ba, positon, size);
                        limit = positon+size;
                        int j = 0;
                        for(int i=0; i < limit; i++){
                            if(ba[i]==0x0d && i+1<limit && ba[i+1]==0x0a){
                                byte[] frame = new byte[i-j];
                                System.arraycopy(ba, j, frame, 0, i - j);
                                num = frame[0];
                                if(value==null){
                                    value = new byte[num];
                                    System.out.println(num+" gpio");
                                }
                                System.arraycopy(frame, 1, value, 0, num);

                                i += 2;
                                j = i;
                            }
                        }
                        positon = limit-j;
                        if(positon>0) {
                            System.arraycopy(ba, j, bb, 0, positon);
                            System.arraycopy(bb, 0, ba, 0, positon);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }catch (InterruptedException e){
                    //e.printStackTrace();
                    Thread.interrupted();
                    return;
                }
            }
        }
    }

    private void Comm_Exit() {
        if (mReadThread != null) {
            mReadThread.onResume();
            mReadThread.interrupt();
            mReadThread = null;
        }

        if (mSerialPort != null) {
            mSerialPort.ReleasePort();
            mSerialPort = null;
        }
    }
}
