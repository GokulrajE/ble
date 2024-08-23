package com.example.ble;

import static android.content.ContentValues.TAG;
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
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
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

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private BluetoothLeScanner bluetoothLeScanner;
    private List<BluetoothDevice> bleDevices;
    data data3;
    data data4;
    private BluetoothAdapter my_bluetooth;
    private BluetoothGatt bluetoothGatt1;
    private BluetoothGatt bluetoothGatt2;
    private String deviceAddress1 = "A9:42:49:0E:65:80";
    boolean intervalset2;
    boolean intervalset1;
    private String deviceAddress2 = "8C:40:AD:58:04:E7";
    private static final String PREF_LAST_SEEN = "last_seen";
    private SharedPreferences sharedPreferences;
    FileHandling fileHandling;
    private ViewPager2 viewpager;
    private ChartPagerAdapter pagerAdapter;
    public String dir = "imuble";
    private String filename = currentDate() + ".csv";
    ArrayAdapter arrayAdapter;
    TextView sumleft;
    TextView sumright;
    TextView lastupdate;
    ChartViewModel viewModel;
    Button bt;
    Button five_interval;
    Button Ten_interval;
    TextView text_left;
    TextView text_right;
    static String username;
    uploadCSVWorker uploadCSVWorker;
    BluetoothGattCharacteristic interval1;
    BluetoothGattCharacteristic interval2;
    BluetoothGattCharacteristic characteristic2;
    BluetoothGattCharacteristic stringCharacteristic2;
    BluetoothGattCharacteristic characteristic1;
    BluetoothGattCharacteristic stringCharacteristic1;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!username()) { // To get the username
            startActivity(new Intent(MainActivity.this, GetNameActivity.class));
            finish();
        }

        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {//permission to get external file storage
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        }
        viewModel = new ViewModelProvider(this).get(ChartViewModel.class); // To update live data in chart
        viewpager = findViewById(R.id.viewpager);
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new dynamicChartFragment());
        fragments.add(new overallChartFragment());
        pagerAdapter = new ChartPagerAdapter(this);
        viewpager.setAdapter(pagerAdapter);
        sharedPreferences = getSharedPreferences("mypref", Context.MODE_PRIVATE);// To update last seen
        bleDevices = new ArrayList<>();
        TextView name = findViewById(R.id.textname);
        TextView date = findViewById(R.id.datetext);
        TextView days = findViewById(R.id.days);
        username = getname();

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
        five_interval = findViewById(R.id.Five_interval);
        Ten_interval = findViewById(R.id.Ten_interval);
        text_left = findViewById(R.id.text_left);
        text_right = findViewById(R.id.text_right);
        my_bluetooth = BluetoothAdapter.getDefaultAdapter();
        bluetoothenable();
        displayLastSeen();
        bt.setBackgroundColor(Color.rgb(112, 185, 194));
        five_interval.setBackgroundColor(Color.rgb(90, 143, 136));
        Ten_interval.setBackgroundColor(Color.rgb(90, 143, 136));
        bt.setOnClickListener(new View.OnClickListener() {//connect button
            @Override
            public void onClick(View view) {
                connectToDevice1();
                connectToDevice2();
            }
        });
        five_interval.setOnClickListener(new View.OnClickListener() {// 5 sec interval button
            @Override
            public void onClick(View view) {
                Ten_interval.setBackgroundColor(Color.rgb(90, 143, 136));
                int interval = 5;
                intervalset1 = true;
                intervalset2 = true;
                send_interval(interval);
            }
        });
        Ten_interval.setOnClickListener(new View.OnClickListener() {// 10 sec interval button
            @Override
            public void onClick(View view) {
                five_interval.setBackgroundColor(Color.rgb(90, 143, 136));
                intervalset1 = true;
                intervalset2 = true;
                int interval = 10;
                send_interval(interval);
            }
        });
        scheduleCsvUpload(); // shedule the backgroud process to upload the last updated csv file to cloud
//        CardView comment = findViewById(R.id.commentcard);
        TextView load = findViewById(R.id.load);
