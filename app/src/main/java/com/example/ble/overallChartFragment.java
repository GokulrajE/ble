package com.example.ble;

import static com.example.ble.MainActivity.filename_left;
import static com.example.ble.MainActivity.filename_right;
import static com.example.ble.S3Uploader.getExternalStorageDir;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class overallChartFragment extends Fragment {
    static FileHandling fileHandling;
    boolean initilized = false;
    private LineChart mChart;
   public static String dir = "Arm_use";
    List<Entry>[] entries;
    List<String>labels= new ArrayList<>();
    List<String>labels1= new ArrayList<>();
    List<ILineDataSet> dataSets1;
    List<EmojiEntry> emojiEntries = new ArrayList<>();
    List<ILineDataSet> dataSets2;
    Button refresh;
    TextView load;
    Handler mainhandler = new Handler(Looper.getMainLooper());
    private static String ACCESS = "";
    private static String SECRET = "";
    String username;
    static final String BUCKET_NAME = "clinicianappbucket";
    static BasicAWSCredentials awsCreds = new BasicAWSCredentials(ACCESS, SECRET);
    static AmazonS3Client s3Client = new AmazonS3Client(awsCreds, Region.getRegion(Regions.EU_NORTH_1));

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootview = inflater.inflate(R.layout.fragment_overall_chart, container, false);
        mChart = rootview.findViewById(R.id.line_chart);
        refresh = rootview.findViewById(R.id.refresh);
        load = rootview.findViewById(R.id.loading);
        System.out.println("oncreatview");
        if (!initilized) {
            setup();
        }
        username = MainActivity.username;
        initilized = true;
        refresh.setBackgroundColor(Color.rgb(90, 143, 136));
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                load.setText("loading...");
//                refreshchart();
            }
        });
        return rootview;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            updatechart();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void onResume() {
        super.onResume();
    }
    @Override
    public void onPause() {
        super.onPause();
        //barchart.removeAllSeries();
    }
    public void refreshchart(){
        fileHandling = new FileHandling();
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            emojiEntries.clear();
            emojiEntries = S3Uploader.readDataFromCSV( username+"/emojidata.csv");
            System.out.println(emojiEntries);
            entries = fetchfromcsv(username+"/",emojiEntries);
            mainhandler.post(new Runnable() {
                @Override
                public void run() {
                    updatechart(entries);
                }
            });
        });
    }

    private List<Entry>[] fetchfromcsv(String foldername,List<EmojiEntry> emojiEntries){
        List<String> keys = new ArrayList<>();
        List<Entry> entries1 = new ArrayList<>();
        List<Entry> entries2 = new ArrayList<>();
        List<Entry> entries4 = new ArrayList<>();
        float mXValue = 0;
        try {
            ListObjectsV2Request request = new ListObjectsV2Request().withBucketName(BUCKET_NAME).withPrefix(foldername);
            ListObjectsV2Result result;
            result = s3Client.listObjectsV2(request);
//            System.out.println(result);
            if(!result.getObjectSummaries().isEmpty()) {
                List<S3ObjectSummary> lobjects = result.getObjectSummaries();
                System.out.println(lobjects);
                do {

                    for (S3ObjectSummary s3ObjectSummary : lobjects) {
                        String key = s3ObjectSummary.getKey();
//                         System.out.println(key);
                        keys.add(key);
                        S3Object s3Object = s3Client.getObject(BUCKET_NAME, key);
                        S3ObjectInputStream objectInputStream = s3Object.getObjectContent();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(objectInputStream));
                        String line;
                        float calcval1 = 0;
                        float calcval2 = 0;

                        float sumValue1 = 0;
                        float sumValue2 = 0;
                        try {
                            while ((line = reader.readLine()) != null) {

                                String[] parts = line.split(",");
                                sumValue1 += Double.parseDouble(parts[1]);
                                sumValue2 += Double.parseDouble(parts[2]);
                            }
                            reader.close();
                            calcval1 = sumValue1 / 60;
                            calcval2 = sumValue2 / 60;
                            //System.out.println(calcval1 + ":" + calcval2);

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        String[] parts = key.split("/");
                        String dates[] = parts[1].split("-");
                        String dateafter = dates[0]+"/"+dates[1];
                        Entry entry1 = new Entry(mXValue,calcval1);
                        Entry entry2 = new Entry(mXValue,calcval2);
                        for(EmojiEntry emojiEntry:emojiEntries){
                            if(emojiEntry.getX()==entry1.getX() && emojiEntry.getY()==entry1.getY()){
                                String emoji = emojiEntry.getEmoji();
                                System.out.println(emojiEntry.getX()+","+emojiEntry.getY()+emoji);
                                System.out.println(emoji);
                                Drawable drawable = EmojiDrawableHelper.getEmojiDrawable(this,emoji);
                                entry1.setIcon(drawable);
                            }
                            if(emojiEntry.getX()==entry2.getX()&&emojiEntry.getY()==entry2.getY()){
                                String emoji = emojiEntry.getEmoji();
                                System.out.println(emojiEntry.getX()+","+emojiEntry.getY()+emoji);
                                Drawable drawable = EmojiDrawableHelper.getEmojiDrawable(this,emoji);
                                System.out.println(emoji);
                                entry2.setIcon(drawable);
                            }
                        }
                        entries1.add(entry1);
                        entries2.add(entry2);
                        labels.add(dateafter);
                        mXValue++;
                    }
                } while (result.isTruncated());
            }else{

            }
        } catch(RuntimeException e){
            System.out.println(e.getMessage());
        }
        return new List[]{entries1,entries2};

    }
    public void updatechart( List<Entry>[] entries){
        try{
            LineDataSet dataSet1 = new LineDataSet(entries[0], "l-Arm");
            LineDataSet dataSet2 = new LineDataSet(entries[1], "r-Arm");
            dataSet1.setLineWidth(2f);
            dataSet1.setColor(Color.rgb(135,206,235));
            dataSet1.setCircleColor(Color.GREEN);
            dataSet1.setCircleColorHole(Color.BLACK);
            dataSet1.setDrawCircles(true);
            dataSet1.setDrawValues(false);
            dataSet2.setColor(Color.rgb(255,165,0));
            dataSet2.setCircleColor(Color.GREEN);
            dataSet2.setLineWidth(2f);
            dataSet2.setDrawCircles(true);
            dataSet2.setCircleColorHole(Color.BLACK);
            dataSet2.setDrawValues(false);
            dataSets1= new ArrayList<>();
            dataSets2  = new ArrayList<>();
            dataSets1.add(dataSet1);
            dataSets2.add(dataSet2);
            dataSet1.setDrawIcons(true);
            LineData lineData = new LineData();
            lineData.addDataSet(dataSet1);
            lineData.addDataSet(dataSet2);
            try{
                if(mChart!= null) {
                    mChart.setData(lineData);
                    XAxis xAxis = mChart.getXAxis();
                    xAxis.setValueFormatter(new XAxisValueFormatter(labels));// Set custom X-axis labels
                    mChart.setVisibleXRangeMaximum(10);
                    mChart.moveViewToX(0);
                    mChart.setExtraOffsets(10,10,10,10);
                    load.setText(" ");
                    mChart.invalidate(); // Refresh chart
                }else{
                    System.out.println("mchart is nulls");
                }
            }catch (NullPointerException e){
                e.printStackTrace();

            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void updatechart() throws IOException {
        List<Entry>[] entries = to_calculate_average();
        try{

            if(!entries[0].isEmpty()&&!entries[1].isEmpty()) {
                LineDataSet dataSet1 = new LineDataSet(entries[0], "l-Arm");
                LineDataSet dataSet2 = new LineDataSet(entries[1], "r-Arm");
                dataSet1.setLineWidth(2f);
                dataSet1.setColor(Color.rgb(135, 206, 235));
                dataSet1.setCircleColor(Color.GREEN);
                dataSet1.setCircleColorHole(Color.BLACK);
                dataSet1.setDrawCircles(true);
                dataSet1.setDrawValues(false);
                dataSet2.setColor(Color.rgb(255, 165, 0));
                dataSet2.setCircleColor(Color.GREEN);
                dataSet2.setLineWidth(2f);
                dataSet2.setDrawCircles(true);
                dataSet2.setCircleColorHole(Color.BLACK);
                dataSet2.setDrawValues(false);
                dataSets1 = new ArrayList<>();
                dataSets2 = new ArrayList<>();
                dataSets1.add(dataSet1);
                dataSets2.add(dataSet2);
                dataSet1.setDrawIcons(true);
                LineData lineData = new LineData();
                lineData.addDataSet(dataSet1);
                lineData.addDataSet(dataSet2);
                try {
                    if (mChart != null) {
                        mChart.setData(lineData);
                        XAxis xAxis = mChart.getXAxis();
                        xAxis.setValueFormatter(new XAxisValueFormatter(labels1));// Set custom X-axis labels
                        mChart.setVisibleXRangeMaximum(10);
                        mChart.moveViewToX(0);
                        mChart.setExtraOffsets(10, 10, 10, 10);
                        mChart.invalidate(); // Refresh chart
                    } else {
                        System.out.println("mchart is nulls");
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();

                }
            }
            else{
                Toast.makeText(getContext(),"No Data",Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    List<Entry>[] to_calculate_average() throws IOException {
        List<Entry> data1 = new ArrayList<>();
        List<Entry> data2 = new ArrayList<>();
        int mXValue=0;
        File dirname = getExternalStorageDir(dir);
        if(dirname.exists()&&dirname.isDirectory()){
            File[] files = dirname.listFiles();
            for(File file: files){
              File right_f = new File(file,filename_right);
              File left_f = new File(file,filename_left);
              Float value_r = calculateSum(right_f,1)/60;
              Float value_l = calculateSum(left_f,1)/60;
              data1.add(new Entry(mXValue,value_l));
              data2.add(new Entry(mXValue,value_r));
              String date = file.getName();
              String[] parts = date.split("-");
              String final_date = parts[0]+"/"+parts[1];
              labels1.add(final_date);
            }
        }
        return new List[]{data1, data2};
    }
    private static float calculateSum(File file, int valueIndex) throws IOException {
        float sum = 0;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] parts = line.split(",");

                    sum += Float.parseFloat(parts[valueIndex]);
            }
        }
        return sum;
    }
    private void setup() {
        mChart.getDescription().setEnabled(false);
        mChart.setTouchEnabled(true);
        mChart.setDragEnabled(true);
        mChart.setPinchZoom(true);
        mChart.setScaleXEnabled(true);
        mChart.setScaleYEnabled(true);
        mChart.setBackgroundColor(Color.rgb(255, 255, 255));
        XAxis xAxis = mChart.getXAxis();
        xAxis.setGranularity(1f);
        YAxis yAxisleft = mChart.getAxisLeft();
        yAxisleft.setAxisMinimum(0f);
        xAxis.setAxisMaximum(1.1f);
        mChart.setVisibleXRangeMaximum(10);
        mChart.moveViewToX(0);
        mChart.getXAxis().setDrawGridLines(false);
        mChart.getAxisLeft().setDrawGridLines(false);
        mChart.getAxisRight().setDrawGridLines(false);
        mChart.getAxisRight().setDrawLabels(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        System.out.println("setup");
        mChart.animateX(1500);
        mChart.invalidate();
    }


}