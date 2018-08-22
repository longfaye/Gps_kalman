package com.kpo.mcu;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.KeyEvent;

import com.kpo.mcu.mcucontrols.Gpio;
//import com.kpo.libmcu.Gpio;

public class GPIOActivity extends AppCompatActivity {

    private Gpio GpioPin;
    public Thread myMcuThread;
    public boolean isThreadRun = true;

    static final int MCU_KEY1 = 2036;
    static final int MCU_KEY2 = 2037;
    static final int MCU_KEY3 = 2038;

    Button bt_get;
    EditText et_val;
    TextView tv_val;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gpio);

        et_val = (EditText) findViewById(R.id.editTextGpio);
        bt_get = (Button) findViewById(R.id.getGpio);
        tv_val = (TextView)findViewById(R.id.textViewGpio);

        GpioPin = GpioPin.getInstance(this);
        GpioPin.Open();

        myMcuThread = new Thread(new McuThread());
        myMcuThread.start();

        bt_get.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int index = Integer.parseInt(et_val.getText().toString());
                int num = GpioPin.getGPIOnum();
                if(index>=0 && index<num) {
                    Toast.makeText(getBaseContext(), "pin "+index+" value: "+GpioPin.getGPIOvalue(index), Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getBaseContext(), "only have "+num+" pin", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            isThreadRun = false;
            return true;
        }
        switch (keyCode) {
            case MCU_KEY1:
                et_val.setText(String.valueOf(keyCode));
                return true;
            case MCU_KEY2:
                et_val.setText(String.valueOf(keyCode));
                return true;
            case MCU_KEY3:
                et_val.setText(String.valueOf(keyCode));
                return true;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }


    class McuThread implements Runnable {
        String stateStr = "";

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    stateStr = "ERROR";
                    tv_val.setText(stateStr);
                    break;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isThreadRun) {
                            stateStr = String.valueOf(" Pin0=" + GpioPin.getGPIOvalue(0))
                                    + "\n Pin1=" + String.valueOf(GpioPin.getGPIOvalue(1))
                                    + "\n Pin2=" + String.valueOf(GpioPin.getGPIOvalue(2))
                                    + "\n Pin3=" + String.valueOf(GpioPin.getGPIOvalue(3))
                                    + "\n Pin4=" + String.valueOf(GpioPin.getGPIOvalue(4))
                                    + "\n Pin5=" + String.valueOf(GpioPin.getGPIOvalue(5))
                                    + "\n Pin6=" + String.valueOf(GpioPin.getGPIOvalue(6))
                                    + "\n Pin7=" + String.valueOf(GpioPin.getGPIOvalue(7))
                                    + "\n Pin8=" + String.valueOf(GpioPin.getGPIOvalue(8));

                            tv_val.setText(stateStr);
                        }else {
                            finish();
                        }
                    }
                });
                if (!isThreadRun) {
                    break;
                }
            }
        }
    }

}
