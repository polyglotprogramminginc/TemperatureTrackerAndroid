package com.mbientlab.temperatureTracker;

import android.app.Activity;
import android.app.LoaderManager;
import android.bluetooth.BluetoothDevice;
import android.content.Loader;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.ChartData;
import com.mbientlab.temperatureTracker.model.TemperatureLoader;
import com.mbientlab.temperatureTracker.model.TemperatureSample;
import com.mbientlab.temperatureTracker.model.TemperatureSample$Table;
import com.raizlabs.android.dbflow.sql.language.Select;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
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
 * Created by Lance Gleason of Polyglot Programming LLC. on 4/26/15.
 * http://www.polyglotprogramminginc.com
 * https://github.com/lgleasain
 * Twitter: @lgleasain
 */
public class GraphFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<TemperatureSample>> {

    public static Fragment newInstance() {
        return new GraphFragment();
    }

    final int colors[] = {Color.parseColor("#52EDC7"), Color.parseColor("#5AC8FB")};

    private BarChart mChart;
    private boolean demo = false;
    private TemperatureSample[] temperatureSamples = new TemperatureSample[61];
    private GraphCallback callback;
    private TemperatureLoader temperatureLoader;


    public interface GraphCallback {
        void setGraphFragment(GraphFragment graphFragment);

        String getBluetoothDevice();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        for (int i = 0; i < temperatureSamples.length; i++) {
            temperatureSamples[i] = new TemperatureSample();
        }

        View v = inflater.inflate(R.layout.fragment_graph, container, false);
        mChart = (BarChart) v.findViewById(R.id.gragh_layout);
        mChart.setDescription("");
        mChart.setDrawGridBackground(false);
        mChart.setDrawBarShadow(false);
        mChart.setDrawBorders(false);
        mChart.setMaxVisibleValueCount(1);
        mChart.setBackgroundColor(Color.BLACK);

        getLoaderManager().initLoader(0, null, this);
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        if (!(activity instanceof GraphCallback)) {
            throw new RuntimeException("Acitivty does not implement DeviceConfirmationCallback interface");
        }
        callback = (GraphCallback) activity;
        super.onAttach(activity);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();

        callback.setGraphFragment(this);
        updateGraph();
        mChart.invalidate();
        Legend l = mChart.getLegend();
        if (l != null) {
            l.setEnabled(false);
        }

        mChart.getAxisRight().setEnabled(false);
        mChart.getXAxis().setEnabled(false);

        XAxis xAxis = mChart.getXAxis();
        xAxis.setEnabled(false);
        mChart.getAxisLeft().setEnabled(false);
    }

    public BarChart getmChart() {
        return mChart;
    }

    public void toggleDemoData(boolean isChecked) {
        demo = isChecked;
        updateGraph();
    }

    public void updateGraph() {
        if (demo) {
            generateBarData(100, 60);
            mChart.setData(getCurrentReadings());
        } else {
            readPersistedValues();
            mChart.setData(getCurrentReadings());
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mChart.invalidate();
            }
        });
    }

    private void readPersistedValues() {
        if (callback.getBluetoothDevice() != null) {
            temperatureLoader.setMacAddress(callback.getBluetoothDevice());
            temperatureLoader.onContentChanged();
        } else {
            for (int finishIndex = 59; finishIndex >= 0; finishIndex--) {
                long lastTime = System.currentTimeMillis();
                temperatureSamples[finishIndex].setDate(new java.sql.Date(lastTime - (60000 * (59 - finishIndex))));
                temperatureSamples[finishIndex].setTemperature(0L);
            }
        }
    }

    public BarData getCurrentReadings() {
        ArrayList<BarDataSet> sets = new ArrayList<>();

        ArrayList<BarEntry> entries = new ArrayList<>();

        for (int j = 1; j < 61; j++) {
            entries.add(new BarEntry(temperatureSamples[j].getTemperature(), j));
        }

        BarDataSet ds = new BarDataSet(entries, getLabel(0));

        ds.setColors(colors);
        sets.add(ds);

        return new BarData(ChartData.generateXVals(0, 60), sets);
    }

    public TemperatureSample getActivitySample(int index) {
        return temperatureSamples[index];
    }

    protected void generateBarData(float range, int count) {

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        for (int j = 0; j < count; j++) {
            temperatureSamples[j].setDate(new java.sql.Date(System.currentTimeMillis() - (60000 * (count - j))));
            Long temperature = (long) ((Math.random() * range) + range / 4);
            temperatureSamples[j].setTemperature(temperature);
        }
    }

    @Override
    public Loader<List<TemperatureSample>> onCreateLoader(int i, Bundle bundle) {
        temperatureLoader = new TemperatureLoader(getActivity());
        return temperatureLoader;
    }

    public void zeroOutReadings() {
        long lastTime = System.currentTimeMillis();
        for (int finishIndex = 0; finishIndex < 60; finishIndex++) {
            temperatureSamples[finishIndex].setDate(new java.sql.Date(lastTime - (60000 * (59 - finishIndex))));
            temperatureSamples[finishIndex].setTemperature(0L);
        }
        mChart.setData(getCurrentReadings());
        mChart.invalidate();
    }

    @Override
    public void onLoadFinished(Loader<List<TemperatureSample>> loader, List<TemperatureSample> persistedSamples) {
        //List<TemperatureSample> persistedSamples = new Select().from(TemperatureSample.class).orderBy(false, TemperatureSample$Table.DATE).limit(60).queryList();
        int i = 59;
        for (TemperatureSample persistedSample : persistedSamples) {
            temperatureSamples[i].setDate(new java.sql.Date(persistedSample.getDate().getTime()));
            temperatureSamples[i].setTemperature(persistedSample.getTemperature());
            //temperatureSamples[i].setIndividualTemperature(temperature);
            Log.i("GraphFragment data time ", String.valueOf(persistedSample.getDate().getTime()));
            Log.i("GraphFragment data value ", String.valueOf(persistedSample.getTemperature()));
            i--;
        }
        long lastTime = 0;

        if (i == 59) {
            lastTime = System.currentTimeMillis();
        } else {
            lastTime = temperatureSamples[i + 1].getDate().getTime() - 60000;
        }

        for (int finishIndex = i; finishIndex >= 0; finishIndex--) {
            temperatureSamples[finishIndex].setDate(new java.sql.Date(lastTime - (60000 * (i - finishIndex))));
            temperatureSamples[finishIndex].setTemperature(0L);
        }
        mChart.setData(getCurrentReadings());
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Legend l = mChart.getLegend();
                if (l != null) {
                    l.setEnabled(false);
                }

                mChart.invalidate();
            }
        });
        TextView readingTime = (TextView) getActivity().findViewById(R.id.reading_time);
        readingTime.setText("");
        TextView tempView = (TextView) getActivity().findViewById(R.id.temperature);
        tempView.setText("");
    }

    @Override
    public void onLoaderReset(Loader<List<TemperatureSample>> loader) {
        Log.i("TemperatureSample", "reset called");
    }

    private String[] mLabels = new String[]{"Activity A", "Activity B", "Activity C", "Activity D", "Activity E", "Activity F"};

    private String getLabel(int i) {
        return mLabels[i];
    }

    public boolean isDemo() {
        return demo;
    }

    public TemperatureSample[] getTemperatureSamples() {
        return temperatureSamples;
    }

    public TemperatureLoader getTemperatureLoader() {
        return temperatureLoader;
    }
}
