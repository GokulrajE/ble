package com.example.ble;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.util.List;

public class XAxisValueFormatter implements IAxisValueFormatter {
    private List<String> labels;

    public XAxisValueFormatter(List<String> labels) {
        this.labels = labels;
    }
    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        int index = (int) value;
        if (index >= 0 && index < labels.size()) {
            return labels.get(index);
        } else {
            return "";
        }
    }

}
