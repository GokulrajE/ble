package com.example.ble;

import static com.example.ble.MainActivity.filename_left;
import static com.example.ble.MainActivity.filename_right;
import static com.example.ble.MainActivity.getCurrentTime;
import static com.example.ble.overallChartFragment.dir;
import static com.example.ble.overallChartFragment.fileHandling;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.charts.Sparkline;
import com.anychart.core.cartesian.series.Line;
import com.anychart.core.stock.series.Spline;
import com.anychart.data.Set;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.temporal.Temporal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class dynamicChartFragment extends Fragment {

    ChartViewModel viewModel ;
    private LineChart mChart;
    private List<Entry> entries1 = new ArrayList<>();
    private List<Entry> entries2 = new ArrayList<>();
    private List<String> labels = new ArrayList<>();
    private int mXValue = 0;
    float yValue1,yValue2;
    float suml,sumr;
    List<Long> TimeStamp_left ;
    List<Long> TimeStamp_right ;
    List<Float> value_left ;
    List<Float> value_right ;
    FileHandling fileHandling;
    public String foldernmae = currentDate();
//    public String filename = "27-08-2024.csv";
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootview = inflater.inflate(R.layout.fragment_dynamic_chart, container, false);
        Log.e("lifecycle","oncreate");
        fileHandling = new FileHandling();
        mChart = rootview.findViewById(R.id.dline_chart);
        System.out.println(mChart);
        setupChart();

        //setup();
        return rootview;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TimeStamp_right = new ArrayList<>();
        TimeStamp_left = new ArrayList<>();
        value_left = new ArrayList<>();
        value_right = new ArrayList<>();
        DecimalFormat df = new DecimalFormat("#.##");
        File dirname = fileHandling.getExternalStorageDir(dir);
        File folder = new File(dirname,foldernmae);
        File left_file = new File(folder,filename_left);
        File right_file = new File (folder,filename_right);
//
        if (left_file.exists() && right_file.exists()) {
            // Use ExecutorService to run parallel tasks
            ExecutorService executor = Executors.newFixedThreadPool(2);
            // Task to read left file
            Future<Void> leftFileTask = executor.submit(() -> {
                try (BufferedReader bufferedReader = new BufferedReader(new FileReader(left_file))) {
                    String nextline;
                    while ((nextline = bufferedReader.readLine()) != null) {
                        String[] parts = nextline.split(",");
                        long time = Long.parseLong(parts[0]);
                        float l = Float.parseFloat(parts[1]);
                        String datetime = MainActivity.getCurrentTime(time);
                        String graph_time = MainActivity.convertStringtoMinutes(datetime);
                        getActivity().runOnUiThread(() -> addDataPointToGraph(graph_time, l, 1));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            });

            // Task to read right file
            Future<Void> rightFileTask = executor.submit(() -> {
                try (BufferedReader bufferedReader = new BufferedReader(new FileReader(right_file))) {
                    String nextline;
                    while ((nextline = bufferedReader.readLine()) != null) {
                        String[] parts = nextline.split(",");
                        long time = Long.parseLong(parts[0]);
                        float r = Float.parseFloat(parts[1]);

                        String datetime = MainActivity.getCurrentTime(time);
                        String graph_time = MainActivity.convertStringtoMinutes(datetime);
                        getActivity().runOnUiThread(() -> addDataPointToGraph(graph_time, r, 2));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            });

            // Wait for both tasks to complete
            try {
                leftFileTask.get();
                rightFileTask.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            // Shutdown executor
            executor.shutdown();
        }
                System.out.println("onviewcreated");
                viewModel = new ViewModelProvider(requireActivity()).get(ChartViewModel.class);
                viewModel.getData1().observe(getViewLifecycleOwner(), newData1 -> {
                    yValue1 = newData1.getY();
                    addDataPointToGraph(newData1.getX(),yValue1,1);
                });
                viewModel.getData2().observe(getViewLifecycleOwner(), newData2 -> {

                    yValue2=newData2.getY();
                    addDataPointToGraph(newData2.getX(),yValue2,2);
                });

    }
    private String currentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        return dateFormat.format(new Date());
    }
    public void addDataPointToGraph(String xLabel,float yvalue ,int device) {
        if(device == 1){
            yValue1 = yvalue;
        }else{
            yValue2 = yvalue;
        }
        suml+=yValue1;
        sumr+=yValue2;
        entries1.add(new Entry(mXValue,yValue1));
        entries2.add(new Entry(mXValue,yValue2));

        labels.add(xLabel);
        float roundedValue_l = Math.round(suml * 100) / 100f;
        float roundedValue_r = Math.round(sumr * 100) / 100f;

        if(getActivity()!=null) {
            TextView text1 = getActivity().findViewById(R.id.left);
            TextView text2 = getActivity().findViewById(R.id.right);
            text1.setText("LEFT-ARM : " + roundedValue_l);
            text2.setText("RIGHT-ARM : " + roundedValue_r);
        }
        LineDataSet dataSet1 = new LineDataSet(entries1, "l-Arm");
        LineDataSet dataSet2 = new LineDataSet(entries2, "r-Arm");
        dataSet1.setColor(Color.rgb(135,206,235));
        dataSet1.setDrawCircles(false);
        dataSet1.setDrawValues(false);
        dataSet1.setLineWidth(2f);
        dataSet2.setColor(Color.rgb(255,165,0));
        dataSet2.setDrawCircles(false);
        dataSet2.setDrawValues(false);
        dataSet2.setLineWidth(2f);
        List<ILineDataSet> dataSets1 = new ArrayList<>();
        List<ILineDataSet> dataSets2 = new ArrayList<>();
        dataSets1.add(dataSet1);
        dataSets2.add(dataSet2);
        LineData lineData = new LineData();
        lineData.addDataSet(dataSet1);
        lineData.addDataSet(dataSet2);
        try{
            if(mChart!= null) {
                float currentXRange = mChart.getHighestVisibleX() - mChart.getLowestVisibleX();
                float currentCenter = mChart.getLowestVisibleX() + currentXRange / 2;
                mChart.setData(lineData);
                XAxis xAxis = mChart.getXAxis();
                xAxis.setValueFormatter(new XAxisValueFormatter(labels)); // Set custom X-axis labels
                mChart.setExtraOffsets(10, 10, 10, 10);
                mChart.setVisibleXRangeMaximum(120);
                mChart.moveViewToX(mChart.getLowestVisibleX()+1);
                mChart.invalidate();
            }else{
                System.out.println("mchart is nulls");
            }
        }catch (NullPointerException e){
            e.printStackTrace();

        }
        mXValue++;
    }
    private void setupChart() {
        mChart.getDescription().setEnabled(false);
        mChart.setTouchEnabled(true);
        mChart.setDragEnabled(true);
        mChart.setPinchZoom(true);
        mChart.setScaleXEnabled(true);
        mChart.setScaleYEnabled(true);
        mChart.setBackgroundColor(Color.rgb(255,255,255));
        XAxis xAxis = mChart.getXAxis();
        xAxis.setGranularity(1f);
        mChart.getXAxis().setDrawGridLines(false);
        mChart.getAxisLeft().setDrawGridLines(false);
        mChart.getAxisRight().setDrawGridLines(false);
        mChart.getAxisRight().setDrawLabels(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        YAxis yAxis = mChart.getAxisLeft();
        yAxis.setAxisMaximum(1.2f);
        yAxis.setAxisMinimum(-0.05f);
//        xAxis.setAxisMinimum(-0.01f);
        System.out.println("setup");
        mChart.animateX(1500);
        mChart.invalidate();
    }

}





