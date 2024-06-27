package com.example.ble;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.anychart.chart.common.dataentry.DataEntry;

import java.util.Deque;

public class ChartViewModel extends ViewModel {
    private MutableLiveData<data> data1 = new MutableLiveData<>();

    private MutableLiveData<data> data2 = new MutableLiveData<>();
    public LiveData<data> getData1() {
        System.out.println("getdata1");
        return data1;

    }
    public LiveData<data> getData2() {
        System.out.println("getdata2");
        return data2;

    }

    public void setData1(data data) {
        data1.setValue(data);
    }
    public void setData2(data data) {
        data2.setValue(data);
    }




}
