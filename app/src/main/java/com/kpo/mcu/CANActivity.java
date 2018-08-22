package com.kpo.mcu;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
//import com.kpo.libmcu.CANProxy;
//import com.kpo.libmcu.CANproperty;
import com.kpo.mcu.mcucontrols.CANProxy;
import com.kpo.mcu.mcucontrols.CANproperty;

public class CANActivity extends ActionBarActivity {

    Button btn;
    EditText edt;
    private AutoScrollView scrollView;
    private TextView scrollTextview;

    //每帧的头部，有多少个发送id，就预先定义多少个帧头
    byte[] template = new byte[7];

    private CANProxy mCAN;

    Handler mMainHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            byte[] dat = (byte[]) msg.obj;
            /*switch (dat[3]) {//此处根据设定的6个帧id进行分类处理
                case 1:
                    Toast.makeText(MainActivity.this, "扩展数据帧 ", Toast.LENGTH_SHORT).show();
                    break;

                case 2:
                    Toast.makeText(MainActivity.this, "扩展远程帧 ", Toast.LENGTH_SHORT).show();
                    break;

                case 3:
                    Toast.makeText(MainActivity.this, "标准数据帧 ", Toast.LENGTH_SHORT).show();
                    break;

                case 4:
                    Toast.makeText(MainActivity.this, "标准数据帧 ", Toast.LENGTH_SHORT).show();
                    break;

                case 5:
                    Toast.makeText(MainActivity.this, "扩展帧 ", Toast.LENGTH_SHORT).show();
                    break;

                case 6:
                    Toast.makeText(MainActivity.this, "标准帧 ", Toast.LENGTH_SHORT).show();
                    break;
            }*/
            scrollTextview.append(byteToHexString(dat, dat.length));
            scrollTextview.append("\n");
            scrollView.fullScroll(AutoScrollView.FOCUS_DOWN);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_can);

        initView();
        initCAN();
    }

    private void initView(){
        edt = (EditText) findViewById(R.id.editText);
        btn = (Button)findViewById(R.id.send);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String contString = edt.getText().toString();
                send(contString);
            }
        });
        scrollView = (AutoScrollView) findViewById(R.id.auto_scrollview);
        scrollTextview = (TextView)findViewById(R.id.scrollTextview);
    }

    private void initCAN(){
        mCAN = CANProxy.getInstance(this);
        mCAN.setHandler(mMainHandler);
        mCAN.getSerialPort();

        //id定义示例，此帧id为012ab34c，帧类型为扩展帧，帧格式为数据帧
        mCAN.SetID(template, "012ab34c", CANproperty.CAN_EXD_FRAME, CANproperty.CAN_DATA_FRAME);

        //设置波特率
        byte[] param1 = {CANproperty.CAN_SetBaudRate, CANproperty.CAN_Baud_250K};
        mCAN.SendCmd(param1);

        //设置接收过滤
        /*byte[] param2 = {CANproperty.CAN_SetReceiveID,
                0x00, 0x00, 0x00, 0x01, CANproperty.CAN_EXD_FRAME, CANproperty.CAN_DATA_FRAME,
                0x00, 0x00, 0x00, 0x02, CANproperty.CAN_EXD_FRAME, CANproperty.CAN_REMOTE_FRAME,
                0x00, 0x00, 0x00, 0x03, CANproperty.CAN_STD_FRAME, CANproperty.CAN_DATA_FRAME,
                0x00, 0x00, 0x00, 0x04, CANproperty.CAN_STD_FRAME, CANproperty.CAN_REMOTE_FRAME,
                0x00, 0x00, 0x00, 0x05, CANproperty.CAN_EXD_FRAME, CANproperty.CAN_FRAME,
                0x00, 0x00, 0x00, 0x06, CANproperty.CAN_STD_FRAME, CANproperty.CAN_FRAME, };*/
        byte[] param2 = {CANproperty.CAN_SetReceiveAll};
        mCAN.SendCmd(param2);
        mCAN.startThread();
    }

    String byteToHexString(byte[] bArray , final int size) {
        StringBuffer sb = new StringBuffer(size + size/2);
        String sTemp;

        for (int i = 0; i < size; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
            sb.append(' ');
        }
        return sb.toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void send(String sendata)
    {
        mCAN.SendString(template, sendata);
    }
}