//        comment.setOnClickListener(new View.OnClickListener() { //comment section
//            @Override
//            public void onClick(View view) {
//                load.setText("loading...");
//                Executor executor = Executors.newSingleThreadExecutor();
//                executor.execute(() -> {
//                    try {
//                        List<String[]> comments = S3Uploader.fetchComments();
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                load.setText("");
//                                load.setText("");
//                                S3Uploader.showCommentsDialog(MainActivity.this, comments);
//                            }
//                        });
//
//                    } catch (IOException e) {
//                        System.out.println(e.getMessage());
//                    }
//                });
//
//            }
//        });

        EmojiCompat.Config config = new BundledEmojiCompatConfig(this);
        EmojiCompat.init(config);

//        fileHandling.writetoexternalfile_aws(dir,"aws.csv");
    }

    public void send_interval(int interval) { // to set interval at run time
        if (intervalset1) {
            if (interval1 != null) {
                byte[] intervalBytes = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(interval).array();
                interval1.setValue(intervalBytes);
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                bluetoothGatt1.writeCharacteristic(interval1);
                intervalset1 = false;
            } else {
                System.out.println("null1");
            }
        }
        if (intervalset2) {
            if (interval2 != null) {
                byte[] intervalBytes = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(interval).array();
                interval2.setValue(intervalBytes);
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                bluetoothGatt2.writeCharacteristic(interval2);
                intervalset2 = false;
            }else{
                System.out.println("null2");
            }
        }
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
        calendar.set(Calendar.HOUR_OF_DAY, 24);
        calendar.set(Calendar.MINUTE, 0);
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

    private void updatelastseen() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PREF_LAST_SEEN, System.currentTimeMillis());
        editor.apply();
    }
    private void displayLastSeen() {
        long lastSeenTimestamp = sharedPreferences.getLong(PREF_LAST_SEEN, 0);
        Log.e("lastimestamp", "" + lastSeenTimestamp);
        System.out.println(lastSeenTimestamp);
        String lastSeenString = formatTimeDifference(lastSeenTimestamp);
        TextView lastSeenTextView = findViewById(R.id.lastu);
        lastSeenTextView.setText(lastSeenString);
    }
    private String formatTimeDifference(long timestamp) {
        long currentTime = System.currentTimeMillis();
        long diffMillis = currentTime - timestamp;
        System.out.println(diffMillis);
        Log.e("differ", "" + diffMillis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(diffMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(diffMillis);
        long days = TimeUnit.MILLISECONDS.toDays(diffMillis);
        System.out.println("sec" + seconds);
        System.out.println("minu" + minutes);
        System.out.println("hr" + hours);
        System.out.println("days" + days);
        if (days > 0) {
            return days + (days == 1 ? "day ago" : "days ago");
        } else if (hours > 0) {
            return hours + (hours == 1 ? "hour ago" : "hours ago");

        } else if (minutes > 0) {
            return minutes + (minutes == 1 ? "minute ago" : "minutes ago");
        } else {
            return "just now";
        }

    }
    private boolean username() {
        String filename = "username.txt";
        File file = new File(getFilesDir(), filename);
        return file.exists();
    }
     String getname() {// to get username which is stored in internal storage
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

    private void writedata(int number, float value, String time) {//To store received data in csv file
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
    String convertStringtoMinutes(String datestr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            Date date = sdf.parse(datestr);
            if (date != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                long hours = calendar.get(Calendar.HOUR_OF_DAY);
                long minutes = calendar.get(Calendar.MINUTE);
                String time = hours + ":" + minutes;
                return time;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return " ";
    }

    private void oncharacteristicchanged(BluetoothGatt gatt, float value, long millis ) {
        String dateStr = getCurrentTime(millis);
        String time = convertStringtoMinutes(dateStr);
        System.out.println(time);
        int devicenumber = (gatt == bluetoothGatt1) ? 1 : 2;
        writedata(devicenumber,value,dateStr);
        try {
            if (devicenumber == 1) {
                data3 = new data(time, value);
                viewModel.setData1(data3);
            } else {
                data4 = new data(time, value);
                viewModel.setData2(data4);
            }
        } catch (NullPointerException e) {
            Log.e("updataed to chart final", e.getMessage());
        }
    }
    static String getCurrentTime(long millis) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return dateFormat.format(new Date(millis));
    }
    // device-1
    private void connectToDevice1() {
        BluetoothDevice device = my_bluetooth.getRemoteDevice(deviceAddress1);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothGatt1 = device.connectGatt(this, false, mygattCallback1);
        // connect to device
    }

    private final BluetoothGattCallback mygattCallback1 = new BluetoothGattCallback() {
        //once device get connected to app, callback fuctions will call for each state
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            // call`s when state of connection change CONNECT AND DISCONNECT
            super.onConnectionStateChange(gatt, status, newState);
            BluetoothDevice device = gatt.getDevice();
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.e("conncetion", "connected successfully");
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                show("connected");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        text_left.setTextColor(Color.rgb(76,175,80));
                    }
                });
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e("conncetion", "disconnceted");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        text_left.setTextColor(Color.RED);
                    }
                });
                show("disconnected");
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            //call`s when required service is discovered
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1214"));
                characteristic1 = service.getCharacteristic(UUID.fromString("19B10001-E8F2-537E-4F6C-D104768A1214"));
                stringCharacteristic1 = service.getCharacteristic(UUID.fromString("19B10002-E8F2-537E-4F6C-D104768A1214"));
                interval1 = service.getCharacteristic(UUID.fromString("19B10003-E8F2-537E-4F6C-D104768A1214"));
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (characteristic1 != null && stringCharacteristic1 != null) {
                    long time = System.currentTimeMillis();
                    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
                    buffer.putLong(time);
                    byte[] dataToSend = buffer.array();
                    stringCharacteristic1.setValue(dataToSend);
                    gatt.writeCharacteristic(stringCharacteristic1);

                } else {
                    Log.e("characteristci", "receives null value");
                }
            }
        }
        //call`s when data received from ble device
        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);
                if(value.length==12){
                byte[] epoch = Arrays.copyOfRange(value,0,8);
                byte[] floatValueBytes = Arrays.copyOfRange(value,8,12);
                ByteBuffer epochBuffer = ByteBuffer.wrap(epoch).order(ByteOrder.LITTLE_ENDIAN);
                long epochValue = epochBuffer.getLong();
                float floatValue = ByteBuffer.wrap(floatValueBytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                long milli = epochValue;
                long time = System.currentTimeMillis();
                    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
                    buffer.putLong(time);
                    byte[] dataToSend = buffer.array();
                    stringCharacteristic1.setValue(dataToSend);
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                gatt.writeCharacteristic(stringCharacteristic1);
                    runOnUiThread(new Runnable() {
                        //update the received data to chart
                        @Override
                        public void run() {
                            oncharacteristicchanged(gatt, floatValue,milli);
                        }
                    });
            }
            if(value.length == 1){
                byte[] ackData = characteristic.getValue();
                if (ackData != null && ackData.length > 0) {
                    if (ackData[0] == 0x01) { // 0x01 is the acknowledgment code sent by the Arduino
                        Log.i(TAG, "Acknowledgment received for interval update.");
                        Toast.makeText(getApplicationContext(), "Interval update acknowledged!", Toast.LENGTH_SHORT).show();
                    }
                }
                System.out.println("received");
            }
            else{
                Log.e("received value", "invalid bytes array");
                System.out.println(value.length);
            }
        }
        //call`s when data writtern to ble device
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "String characteristic written successfully.");
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                gatt.setCharacteristicNotification(characteristic1, true);
                gatt.setCharacteristicNotification(interval1, true);
