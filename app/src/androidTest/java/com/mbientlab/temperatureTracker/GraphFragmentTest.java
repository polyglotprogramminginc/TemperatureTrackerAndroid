package com.mbientlab.temperatureTracker;

import android.content.SharedPreferences;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.action.ViewActions;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;

import com.github.mikephil.charting.data.BarDataSet;
import com.mbientlab.temperatureTracker.model.TemperatureSample;
import com.raizlabs.android.dbflow.sql.language.Delete;


import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.sql.Date;

import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.comparesEqualTo;

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
 * Created by Lance Gleason of Polyglot Programming LLC. on 12/12/15.
 * http://www.polyglotprogramminginc.com
 * https://github.com/lgleasain
 * Twitter: @lgleasain
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class GraphFragmentTest extends ActivityInstrumentationTestCase2<MainActivity> {

    public GraphFragmentTest() {
        super(MainActivity.class);
    }

    MainActivity activity;
    SharedPreferences.Editor editor;
    String macAddress = "66:66:66:66";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        SharedPreferences sharedPreferences = getInstrumentation().getTargetContext().getApplicationContext().getSharedPreferences("com.mbientlab.temptracker", 0); // 0 - for private mode
        editor = sharedPreferences.edit();
        editor.remove("ble_mac_address");
        editor.apply();
        editor.commit();
        activity = getActivity();
        sharedPreferences = activity.getSharedPreferences("com.mbientlab.temptracker", 0);
        editor = sharedPreferences.edit();
        editor.putString("ble_mac_address", macAddress);
        editor.apply();
        editor.commit();
        new Delete().from(TemperatureSample.class).where().query();
    }

    @Test
    public void testGraphReset() {
        onView(withId(R.id.demo)).perform(ViewActions.click());
        GraphFragment graphFragment = activity.getGraphFragment();
        BarDataSet dataSet = graphFragment.getmChart().getBarData().getDataSets().get(0);
        testGraphValuesGreaterThan(0, dataSet, 0);
        onView(withId(R.id.demo)).perform(ViewActions.click());
        dataSet = graphFragment.getmChart().getBarData().getDataSets().get(0);
        testGraphValuesEqualTo(0, dataSet, 0, dataSet.getEntryCount());
    }

    @Test
    public void testGraphResetWithData() {
        long timeInMilliseconds = System.currentTimeMillis();
        TemperatureSample sample = new TemperatureSample(new Date(timeInMilliseconds), 33L, macAddress);
        sample.save();
        onView(withId(R.id.demo)).perform(ViewActions.click());
        GraphFragment graphFragment = activity.getGraphFragment();
        BarDataSet dataSet = graphFragment.getmChart().getBarData().getDataSets().get(0);
        testGraphValuesGreaterThan(0, dataSet, 0);
        onView(withId(R.id.demo)).perform(ViewActions.click());
        dataSet = graphFragment.getmChart().getBarData().getDataSets().get(0);
        testGraphValuesEqualTo(33, dataSet, dataSet.getEntryCount() - 1, dataSet.getEntryCount());
        testGraphValuesEqualTo(0, dataSet, 0, dataSet.getEntryCount() - 1);
        sample.delete();
    }

    @Test
    public void testTimeAscendingNoPersistedValues(){
        onView(withId(R.id.demo)).perform(ViewActions.click());
        GraphFragment graphFragment = activity.getGraphFragment();
        TemperatureSample laterSample = graphFragment.getActivitySample(59);
        TemperatureSample earlierSample = graphFragment.getActivitySample(58);
        assertThat(earlierSample.getDate().getTime(), lessThan(laterSample.getDate().getTime()));
        onView(withId(R.id.demo)).perform(ViewActions.click());
        laterSample = graphFragment.getActivitySample(59);
        earlierSample = graphFragment.getActivitySample(58);
        long difference = laterSample.getDate().getTime() - earlierSample.getDate().getTime();
        assertThat(difference, comparesEqualTo(60000L));
    }

    @Test
    public void testTimeAscendingPersistedValues(){
        long timeInMilliseconds = System.currentTimeMillis();
        TemperatureSample sample = new TemperatureSample(new Date(timeInMilliseconds), 33L, macAddress);
        sample.save();
        onView(withId(R.id.demo)).perform(ViewActions.click());
        GraphFragment graphFragment = activity.getGraphFragment();
        TemperatureSample laterSample = graphFragment.getActivitySample(59);
        TemperatureSample earlierSample = graphFragment.getActivitySample(58);
        assertThat(earlierSample.getDate().getTime(), lessThan(laterSample.getDate().getTime()));
        onView(withId(R.id.demo)).perform(ViewActions.click());
        laterSample = graphFragment.getActivitySample(59);
        earlierSample = graphFragment.getActivitySample(58);
        long difference = laterSample.getDate().getTime() - earlierSample.getDate().getTime();
        assertThat(difference, comparesEqualTo(60000L));
        new Delete().from(TemperatureSample.class).where().query();
    }

    @Test
    public void testForMorePersistedRecordsThanGraphContains(){
        onView(withId(R.id.demo)).perform(ViewActions.click());
        long timeInMilliseconds = System.currentTimeMillis();
        for(int i = 70; i > 0; i--){
            TemperatureSample sample = new TemperatureSample(new Date(timeInMilliseconds - ((70-i) * 60000)), (long) i, macAddress);
            sample.save();
        }

        onView(withId(R.id.demo)).perform(ViewActions.click());
        GraphFragment graphFragment = activity.getGraphFragment();
        assertThat(graphFragment.getActivitySample(59).getTemperature(), comparesEqualTo(70L));
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

        SharedPreferences sharedPreferences = getInstrumentation().getTargetContext().getApplicationContext().getSharedPreferences("com.mbientlab.temptracker", 0); // 0 - for private mode
        editor = sharedPreferences.edit();
        editor.remove("ble_mac_address");
        editor.apply();
        editor.commit();
    }

    private void testGraphValuesGreaterThan(float value, BarDataSet dataSet, int startIndex) {
        for (int i = startIndex; i < dataSet.getEntryCount(); i++) {
            assertThat(dataSet.getYValForXIndex(i), greaterThan(value));
        }
    }

    public static void testGraphValuesEqualTo(float value, BarDataSet dataSet, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            assertThat(dataSet.getYValForXIndex(i), comparesEqualTo(value));
        }
    }

}
