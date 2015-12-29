package com.mbientlab.temperatureTracker;

import android.content.SharedPreferences;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.action.ViewActions;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.number.OrderingComparison.comparesEqualTo;

import android.test.suitebuilder.annotation.LargeTest;
import android.widget.TextView;

import com.mbientlab.temperatureTracker.model.TemperatureSample;
import com.mbientlab.temperatureTracker.model.TemperatureSample$Table;
import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Select;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.sql.Date;

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
 * Created by Lance Gleason of Polyglot Programming LLC. on 12/13/15.
 * http://www.polyglotprogramminginc.com
 * https://github.com/lgleasain
 * Twitter: @lgleasain
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private SharedPreferences.Editor editor;
    private MainActivity activity;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        SharedPreferences sharedPreferences = getInstrumentation().getTargetContext().getApplicationContext().getSharedPreferences("com.mbientlab.temptracker", 0); // 0 - for private mode
        editor = sharedPreferences.edit();
        editor.remove("ble_mac_address");
        editor.remove("44:44:44:44:44:44_menu_id");
        editor.remove("44:44:44:44:44:66_menu_id");
        editor.remove("saved_adapters");
        editor.apply();
        editor.commit();
    }

    @Test
    public void testNonCompletePersistedAdapter() {
        editor.putString("ble_mac_address", "FA:66:66:66:66:66");
        editor.apply();
        editor.commit();
        activity = getActivity();
        String addressAfterIntialization = activity.getSharedPreferences("com.mbientlab.temptracker", 0).getString("ble_mac_address", null);
        assertNull(addressAfterIntialization);
        assertNull(activity.getMwBoard());
        assertNull(activity.getActivityBluetoothDevice());
    }

    @Test
    public void testUnconnectedRefreshDoesntCrash() {
        activity = getActivity();
        onView(withId(R.id.action_refresh)).perform(ViewActions.click());
        assertTrue(true);
    }

    @Test
    public void unconnectedInitialGraph() {
        activity = getActivity();
        TemperatureSample point = activity.getGraphFragment().getActivitySample(30);
        assertNotNull(point.getDate());
    }

    @Test
    public void testAddingBluetoothToMenuWithNoPriorItems() {
        activity = getActivity();
        getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        activity.addBluetoothToMenuAndConnectionStatus("44:44:44:44:44:44");
                        int addedItemId = activity.getSharedPreferences("com.mbientlab.temptracker",
                                0).getInt("44:44:44:44:44:44_menu_id", 0);
                        assertThat(activity.getMenu().findItem(addedItemId).getTitle().toString(),
                                comparesEqualTo("44:44:44:44:44:44 Connected"));
                        assertThat(activity.getSharedPreferences("com.mbientlab.temptracker",
                                0).getStringSet("saved_adapters", null).size(), comparesEqualTo(1));
                    }
                }
        );
    }

    @Test
    public void testAddingDuplicateBluetoothToMenu() {
        activity = getActivity();
        getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        activity.addBluetoothToMenuAndConnectionStatus("44:44:44:44:44:44");
                        int addedItemId = activity.getSharedPreferences("com.mbientlab.temptracker",
                                0).getInt("44:44:44:44:44:44_menu_id", 0);
                        activity.addBluetoothToMenuAndConnectionStatus("44:44:44:44:44:44");
                        int secondAddedItemId = activity.getSharedPreferences("com.mbientlab.temptracker",
                                0).getInt("44:44:44:44:44:44_menu_id", 0);
                        assertThat(addedItemId, comparesEqualTo(secondAddedItemId));
                        assertThat(activity.getMenu().findItem(addedItemId).getTitle().toString(),
                                comparesEqualTo("44:44:44:44:44:44 Connected"));
                        assertThat(activity.getSharedPreferences("com.mbientlab.temptracker",
                                0).getStringSet("saved_adapters", null).size(), comparesEqualTo(1));

                    }
                }
        );
    }

    @Test
    public void testAddingMultipleBluetoothAdaptersToMenu() {
        activity = getActivity();
        getInstrumentation().runOnMainSync(
                new Runnable() {
                                   @Override
                                   public void run() {
                                       activity.addBluetoothToMenuAndConnectionStatus("44:44:44:44:44:44");
                                       int addedItemId = activity.getSharedPreferences("com.mbientlab.temptracker",
                                               0).getInt("44:44:44:44:44:44_menu_id", 0);
                                       activity.addBluetoothToMenuAndConnectionStatus("44:44:44:44:44:66");
                                       int secondAddedItemId = activity.getSharedPreferences("com.mbientlab.temptracker",
                                               0).getInt("44:44:44:44:44:66_menu_id", 0);
                                       assertTrue(addedItemId != secondAddedItemId);
                                       assertThat(activity.getMenu().findItem(addedItemId).getTitle().toString(),
                                               comparesEqualTo("44:44:44:44:44:44"));
                                       assertThat(activity.getMenu().findItem(secondAddedItemId).getTitle().toString(),
                                               comparesEqualTo("44:44:44:44:44:66 Connected"));
                                       assertThat(activity.getSharedPreferences("com.mbientlab.temptracker",
                                               0).getStringSet("saved_adapters", null).size(), comparesEqualTo(2));

                                   }
                               }
        );
    }

    @Test
    public void testAddingMultipleBluetoothAdaptersToMenuSimulatingClosingSessionInBetween() {
        activity = getActivity();
        getInstrumentation().runOnMainSync(
                new Runnable() {
                                   @Override
                                   public void run() {
                                       activity.addBluetoothToMenuAndConnectionStatus("44:44:44:44:44:44");
                                       int addedItemId = activity.getSharedPreferences("com.mbientlab.temptracker",
                                               0).getInt("44:44:44:44:44:44_menu_id", 0);
                                       activity.getMenu().removeItem(addedItemId);
                                       activity.addBluetoothToMenuAndConnectionStatus("44:44:44:44:44:66");
                                       int secondAddedItemId = activity.getSharedPreferences("com.mbientlab.temptracker",
                                               0).getInt("44:44:44:44:44:66_menu_id", 0);
                                       assertTrue(addedItemId != secondAddedItemId);
                                       assertThat(activity.getMenu().findItem(addedItemId).getTitle().toString(),
                                               comparesEqualTo("44:44:44:44:44:44"));
                                       assertThat(activity.getMenu().findItem(secondAddedItemId).getTitle().toString(),
                                               comparesEqualTo("44:44:44:44:44:66 Connected"));
                                       assertThat(activity.getSharedPreferences("com.mbientlab.temptracker",
                                               0).getStringSet("saved_adapters", null).size(), comparesEqualTo(2));

                                   }
                               }
        );
    }

    @Test
    public void testSwitchingBetweenMultipleBluetoothAdaptersForMenu() {
        activity = getActivity();
        getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        activity.addBluetoothToMenuAndConnectionStatus("44:44:44:44:44:44");
                        int addedItemId = activity.getSharedPreferences("com.mbientlab.temptracker",
                                0).getInt("44:44:44:44:44:44_menu_id", 0);
                        activity.addBluetoothToMenuAndConnectionStatus("44:44:44:44:44:66");
                        int secondAddedItemId = activity.getSharedPreferences("com.mbientlab.temptracker",
                                0).getInt("44:44:44:44:44:66_menu_id", 0);
                        activity.addBluetoothToMenuAndConnectionStatus("44:44:44:44:44:44");
                        assertThat(activity.getSharedPreferences("com.mbientlab.temptracker",
                                0).getString("ble_mac_address", null), comparesEqualTo("44:44:44:44:44:44"));
                        assertThat(activity.getMenu().findItem(R.id.action_connect).getTitle().toString(),
                                comparesEqualTo(activity.getString(R.string.disconnect)));
                        assertThat(((TextView) activity.findViewById(R.id.connection_status)).getText().toString(),
                                comparesEqualTo(activity.getString(R.string.metawear_connected)));
                        assertTrue(addedItemId != secondAddedItemId);
                        assertThat(activity.getMenu().findItem(addedItemId).getTitle().toString(),
                                comparesEqualTo("44:44:44:44:44:44 Connected"));
                        assertThat(activity.getMenu().findItem(secondAddedItemId).getTitle().toString(),
                                comparesEqualTo("44:44:44:44:44:66"));
                        assertThat(activity.getSharedPreferences("com.mbientlab.temptracker",
                                0).getStringSet("saved_adapters", null).size(), comparesEqualTo(2));

                    }
                }
        );
    }

    @Test
    public void testRemovingBluetoothAdapterFromMenu() {
        activity = getActivity();
        getInstrumentation().runOnMainSync(new Runnable() {
                                   @Override
                                   public void run() {
                                       activity.addBluetoothToMenuAndConnectionStatus("44:44:44:44:44:44");
                                       int addedItemId = activity.getSharedPreferences("com.mbientlab.temptracker",
                                               0).getInt("44:44:44:44:44:44_menu_id", 0);
                                       activity.addBluetoothToMenuAndConnectionStatus("44:44:44:44:44:66");
                                       int secondAddedItemId = activity.getSharedPreferences("com.mbientlab.temptracker",
                                               0).getInt("44:44:44:44:44:66_menu_id", 0);
                                       assertTrue(addedItemId != secondAddedItemId);
                                       assertTrue(addedItemId != 0);
                                       assertTrue(secondAddedItemId != 0);
                                       assertThat(activity.getMenu().findItem(addedItemId).getTitle().toString(),
                                               comparesEqualTo("44:44:44:44:44:44"));
                                       assertThat(activity.getMenu().findItem(secondAddedItemId).getTitle().toString(),
                                               comparesEqualTo("44:44:44:44:44:66 Connected"));
                                       assertThat(activity.getSharedPreferences("com.mbientlab.temptracker",
                                               0).getStringSet("saved_adapters", null).size(), comparesEqualTo(2));
                                       activity.removeBluetoothFromMenu("44:44:44:44:44:66");
                                       assertNull(activity.getMenu().findItem(secondAddedItemId));
                                       assertThat(activity.getSharedPreferences("com.mbientlab.temptracker",
                                               0).getStringSet("saved_adapters", null).size(), comparesEqualTo(1));
                                       assertNull(activity.getSharedPreferences("com.mbientlab.temptracker",
                                               0).getStringSet("44:44:44:44:44:66_menu_id", null));
                                       assertThat(activity.getMenu().findItem(R.id.action_connect).getTitle().toString(),
                                               comparesEqualTo(activity.getString(R.string.connect)));
                                       assertNull(activity.getSharedPreferences("com.mbientlab.temptracker",
                                               0).getStringSet("ble_mac_address", null));
                                       assertThat(((TextView) activity.findViewById(R.id.connection_status)).getText().toString(),
                                               comparesEqualTo(activity.getString(R.string.no_metawear_connected)));

                                   }
                               }
        );
    }

    @Test
    public void testForgettingAdapter() {
        activity = getActivity();
        getInstrumentation().runOnMainSync(new Runnable() {
                                   @Override
                                   public void run() {
                                       assertThat(new Select().from(TemperatureSample.class).where(
                                               Condition.column(TemperatureSample$Table.MACADDRESS).eq("44:44:44:44:44:66")).queryList().size(),
                                               comparesEqualTo(0));
                                       (new TemperatureSample(new Date(System.currentTimeMillis()), 33L, "44:44:44:44:44:44")).save();
                                       (new TemperatureSample(new Date(System.currentTimeMillis()), 33L, "44:44:44:44:44:66")).save();
                                       activity.addBluetoothToMenuAndConnectionStatus("44:44:44:44:44:44");
                                       int addedItemId = activity.getSharedPreferences("com.mbientlab.temptracker",
                                               0).getInt("44:44:44:44:44:44_menu_id", 0);
                                       activity.addBluetoothToMenuAndConnectionStatus("44:44:44:44:44:66");
                                       activity.forgetDevice("44:44:44:44:44:66");
                                       assertThat(activity.getSharedPreferences("com.mbientlab.temptracker",
                                               0).getStringSet("saved_adapters", null).size(), comparesEqualTo(1));
                                       assertNull(activity.getSharedPreferences("com.mbientlab.temptracker",
                                               0).getStringSet("44:44:44:44:44:66_menu_id", null));
                                       assertThat(activity.getMenu().findItem(R.id.action_connect).getTitle().toString(),
                                               comparesEqualTo(activity.getString(R.string.connect)));
                                       assertNull(activity.getSharedPreferences("com.mbientlab.temptracker",
                                               0).getStringSet("ble_mac_address", null));
                                       assertThat(((TextView) activity.findViewById(R.id.connection_status)).getText().toString(),
                                               comparesEqualTo(activity.getString(R.string.no_metawear_connected)));
                                       assertThat(new Select().from(TemperatureSample.class).where(
                                               Condition.column(TemperatureSample$Table.MACADDRESS).eq("44:44:44:44:44:66")).queryList().size(),
                                               comparesEqualTo(0));
                                   }
                               }
        );
    }

    @After
    public void teardown() throws Exception {
        editor.remove("ble_mac_address");
        editor.remove("44:44:44:44:44:44_menu_id");
        editor.remove("44:44:44:44:44:66_menu_id");
        editor.remove("saved_adapters");
        editor.apply();
        editor.commit();
        super.tearDown();
    }
}
