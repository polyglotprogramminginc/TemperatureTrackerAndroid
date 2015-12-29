package com.mbientlab.temperatureTracker;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.Highlight;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Copyright 2014 MbientLab Inc. All rights reserved.
 * <p/>
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 * <p/>
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 * <p/>
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 * <p/>
 * <p/>
 * Created by Lance Gleason of Polyglot Programming LLC. on 12/22/15.
 * http://www.polyglotprogramminginc.com
 * https://github.com/lgleasain
 * Twitter: @lgleasain
 */
public class MainFragment extends Fragment implements ServiceConnection, OnChartValueSelectedListener {


    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);


        Switch demoSwitch = (Switch) rootView.findViewById(R.id.demo);
        demoSwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean isChecked) {

                GraphFragment graphFragment = (GraphFragment) getChildFragmentManager().findFragmentById(R.id.graph);
                if (graphFragment == null) {
                    graphFragment = (GraphFragment) getFragmentManager().findFragmentById(R.id.graph);
                }
                graphFragment.toggleDemoData(isChecked);
            }
        });
        GraphFragment graphFragment = getGraphFragment();
        graphFragment.getmChart().setOnChartValueSelectedListener(this);
        return rootView;
    }


    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
        GraphFragment graphFragment = getGraphFragment();
        int steps = Float.valueOf(e.getVal()).intValue();
        TextView readingTime = (TextView) getActivity().findViewById(R.id.reading_time);
        String formattedDate;
        Date date = new Date(graphFragment.getActivitySample(e.getXIndex()).getDate().getTime());
        DateFormat outputDateFormat = new SimpleDateFormat("MMM dd, yyyy   HH:mm", Locale.getDefault());
        outputDateFormat.setTimeZone(TimeZone.getDefault());
        formattedDate = outputDateFormat.format(date);

        readingTime.setText(formattedDate);
        TextView tempView = (TextView) getActivity().findViewById(R.id.temperature);
        tempView.setText(String.valueOf(steps) + getString(R.string.degrees));
    }

    @Override
    public void onNothingSelected() {

    }

    private GraphFragment getGraphFragment() {
        GraphFragment graphFragment = (GraphFragment) getChildFragmentManager().findFragmentById(R.id.graph);
        if (graphFragment == null) {
            graphFragment = (GraphFragment) getFragmentManager().findFragmentById(R.id.graph);
        }
        return graphFragment;
    }

}
