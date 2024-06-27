package com.example.ble;

import static com.example.ble.S3Uploader.BUCKET_NAME;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.emoji.bundled.BundledEmojiCompatConfig;
import androidx.emoji.text.EmojiCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.core.cartesian.series.Line;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity{

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private BluetoothLeScanner bluetoothLeScanner;
    private List<BluetoothDevice> bleDevices;
   data data3;
   data data4;
    //List<data> data4 = new ArrayList<>();

    private BluetoothAdapter my_bluetooth;
    private BluetoothGatt bluetoothGatt1;
    private BluetoothGatt bluetoothGatt2;
   private String deviceAddress1 = "A9:42:49:0E:65:80";

    private String deviceAddress2 = "47:75:AC:4E:78:36";
    private boolean isconnected1, isconnected2;
    private static final String PREF_LAST_SEEN = "last_seen";
    private SharedPreferences sharedPreferences;
    private Handler mHandler = new Handler();
    private Runnable mRunnable;

    FileHandling fileHandling;
    private ViewPager2 viewpager;
    private ChartPagerAdapter pagerAdapter;
    public  String dir = "imuble";
    private String filename = currentDate() + ".csv";
    ArrayAdapter arrayAdapter;

    TextView sumleft;
    TextView sumright;
    TextView lastupdate;
    DataEntry data1,data2;
    Deque<DataEntry> dataqueue1 = new ArrayDeque<>(), dataqueue2= new ArrayDeque<>();
    ChartViewModel viewModel;
    Button bt;
    uploadCSVWorker uploadCSVWorker;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!username()) {
            startActivity(new Intent(MainActivity.this, GetNameActivity.class));
            finish();
        }

        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        }
        viewModel = new ViewModelProvider(this).get(ChartViewModel.class);
        viewpager = findViewById(R.id.viewpager);
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new dynamicChartFragment() );
        fragments.add(new overallChartFragment() );
        pagerAdapter = new ChartPagerAdapter(this);
        viewpager.setAdapter(pagerAdapter);
        //viewpager.setOffscreenPageLimit(1);

        sharedPreferences = getSharedPreferences("mypref", Context.MODE_PRIVATE);
        bleDevices = new ArrayList<>();
        TextView name = findViewById(R.id.textname);
        TextView date = findViewById(R.id.datetext);
        TextView days = findViewById(R.id.days);
        String username = getname();
        String c_date = currentDate();

        if (date != null) {
            date.setText("Date:" + c_date);
        }
        if (!TextUtils.isEmpty(username)) {
            name.setText("Hello!.." + username);
        } else {
            name.setText("Hello!..");
        }
        fileHandling = new FileHandling();
        int size = fileHandling.no_of_files(dir);
        if (size < 10) {
            days.setText("Days of use:0" + size);
        } else {
            days.setText("Days of use:" + size);
        }
        sumleft = findViewById(R.id.left);
        sumright = findViewById(R.id.right);
        lastupdate = findViewById(R.id.lastu);
        bt = findViewById(R.id.show);
        my_bluetooth = BluetoothAdapter.getDefaultAdapter();
        bluetoothenable();
        displayLastSeen();

        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToDevice1();
                connectToDevice2();

            }
        });
        scheduleCsvUpload();
        CardView comment = findViewById(R.id.commentcard);
        TextView load = findViewById(R.id.load);
        comment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                load.setText("loading...");
                Executor executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    try {
                        List<String[]> comments = S3Uploader.fetchComments();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                load.setText("");
                                load.setText("");
                                S3Uploader.showCommentsDialog(MainActivity.this,comments);
                            }
                        });

                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                });

            }
        });

        EmojiCompat.Config config = new BundledEmojiCompatConfig(this);
        EmojiCompat.init(config);

    }
    public void scheduleCsvUpload() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest uploadRequest =
                new OneTimeWorkRequest.Builder(uploadCSVWorker.class)
                        .setConstraints(constraints)
                        .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
                        .build();

        WorkManager.getInstance(getApplicationContext()).enqueue(uploadRequest);
    }

    private long calculateInitialDelay() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 22);
        calendar.set(Calendar.MINUTE, 55);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long currentTime = System.currentTimeMillis();
        long scheduledTime = calendar.getTimeInMillis();

        if (currentTime > scheduledTime) {
            // If current time is already past 12 am, schedule for the next day
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            scheduledTime = calendar.getTimeInMillis();
        }

        return scheduledTime - currentTime;
    }

    private void updatelastseen(){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREF_LAST_SEEN,System.currentTimeMillis());
        editor.apply();
    }
    private void displayLastSeen() {
        long lastSeenTimestamp = sharedPreferences.getLong(PREF_LAST_SEEN,0);
        Log.e("lastimestamp",""+lastSeenTimestamp);
        System.out.println(lastSeenTimestamp);
        String lastSeenString = formatTimeDifference(lastSeenTimestamp);


        TextView lastSeenTextView = findViewById(R.id.lastu);
        lastSeenTextView.setText( lastSeenString);
    }

    private String formatTimeDifference(long timestamp) {
        long currentTime = System.currentTimeMillis();

        long diffMillis = currentTime - timestamp;
        System.out.println(diffMillis);
        Log.e("differ",""+diffMillis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(diffMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(diffMillis);
        long days = TimeUnit.MILLISECONDS.toDays(diffMillis);
        System.out.println("sec"+seconds);
        System.out.println("minu"+minutes);
        System.out.println("hr"+hours);
        System.out.println("days"+days);
        if(days>0){
            return days+(days==1?"day ago":"days ago");
        } else if (hours>0) {
            return hours+(hours==1?"hour ago":"hours ago");

        }else if (minutes>0) {
            return minutes+(minutes==1?"minute ago":"minutes ago");
        }else {
            return "just now";
        }

    }
    private boolean username() {
        String filename = "username.txt";
        File file = new File(getFilesDir(), filename);
        return file.exists();
    }

    private String getname() {
        String filename = "username.txt";
        try {
            FileInputStream fis = openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            return br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return " ";
        }
    }

    private String currentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        return dateFormat.format(new Date());
    }

    private void writedata(int number, float value, String time) {
        float devicel = 0;
        float devicer = 0;

        if (number == 1) {
            devicel = value;
        }
        if (number == 2) {
            devicer = value;
        }

        String data;
        data = time + "," + devicel + "," + devicer;
        fileHandling.writetoexternalfile(dir, filename, data);

    }
    String convertStringtoMinutes(String datestr){
        try{
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            Date date = sdf.parse(datestr);
            if(date != null){
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                long hours = calendar.get(Calendar.HOUR_OF_DAY);
                long minutes = calendar.get(Calendar.MINUTE);
                String time = hours+":"+minutes;
                return time;
            }
        }catch ( ParseException e){
            e.printStackTrace();
        }

        return" ";
    }
    private void oncharacteristicchanged(BluetoothGatt gatt, float value){
        String dateStr = getCurrentTime();
        String time = convertStringtoMinutes(dateStr);
        int devicenumber = (gatt == bluetoothGatt1)?1:2;
        writedata(devicenumber,value,dateStr);

        try {
            if (devicenumber == 1) {
                data3 = new data(time,value);
               viewModel.setData1(data3);
            } else {
                data4 = new data(time,value);
                viewModel.setData2(data4);
            }
        } catch (NullPointerException e) {
            Log.e("updataed to chart final", e.getMessage());
        }

    }
    static  String getCurrentTime(){
        SimpleDateFormat dateFormat= new SimpleDateFormat("yyyyMMdd_HHmmss");
        return dateFormat.format(new Date());
    }
    private void connectToDevice1() {
        BluetoothDevice device = my_bluetooth.getRemoteDevice(deviceAddress1);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothGatt1 = device.connectGatt(this, false, mygattCallback1);
    }

    private final BluetoothGattCallback mygattCallback1 = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            BluetoothDevice device = gatt.getDevice();
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.e("conncetion", "connected successfully");
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                show("connected");

                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e("conncetion", "disconnceted");


                show("disconnected");

                gatt.close();

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1214"));
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString("19B10001-E8F2-537E-4F6C-D104768A1214"));
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (characteristic != null) {
                    gatt.setCharacteristicNotification(characteristic, true);
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                    //gatt.readCharacteristic(characteristic);
                } else {
                    Log.e("characteristci", "receives null value");
                }
            }
        }
        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);
            if (value.length == 7) {
                byte value1 = value[0];
                byte value2 = value[1];
                byte[] floatValueBytes = new byte[] {value[2], value[3], value[4], value[5]};
                byte checksum = value[6];

                if (value1 == (byte) 0xFF && value2 == (byte) 0xFF) {
                    float floatValue = ByteBuffer.wrap(floatValueBytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    byte calculatedChecksum = calculateChecksum(new byte[] {value1, value2, floatValueBytes[0], floatValueBytes[1], floatValueBytes[2], floatValueBytes[3]});

                    if (calculatedChecksum == checksum) {
                        Log.e("received value", ":" + floatValue);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                oncharacteristicchanged(gatt, floatValue);
                            }
                        });
                    } else {
                        Log.e("received value", "Invalid checksum");
                    }
                } else {
                    Log.e("received value", "Invalid values");
                }
            } else {
                Log.e("received value", "Invalid bytes array");
            }
        }

        // Function to calculate checksum
        byte calculateChecksum(byte[] data) {
            byte checksum = 0;
            for (int i = 0; i < data.length; i++) {
                checksum += data[i];
            }
            return checksum;
        }

    };

    private void connectToDevice2() {
        BluetoothDevice device = my_bluetooth.getRemoteDevice(deviceAddress2);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        bluetoothGatt2 = device.connectGatt(this, false, mygattCallback2);

    }

    private final BluetoothGattCallback mygattCallback2 = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            BluetoothDevice device = gatt.getDevice();
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.e("conncetion", "connected successfully");
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                show("connected");

                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e("conncetion", "disconnceted");

                show("disconnected");

                gatt.close();

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1214"));
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString("19B10001-E8F2-537E-4F6C-D104768A1214"));

                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (characteristic != null) {
                    gatt.setCharacteristicNotification(characteristic, true);
                    Log.e("read", "ready to function");
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                    //gatt.readCharacteristic(characteristic);
                } else {
                    Log.e("characteristci", "receives null value");
                }
            }
        }
        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);
            if(value.length == 4) {
                float  floatvalue = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN).getFloat();
