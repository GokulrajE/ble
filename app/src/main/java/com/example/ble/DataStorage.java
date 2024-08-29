package com.example.ble;

import java.util.ArrayList;
import java.util.List;

public class DataStorage {
    private String x;
    private float y;
    private int device_no;


    public  DataStorage(String x, float y,int number) {
        this.x = x;
        this.y = y;
        this.device_no = number;
    }

    public String getX() { return x; }
    public float getY() { return y; }
    public int getDevice_no(){return device_no;}

}
