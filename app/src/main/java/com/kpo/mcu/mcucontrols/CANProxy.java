package com.kpo.mcu.mcucontrols;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.kpo.libmcu.SerialPort;

/*
 * Created by kp on 2015/12/14.
 */
public class CANProxy {

    private byte[] ba = new byte[1024];
    private byte[] bb = new byte[32];
    private int positon = 0;
    private int limit = 0;

    private static CANProxy Proxy;
    private static String DEVICE_PATH = "/dev/mcu2ttyS7";
    private static int DEVICE_RATE = 115200;
    int databit = 0;
    int stopbit = 0;
    int parity = 0;
    private Context mContext;
    private Handler mHandler;

    private SerialPort mSerialPort = null;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private ReadThread mReadThread;
    private static byte[] BUFFER = new byte[1024];

    public static CANProxy getInstance(Context context){
        if(Proxy == null){
            Proxy = new CANProxy(context);
        }

        return Proxy;
    }

    private CANProxy(Context context){
        this.mContext = context;
    }

    public void setHandler(Handler handler){
        this.mHandler = handler;
    }

    public Handler getHandler(){
        return this.mHandler;
    }

    public void getSerialPort() {
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

    public void SetID(byte[] frame, String id, int frameType, int frameForm){
        frame[0] = CANproperty.CAN_WriteData;

        for(int i=0; i<4; i++){
            byte high = (byte)(Character.digit(id.charAt(2*i), 16)&0xff);
            byte low = (byte)(Character.digit(id.charAt(2*i+1), 16)&0xff);
            frame[i+1] = (byte)(high<<4|low);
        }

        frame[5] = (byte)frameType;
        frame[6] = (byte)frameForm;
    }

    public void SendString(byte[] template, String sendata)
    {
        byte[] result = new byte[7+sendata.length()];
        System.arraycopy(template, 0, result, 0, 7);

        for(int i=0; i<sendata.length(); i++){
            result[i+7] = (byte)(Character.digit(sendata.charAt(i), 16));
        }

        SendCmd(result);
    }

    public void SendByte(byte[] template, byte[] sendata)
    {
        byte[] result = new byte[7+sendata.length];
        System.arraycopy(template, 0, result, 0, 7);
        System.arraycopy(sendata, 0, result, 7, sendata.length);

        SendCmd(result);
    }

    private byte[] Splice(byte[] frame, String send){
        byte[] result = new byte[7+send.length()];
        System.arraycopy(frame, 0, result, 0, 7);

        for(int i=0; i<send.length(); i++){
            result[i+7] = (byte)(Character.digit(send.charAt(i), 16));
        }

        return result;
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
                    Thread.sleep(10);

                    int size = mInputStream.read(BUFFER);
                    if (size > 0) {
                        System.arraycopy(BUFFER, 0, ba, positon, size);
                        limit = positon+size;
                        int j = 0;
                        for(int i=0; i < limit; i++){
                            if(ba[i]==0x0d && i+1<limit && ba[i+1]==0x0a){
                                byte[] frame = new byte[i-j];
                                System.arraycopy(ba, j, frame, 0, i - j);
                                mHandler.obtainMessage(0, frame).sendToTarget();

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
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    public void stopThread(){
        if(mReadThread != null) mReadThread.onPause();
    }

    public void startThread(){
        if(mReadThread != null) mReadThread.onResume();
    }

    public void SendCmd(byte[] _cmd) {
        try {
            if(mOutputStream != null) mOutputStream.write(_cmd);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void Comm_Exit() {
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