//                Log.e("received vlaue", ":" + floatvalue);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        oncharacteristicchanged(gatt,floatvalue);
                    }
                });

            }else{
                Log.e("received value","invalid bytes array");
            }
        }
    };
    private void show(String text){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),text,Toast.LENGTH_LONG).show();
            }
        });


    }

    private void bluetoothenable() {                                    //bluetooth connection method
        if (my_bluetooth != null && !my_bluetooth.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            bluetoothLauncher.launch(enableBtIntent);
        }
    }

    private final ActivityResultLauncher<Intent> bluetoothLauncher = registerForActivityResult(        //intent for enable bluetooth if it is not enabled
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == Activity.RESULT_OK){
                    Toast.makeText(getApplicationContext(),"bluetooth enabled",Toast.LENGTH_LONG).show();

                }else{
                    Toast.makeText(getApplicationContext(),"bluetooth not enabled",Toast.LENGTH_LONG).show();
                    finish();
                }

            }
    );
    private void startBleScan() {
        bluetoothLeScanner = my_bluetooth.getBluetoothLeScanner();
        if (bluetoothLeScanner != null) {
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothLeScanner.startScan(null, settings, scanCallback);
        } else {
            Log.e("BLE", "BluetoothLeScanner is null");
        }
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            BluetoothDevice newDevice = result.getDevice();
            if (newDevice != null && !bleDevices.contains(newDevice)) {
                bleDevices.add(newDevice);
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                String devicename = newDevice.getName();
                if(devicename != null)
                {
                    arrayAdapter.add(newDevice.getName());
                    arrayAdapter.notifyDataSetChanged();
                    Log.e("BLE", "device found" + newDevice.getName());
                }
                else{
                    Log.e("ble","device found with null name");
                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start BLE scanning
                Log.e("ble","Start scanning");
                startBleScan();
            } else {
                // Permission denied, handle accordingly
                Log.e("BLE", "Location permission denied");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        updatelastseen();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (bluetoothLeScanner != null) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothLeScanner.stopScan(scanCallback);
        }

        if(bluetoothGatt1 != null){
            bluetoothGatt1.close();

            bluetoothGatt1.disconnect();

        }
        if(bluetoothGatt2 != null){
            bluetoothGatt2.close();

            bluetoothGatt2.disconnect();

        }

    }

}