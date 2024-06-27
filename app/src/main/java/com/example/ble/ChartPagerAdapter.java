package com.example.ble;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class ChartPagerAdapter extends FragmentStateAdapter {
    private Fragment[] fragments;

    public ChartPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        fragments = new Fragment[]{
                new dynamicChartFragment(),new overallChartFragment()
        };
    }


    @NonNull
    @Override
    public Fragment createFragment(int position) {

        return fragments[position];
    }

    @Override
    public int getItemCount() {
        return fragments.length;
    }
    public Fragment getFragment (int position){
        return fragments[position];
    }

}
