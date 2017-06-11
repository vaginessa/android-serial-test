package com.minsight.haneden.androidserialtest;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private TextView textView;
    private UsbManager usbManager;
    private String str = "";
    private UsbDevice usbDevice;
    private UsbDeviceConnection connection;
    private UsbInterface usbIf;
    private UsbEndpoint usbEpOut;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.text_view);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        //ボタン押された時のアクション
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                str = str + "ボタンが押された!" +  "\n";
                textView.setText(str);
                if(connection == null)return;
                str = str + "コネクションはあるみたいだ" +  "\n";

                byte b [][]= {{(byte)0x00},{(byte)0xff}};
                Random rnd = new Random();
                int result = connection.bulkTransfer(usbEpOut, b[rnd.nextInt(2)], 1, 0);
                str = str + "送信結果が正なら成功　->  " + result + "\n";
                textView.setText(str);
            }
        });



        //パーミッションダイアログ用コールバック設定
        IntentFilter filter=new IntentFilter();
        filter.addAction("usbPermissionDialog");
        registerReceiver(myReceiver,filter);


        //ここからデバイスアクセス
        str = str + "deviceList.size:" + deviceList.size() + "\n";

        for (String name : deviceList.keySet()) {
            str = str + "usbDevice " + name + "\n";
            UsbDevice usbDevice = deviceList.get(name);


            str = str + "Interface count " + usbDevice.getInterfaceCount() + "\n";
            if (usbDevice.getInterfaceCount() >= 2) {
                this.usbDevice = usbDevice;
                UsbInterface usbIf = usbDevice.getInterface(1);
                str = str + "endpoint count " + usbIf.getEndpointCount() + "\n";
                for(int i=0 ; i< usbIf.getEndpointCount() ; i++){
                    UsbEndpoint usbEp = usbIf.getEndpoint(i);
                    if(usbEp.getType()== UsbConstants.USB_ENDPOINT_XFER_BULK){
                        str = str + "XFER_BULKエンドポイントが見つかった！"  + "\n";
                        if (usbEp.getDirection() == UsbConstants.USB_DIR_OUT){
                            str = str + "このエンドポイントはOUTのようだ！"  + "\n";
                            usbEpOut = usbEp;
                            this.usbIf = usbIf;
                        }
                    }
                }

                if (!usbManager.hasPermission(usbDevice)) {
                    usbManager.requestPermission(usbDevice,
                            PendingIntent.getBroadcast(MainActivity.this, 0, new Intent("usbPermissionDialog"), 0));
                }


            }



        }

        textView.setText(str);

    }

    public BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(usbDevice == null )return;
            if(usbManager.hasPermission(usbDevice)){
                str = str + "許可された！" + intent.getAction() + "\n";
                connection = usbManager.openDevice(usbDevice);
                boolean result = connection.claimInterface(usbIf, true);
                str = str + "claiminterfaceの結果" + result + "\n";
                //下の二つを送ると送受信用のエンドポイントが通るようになるらしい
                connection.controlTransfer(0x21, 34, 0, 0, null, 0, 0);
                connection.controlTransfer(0x21, 32, 0, 0, new byte[] {
                        (byte)0x80, 0x25, 0x00, 0x00, 0x00, 0x00, 0x08
                }, 7, 0);

            }else{
                str = str + "許可されなかった！" + intent.getAction() + "\n";
            }

            textView.setText(str);
        }
    };

}
