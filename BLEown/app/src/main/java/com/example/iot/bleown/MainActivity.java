package com.example.iot.bleown;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    private BluetoothDevice blemod;
    private String timeStamp;
    private int i=0;
    private String first="";
    private String totRecieved;
    private int flag = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        Button button = (Button)findViewById(R.id.button30);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(),Main2Activity.class);
                startActivity(i);
            }
        });




    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<ScanFilter>();
            }
            scanLeDevice(true);
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        if (flag==1) {
            flag = 0;
            mGatt.disconnect();
            mGatt.close();
            mGatt = null;
        }
        /*if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
        }

        mGatt = null;*/
        Log.i("onpause", "onpause");
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mGatt == null) {

            return;
        }
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
        }

        mGatt.close();
        mGatt = null;

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    } else {
                        mLEScanner.stopScan(mScanCallback);

                    }
                }
            }, SCAN_PERIOD);
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();
            if(!(btDevice.getName()==null) && (btDevice.getName().equals("HMSoft"))){
                connectToDevice(btDevice);
                blemod = btDevice;
                Log.e("ragha","ragha");
            }

        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("onLeScan", device.toString());
                            connectToDevice(device);
                        }
                    });
                }
            };

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, true, gattCallback);
            Log.i("insideconnect", "insideconnect");
            scanLeDevice(false);// will stop after first device detection
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    flag = 0;
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    flag = 1;
                    Log.i("flag", String.valueOf(flag));
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");

            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.get(0).getUuid().toString());
            Log.i("onServicesDiscovered", services.get(1).getUuid().toString());
            Log.i("onServicesDiscovered", services.get(2).getUuid().toString());
            BluetoothGattService service = gatt.getService(UUID.fromString("ffe0-0000-1000-8000-00805f9b34fb"));
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString("ffe1-0000-1000-8000-00805f9b34fb"));
            if(characteristic == null){
                gatt.close();
                return;
            }
            if(gatt.setCharacteristicNotification(characteristic,true)){
                UUID descriptorUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("2902-0000-1000-8000-00805f9b34fb"));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if(!gatt.writeDescriptor(descriptor)){
                    gatt.close();
                }
            }
            //writechar(gatt,characteristic,"hello");
            /*byte[] value = new byte[2];
            value[0] = (byte) (21 & 0xFF);
            value[1] = (byte) (21 & 0xFF);
            characteristic.setValue(value);*/



            //BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
            //        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));

            /*if(services!=null){
                gatt.setCharacteristicNotification(services.get(2).getCharacteristics().get(0), true);
                BluetoothGattDescriptor descriptor = services.get(2).getCharacteristics().get(0).getDescriptor(UUID.fromString("2902-0000-1000-8000-00805f9b34fb"));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
                Log.i("onservices", "onservices");
                Log.i("descvalue",services.get(2).getCharacteristics().get(0).getDescriptor(UUID.fromString("2902-0000-1000-8000-00805f9b34fb")).getValue().toString());
                //Log.i("hello",String.valueOf(gatt.readCharacteristic(services.get(1).getCharacteristics().get(0))));
            }*/
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            //List<BluetoothGattService> services = gatt.getServices();
            //gatt.setCharacteristicNotification(services.get(2).getCharacteristics().get(0), true);
            //gatt.readCharacteristic(services.get(2).getCharacteristics().get(0));
            if(status!=BluetoothGatt.GATT_SUCCESS){
                gatt.close();
            }
            Log.i("descwrite", "descwrite");
            //Log.i("hello",String.valueOf(gatt.readCharacteristic(services.get(1).getCharacteristics().get(0))));
        }


        public void write(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic,String timeStamp){
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            characteristic.setValue("hello");

            //characteristic.setValue((int)timeStamp.charAt(i)/*Character.getNumericValue(timeStamp.charAt(i))*/,BluetoothGattCharacteristic.FORMAT_UINT8,0);
            //characteristic.setValue(65,BluetoothGattCharacteristic.FORMAT_UINT8,0);
            gatt.writeCharacteristic(characteristic);
            //boolean writestatus = gatt.writeCharacteristic(characteristic);
            //Log.i("writestatus", String.valueOf(writestatus));
            Log.i("writeresponse", String.valueOf(characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE));
            i = (i+1);
        }
        public void writechar(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic,String timeStamp){
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            byte pepe = (byte) 0x61;
            byte[] charLetra = new byte[1];
            charLetra[0] = pepe;

            characteristic.setValue(charLetra);
            Log.i("writechar", characteristic.getUuid().toString());
            Log.i("writecharservice", characteristic.getService().getUuid().toString());
            //boolean status = gatt.writeCharacteristic(characteristic);
            //characteristic.setValue("hello");

            //characteristic.setValue((int)timeStamp.charAt(i)/*Character.getNumericValue(timeStamp.charAt(i))*/,BluetoothGattCharacteristic.FORMAT_UINT8,0);
            //characteristic.setValue(65,BluetoothGattCharacteristic.FORMAT_UINT8,0);
            //gatt.writeCharacteristic(characteristic);
            //boolean writestatus = gatt.writeCharacteristic(characteristic);
            //Log.i("writestatus", String.valueOf(writestatus));
            //Log.i("writeresponse", String.valueOf(status));
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            /*if(i<timeStamp.length()){
                write(gatt,characteristic,timeStamp);
            }*/

            Log.i("writestatusnew",String.valueOf(status));
            //if(i==timeStamp.length()){
            //    i=0;
            //}
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            //gatt.setCharacteristicNotification(characteristic, true);
            Log.i("onCharacteristicRead", characteristic.toString());
            Log.i("onCharacteristicRead", characteristic.getUuid().toString());
            Log.i("desc", String.valueOf(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE));
            Log.i("desc", String.valueOf(characteristic.getDescriptors().get(0).getValue()));
            //gatt.setCharacteristicNotification(characteristic, false);
           // Log.i("hello",String.valueOf(gatt.readCharacteristic(characteristic)));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                final byte[] data = characteristic.getValue();

                Log.v("AndroidLE", "data.length: " + data.length);

                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for(byte byteChar : data) {
                        stringBuilder.append(String.format("%04X ", byteChar));

                        Log.v("AndroidLE", String.format("%04X ", byteChar));


                    }
                    Log.v("AndroidLE", new String(data) + stringBuilder.toString());
                }
               // Log.i("hello", characteristic.getValue().toString());
                Log.i("hello", new String(data));
            }
            //gatt.disconnect();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            List<BluetoothGattService> services = gatt.getServices();
            Log.i("change", "change");
            Log.i("onCharacteristicRead", characteristic.getUuid().toString());
            Log.i("onCharacteristicRead", new String(characteristic.getValue()));
            Log.i("onCharacteristicRead", characteristic.getService().getUuid().toString());
            /*characteristic.setValue(65, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            gatt.writeCharacteristic(characteristic);
            characteristic.setValue(66, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            gatt.writeCharacteristic(characteristic);*/
            i=0;

            String second="hello";
            String recieved = new String(characteristic.getValue());
            Log.i("recieved", recieved);
            if(recieved.equals("*b@#")){
                long unixTime = System.currentTimeMillis() / 1000L;
                String unixTimeString = Long.toString(unixTime);
                timeStamp = "**b@"+unixTimeString+"##";
                Log.i("time", timeStamp);
                recieved = "hello";
                characteristic.setValue(timeStamp);
                boolean status = gatt.writeCharacteristic(characteristic);
                Log.i("status", String.valueOf(status));
                first = "";
            }
            if(recieved.contains("*f@")) {
                first = recieved;
            }
            if(!recieved.contains("*")){
                first = first + recieved;
            }
            if(first.contains("#")){
                totRecieved = first;
                Log.i("totrecieved", first);
            }
            boolean status = gatt.writeCharacteristic(characteristic);
            Log.i("status", String.valueOf(status));


            EditText val = (EditText)findViewById(R.id.editText);
            final Button [] bt = new Button[28];
            bt[0] = (Button) findViewById(R.id.button);
            bt[1] = (Button) findViewById(R.id.button8);//This is what i'm trying to replace
            bt[2] = (Button) findViewById(R.id.button15);
            bt[3] = (Button) findViewById(R.id.button22);
            bt[4] = (Button) findViewById(R.id.button2);
            bt[5] = (Button) findViewById(R.id.button9);
            bt[6] = (Button) findViewById(R.id.button16);
            bt[7] = (Button) findViewById(R.id.button23);//This is what i'm trying to replace
            bt[8] = (Button) findViewById(R.id.button3);
            bt[9] = (Button) findViewById(R.id.button10);
            bt[10] = (Button) findViewById(R.id.button17);
            bt[11] = (Button) findViewById(R.id.button24);//This is what i'm trying to replace
            bt[12] = (Button) findViewById(R.id.button4);
            bt[13] = (Button) findViewById(R.id.button11);
            bt[14] = (Button) findViewById(R.id.button18);
            bt[15] = (Button) findViewById(R.id.button25);
            bt[16] = (Button) findViewById(R.id.button5);
            bt[17] = (Button) findViewById(R.id.button12);//This is what i'm trying to replace
            bt[18] = (Button) findViewById(R.id.button19);
            bt[19] = (Button) findViewById(R.id.button26);
            bt[20] = (Button) findViewById(R.id.button6);
            bt[21] = (Button) findViewById(R.id.button13);//This is what i'm trying to replace
            bt[22] = (Button) findViewById(R.id.button20);
            bt[23] = (Button) findViewById(R.id.button27);
            bt[24] = (Button) findViewById(R.id.button7);
            bt[25] = (Button) findViewById(R.id.button29);
            bt[26] = (Button) findViewById(R.id.button21);
            bt[27] = (Button) findViewById(R.id.button28);


            if(recieved.equals("*d@#")){

                characteristic.setValue("**d@"+val.getText().toString()+"##");
                gatt.writeCharacteristic(characteristic);
                first = "";
                Log.i("val", val.getText().toString());
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //button.getBackground().setColorFilter(0xFF00FF00, PorterDuff.Mode.MULTIPLY);
//stuff that updates ui
                    if(totRecieved!=null && totRecieved.contains("#") && totRecieved.contains("#")){
                        for(i=3;i<totRecieved.indexOf('#')-1;i+=5){
                            if(totRecieved.charAt(i)=='1'){
                                bt[(i-3)/5].getBackground().setColorFilter(0xFF00FF00, PorterDuff.Mode.MULTIPLY);
                            }
                            if(totRecieved.charAt(i)=='1' && totRecieved.charAt(i+1)=='0'){
                                bt[(i-3)/5].getBackground().setColorFilter(0xffffff00, PorterDuff.Mode.MULTIPLY); //red
                            }
                            if(totRecieved.charAt(i)=='0' && totRecieved.charAt(i+3)=='1'){
                                bt[(i-3)/5].getBackground().setColorFilter(0xffff0000, PorterDuff.Mode.MULTIPLY);
                            }
                            if(totRecieved.charAt(i)=='0' && totRecieved.charAt(i+3)=='0'){
                                bt[(i-3)/5].getBackground().setColorFilter(0xff0000ff, PorterDuff.Mode.MULTIPLY);
                            }
                            //else{
                            //    bt[(i-3)/5].getBackground().setColorFilter(0xffffffff, PorterDuff.Mode.MULTIPLY);
                            //}
                            if(totRecieved.charAt(i+2)=='1'){
                                bt[(i-3)/5].setText("P");
                            }else{
                                bt[(i-3)/5].setText("N");
                            }


                        }
                    }


                }
            });




            //gatt.setCharacteristicNotification(characteristic,true);
            //gatt.readCharacteristic(characteristic);
            //Log.i("onCharacteristicRead", characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 1).toString());
            //Log.i("hello",characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1).toString());
        }
    };

}