//                Log.e("read", "ready to function");
                BluetoothGattDescriptor descriptor = characteristic1.getDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
                BluetoothGattDescriptor descriptor1 = interval1.getDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor1);
                if (characteristic.getUuid().equals(UUID.fromString("19B10003-E8F2-537E-4F6C-D104768A1214"))) {
                    Log.i(TAG, "Interval characteristic written successfully.");
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Interval written successfully", Toast.LENGTH_SHORT).show());
                }
            }
            else {
                Log.e("Characteristic Write", "Failed with status: " + status);
            }
        }
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Descriptor written successfully, notifications enabled.");
            } else {
                Log.e(TAG, "Descriptor write failed with status: " + status);
            }
        }


    };
    // device -2
    private void connectToDevice2() {
        BluetoothDevice device = my_bluetooth.getRemoteDevice(deviceAddress2);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        bluetoothGatt2 = device.connectGatt(this, false, mygattCallback2);

    }

    private final BluetoothGattCallback mygattCallback2 = new BluetoothGattCallback() {//once device get connected to app, callback fuctions will call for each state
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            BluetoothDevice device = gatt.getDevice();
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.e("conncetion", "connected successfully");
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        text_right.setTextColor(Color.rgb(76,175,80));
                    }
                });

                show("connected");

                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e("conncetion", "disconnceted");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        text_right.setTextColor(Color.RED);
                    }
                });

                show("disconnected");

                gatt.close();

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID.fromString("19B10000-E8F2-537E-4F6C-D104768A1214"));
                characteristic2 = service.getCharacteristic(UUID.fromString("19B10001-E8F2-537E-4F6C-D104768A1214"));
                stringCharacteristic2 = service.getCharacteristic(UUID.fromString("19B10002-E8F2-537E-4F6C-D104768A1214"));
                interval2 = service.getCharacteristic(UUID.fromString("19B10003-E8F2-537E-4F6C-D104768A1214"));
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (characteristic2 != null && stringCharacteristic2 != null) {
                    long time = System.currentTimeMillis();
                    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
                    buffer.putLong(time);
                    byte[] dataToSend = buffer.array();
                    stringCharacteristic2.setValue(dataToSend);
                    gatt.writeCharacteristic(stringCharacteristic2);
                } else {
                    Log.e("characteristci", "receives null value");
                }
            }
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);
            if(value.length==12){
                byte[] epoch = Arrays.copyOfRange(value,0,8);
                byte[] floatValueBytes = Arrays.copyOfRange(value,8,12);
                ByteBuffer epochBuffer = ByteBuffer.wrap(epoch).order(ByteOrder.LITTLE_ENDIAN);
                long epochValue = epochBuffer.getLong();
                float floatValue = ByteBuffer.wrap(floatValueBytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
               long milli = epochValue;
                long time = System.currentTimeMillis();
                ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
                buffer.putLong(time);
                byte[] dataToSend = buffer.array();
                stringCharacteristic2.setValue(dataToSend);
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                gatt.writeCharacteristic(stringCharacteristic2);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        oncharacteristicchanged(gatt, floatValue,milli);
                    }
                });
            }
            if(value.length == 1){
                System.out.println("received");
            }
            else{
                Log.e("received value", "invalid bytes array");
                System.out.println(value.length);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "String characteristic written successfully.");
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                gatt.setCharacteristicNotification(characteristic2, true);
//                 Log.e("read", "ready to function");
                BluetoothGattDescriptor descriptor = characteristic2.getDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
                BluetoothGattDescriptor descriptor2 = interval2.getDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor2);
                if (characteristic.getUuid().equals(UUID.fromString("19B10003-E8F2-537E-4F6C-D104768A1214"))) {
                    Log.i(TAG, "Interval characteristic written successfully.");
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Interval written successfully", Toast.LENGTH_SHORT).show());
                }


            }
            else {
                Log.e("Characteristic Write", "Failed with status: " + status);
            }
        }
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Descriptor written successfully, notifications enabled.");
            } else {
                Log.e(TAG, "Descriptor write failed with status: " + status);
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
   public void disconnect(){
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
    @Override
    protected void onPause() {
        super.onPause();
        updatelastseen();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();

    }

}