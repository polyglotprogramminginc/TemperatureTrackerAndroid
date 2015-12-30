package com.mbientlab.temperatureTracker;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.mbientlab.bletoolbox.scanner.BleScannerFragment;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.MetaWearBoard.ConnectionStateHandler;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.temperatureTracker.MWDeviceConfirmFragment.DeviceConfirmCallback;
import com.mbientlab.temperatureTracker.GraphFragment.GraphCallback;
import com.mbientlab.temperatureTracker.model.TemperatureSample;
import com.mbientlab.temperatureTracker.model.TemperatureSample$Table;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Delete;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

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
 * /* Software and/or its documentation for any purpose.
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

public class MainActivity extends ActionBarActivity implements ServiceConnection, DeviceConfirmCallback, GraphCallback,
        ThermistorFragment.ThermistorCallback, BleScannerFragment.ScannerCommunicationBus {
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private GraphFragment mGraphFragment;
    private final static int REQUEST_ENABLE_BT = 0;
    private final static String ACCELEROMETER_FRAGMENT_KEY = "AccelerometerFragment.ACCELEROMETER_FRAGMENT_KEY";
    private final static String GRAPH_FRAGMENT_KEY = "GraphFragment.GRAPH_FRAGMENT_KEY";
    private MetaWearBleService.LocalBinder mwBinder = null;
    private MetaWearBoard mwBoard = null;
    private MWScannerFragment mwScannerFragment = null;
    private ThermistorFragment thermistorFragment = null;
    private SharedPreferences sharedPreferences;
    private Editor editor;
    private BluetoothDevice bluetoothDevice;
    private BluetoothAdapter btAdapter;
    private Menu menu;
    private boolean btDeviceSelected;
    private Set<String> adapters;
    private boolean refresh = false;
    private boolean reset = false;
    private boolean reconnect = false;
    private boolean switching = false;

    /**
     * Standard Android lifecyle mehtods.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FlowManager.init(this);
        super.onCreate(savedInstanceState);
        sharedPreferences = getApplicationContext().getSharedPreferences("com.mbientlab.temptracker", 0); // 0 - for private mode
        editor = sharedPreferences.edit();
        setContentView(R.layout.activity_main);

        /** boilerplate code to create new or use existing fragment */
        if (savedInstanceState == null) {
            MainFragment mainFragment = new MainFragment();
            thermistorFragment = new ThermistorFragment();
            getFragmentManager().beginTransaction().add(R.id.container, mainFragment)
                    .add(R.id.container, thermistorFragment, ACCELEROMETER_FRAGMENT_KEY).commit();
            mGraphFragment = (GraphFragment)
                    getFragmentManager().findFragmentById(R.id.graph);
        } else {
            thermistorFragment = (ThermistorFragment) getFragmentManager().getFragment(savedInstanceState, ACCELEROMETER_FRAGMENT_KEY);
            mGraphFragment = (GraphFragment) getFragmentManager().getFragment(savedInstanceState, GRAPH_FRAGMENT_KEY);
        }

        /** code to set up the bluetooth adapter or give messages if it's not enabled or available */
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            btAdapter = bluetoothManager.getAdapter();
        }
        if (btAdapter == null) {
            new AlertDialog.Builder(this).setTitle(R.string.error_title)
                    .setMessage(R.string.error_no_bluetooth)
                    .setCancelable(false)
                    .setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            MainActivity.this.finish();
                        }
                    })
                    .create()
                    .show();
        } else if (!btAdapter.isEnabled()) {
            final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class),
                this, Context.BIND_AUTO_CREATE);
    }

    /**
     * lifecycle methods
     */
    @Override
    protected void onResume() {
        super.onResume();

        String bleMacAddress = sharedPreferences.getString("ble_mac_address", null);
        if (bleMacAddress != null && menu != null) {
            addBluetoothToMenuAndConnectionStatus(bleMacAddress);
        }

        thermistorFragment = (ThermistorFragment) getFragmentManager().findFragmentByTag(ACCELEROMETER_FRAGMENT_KEY);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Only show items in the action bar relevant to this screen
        // if the drawer is not showing. Otherwise, let the drawer
        // decide what to show in the action bar.
        getMenuInflater().inflate(R.menu.main, menu);
        this.menu = menu;
        String bleMacAddress = sharedPreferences.getString("ble_mac_address", null);
        if (bleMacAddress == null) {
            adapters = sharedPreferences.getStringSet("saved_adapters", null);
            if (adapters != null) {
                setAdaptersToDisconnected();
            }
        } else {
            addBluetoothToMenuAndConnectionStatus(bleMacAddress);
        }

        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        getApplicationContext().unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        ///< Get a reference to the MetaWear service from the binder
        mwBinder = (MetaWearBleService.LocalBinder) service;
        String bleMacAddress = sharedPreferences.getString("ble_mac_address", null);
        Log.i("Service Connected", "Stored mac address is " + bleMacAddress);
        if (bleMacAddress != null) {
            bluetoothDevice = btAdapter.getRemoteDevice(bleMacAddress);
            connectDevice(bluetoothDevice);

        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (mwBoard != null) {
            mwBoard.disconnect();
            editor.putString("ble_mac_address", mwBoard.getMacAddress());
            state.putByteArray(mwBoard.getMacAddress(), mwBoard.serializeState());
            editor.putString(mwBoard.getMacAddress(), new String(mwBoard.serializeState()));
            editor.apply();
            editor.commit();
        }
    }

    ///< Don't need this callback method but we must implement it
    @Override
    public void onServiceDisconnected(ComponentName name) {
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Switch demoSwitch = (Switch) findViewById(R.id.demo);
        if(demoSwitch.isChecked()){
            Toast.makeText(getApplicationContext(), R.string.toast_in_demo_mode, Toast.LENGTH_SHORT).show();
        }else {
            //noinspection SimplifiableIfStatement
            switch (id) {
                case R.id.action_connect:
                    if (mwBoard != null) {
                        mwBoard.disconnect();
                        editor.putString(mwBoard.getMacAddress(), new String(mwBoard.serializeState()));
                        editor.apply();
                        editor.commit();
                        disconnectAdapter();
                        if (adapters != null) {
                            setAdaptersToDisconnected();
                        }
                    } else {
                        if (mwScannerFragment != null) {
                            Fragment metawearBlescannerPopup = getFragmentManager().findFragmentById(R.id.metawear_blescanner_popup_fragment);
                            if(metawearBlescannerPopup != null) {
                                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                                fragmentTransaction.remove(metawearBlescannerPopup);
                                fragmentTransaction.commit();
                            }
                            mwScannerFragment.dismiss();
                        }
                        mwScannerFragment = new MWScannerFragment();
                        mwScannerFragment.show(getFragmentManager(), "metawear_scanner_fragment");
                    }
                    break;
                case R.id.action_reset_device:
                    if (mwBoard != null) {
                        if (mwBoard.isConnected()) {
                            try {
                                mwBoard.getModule(Debug.class).resetDevice();
                                Toast.makeText(this, R.string.message_soft_reset, Toast.LENGTH_LONG).show();
                                disconnectAdapter();
                                forgetDevice(bluetoothDevice.getAddress());
                            } catch (UnsupportedModuleException e) {
                                Toast.makeText(this, R.string.error_soft_reset, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            reset = true;
                            mwBoard.connect();
                        }
                    } else {
                        Toast.makeText(this, R.string.toast_no_board, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.action_clear_log:
                    new Delete().from(TemperatureSample.class).where(Condition.column(TemperatureSample$Table.MACADDRESS).eq(bluetoothDevice.getAddress())).async().execute();
                    mGraphFragment.zeroOutReadings();
                    break;
                case R.id.action_forget:
                    disconnectAdapter();
                    forgetDevice(bluetoothDevice.getAddress());
                    break;
                case R.id.action_refresh:
                    if (mwBoard == null) {
                        Toast.makeText(getApplicationContext(), R.string.toast_disconnected, Toast.LENGTH_SHORT).show();
                    } else if (!mwBoard.isConnected()) {
                        Log.i("Main Activity", "connecting");
                        mwBoard.connect();
                        refresh = true;
                    } else {
                        Log.i("Main Activity", "staring download");
                        thermistorFragment.startLogDownload(mwBoard, sharedPreferences);
                    }
                    break;
                default:
                    String bleMacAddress = menu.findItem(id).getTitle().toString();
                    if (bleMacAddress != null) {
                        editor.putString("ble_mac_address", bleMacAddress);
                        editor.apply();
                        editor.commit();
                        mGraphFragment.updateGraph();
                        if (mwBoard != null) {
                            mwBoard.disconnect();
                            editor.putString(mwBoard.getMacAddress(), new String(mwBoard.serializeState()));
                            editor.apply();
                            editor.commit();
                            switching = true;
                            disconnectAdapter();
                            editor.putString("ble_mac_address", bleMacAddress);
                            editor.apply();
                            editor.commit();
                        } else {
                            bluetoothDevice = btAdapter.getRemoteDevice(bleMacAddress);
                            reconnect = true;
                            connectDevice(bluetoothDevice);
                        }
                    }
                    Log.i("Menu Item", String.valueOf(id));
                    Log.i("Menu Item", menu.findItem(id).getTitle().toString());
            }
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * callbacks for Bluetooth device scan
     */
    @Override
    public void onDeviceSelected(BluetoothDevice device) {
        bluetoothDevice = device;
        btDeviceSelected = true;
        connectDevice(device);
        Fragment metawearBlescannerPopup = getFragmentManager().findFragmentById(R.id.metawear_blescanner_popup_fragment);
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.remove(metawearBlescannerPopup);
        fragmentTransaction.commit();
        mwScannerFragment.dismiss();
    }

    @Override
    public UUID[] getFilterServiceUuids() {
        ///< Only return MetaWear boards in the scan
        return new UUID[]{UUID.fromString("326a9000-85cb-9195-d9dd-464cfbbae75a")};
    }

    @Override
    public long getScanDuration() {
        ///< Scan for 10000ms (10 seconds)
        return 10000;
    }

    /**
     * Device confirmation callbacks and helper methods
     */

    public void pairDevice() {
        if (thermistorFragment.setupThermistorAndLogs(mwBoard, editor)) {
            addBluetoothToMenuAndConnectionStatus(bluetoothDevice.getAddress());
        } else {
            Toast.makeText(getApplicationContext(), R.string.thermistor_not_supported, Toast.LENGTH_SHORT).show();
            mwBoard.disconnect();
            bluetoothDevice = null;
            MenuItem connectMenuItem = menu.findItem(R.id.action_connect);
            connectMenuItem.setTitle(R.string.connect);
        }
    }

    public void dontPairDevice() {
        mwBoard.disconnect();
        bluetoothDevice = null;
        mwScannerFragment.show(getFragmentManager(), "metawear_scanner_fragment");
    }

    /**
     * Thermistor fragment callbacks
     *
     * @return Bluetooth Device
     */
    @Override
    public GraphFragment getGraphFragment() {
        return mGraphFragment;
    }

    @Override
    public void startDownload() {
        TextView connectionStatus = (TextView) findViewById(R.id.connection_status);
        connectionStatus.setText(getText(R.string.metawear_syncing));
    }

    @Override
    public void totalDownloadEntries(int entries) {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.download_progress);
        progressBar.setMax(entries);
    }

    @Override
    public void downloadProgress(int entriesDownloaded) {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.download_progress);
        progressBar.setProgress(entriesDownloaded);
    }

    @Override
    public void downloadFinished() {
        mwBoard.disconnect();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView connectionStatus = (TextView) findViewById(R.id.connection_status);
                connectionStatus.setText(getText(R.string.metawear_connected));
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.download_progress);
                progressBar.setProgress(0);
            }
        });
    }

    /**
     * Graph fragment callbacks
     *
     * @return bluetooth Device
     */
    @Override
    public String getBluetoothDevice() {
        return sharedPreferences.getString("ble_mac_address", null);
    }

    @Override
    public void setGraphFragment(GraphFragment graphFragment) {
        mGraphFragment = graphFragment;
    }

    /**
     * private helper method for device connection logic
     */
    private void connectDevice(BluetoothDevice device) {
        mwBoard = mwBinder.getMetaWearBoard(device);

        String bleMacAddress = sharedPreferences.getString("ble_mac_address", null);
        if (bleMacAddress != null) {
            String boardState = sharedPreferences.getString(bleMacAddress, null);
            if (boardState != null) {
                mwBoard.deserializeState(boardState.getBytes());
                Log.i("connect device", "Found instance state");
            } else {
                Log.i("connect device", "Instance not set up correctly");
                editor.remove("ble_mac_address");
                editor.apply();
                editor.commit();
                mwBoard = null;
                bluetoothDevice = null;
            }
        } else {
            Log.i("connect device", "No prior instance state");
        }

        if (mwBoard != null) {

            mwBoard.setConnectionStateHandler(connectionStateHandler);

            mwBoard.connect();
        }
    }

    /**
     * connection handlers
     */
    private ConnectionStateHandler connectionStateHandler = new ConnectionStateHandler() {
        @Override
        public void connected() {
            Log.i("Metawear Controller", "Device Connected");
            runOnUiThread(new Runnable() {
                              @Override
                              public void run() {
                                  Toast.makeText(getApplicationContext(), R.string.toast_connected, Toast.LENGTH_SHORT).show();
                              }
                          }
            );
            if (btDeviceSelected) {
                if ((adapters == null) || !adapters.contains(mwBoard.getMacAddress())) {
                    MWDeviceConfirmFragment mwDeviceConfirmFragment = new MWDeviceConfirmFragment();
                    mwDeviceConfirmFragment.flashDeviceLight(mwBoard, getFragmentManager());
                    btDeviceSelected = false;
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addBluetoothToMenuAndConnectionStatus(mwBoard.getMacAddress());
                            mGraphFragment.updateGraph();
                        }
                    });
                }
            } else if (refresh) {
                refresh = false;
                thermistorFragment.startLogDownload(mwBoard, sharedPreferences);
            } else if (reset) {
                try {
                    mwBoard.getModule(Debug.class).resetDevice();
                } catch (Exception e) {
                    runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    disconnectAdapter();
                                    forgetDevice(bluetoothDevice.getAddress());
                                    Toast.makeText(getApplicationContext(), R.string.error_soft_reset, Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                }
                reset = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        disconnectAdapter();
                        forgetDevice(bluetoothDevice.getAddress());
                    }
                });
            } else if (reconnect) {
                reconnect = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addBluetoothToMenuAndConnectionStatus(mwBoard.getMacAddress());
                        mGraphFragment.updateGraph();
                    }
                });
            }
        }

        @Override
        public void disconnected() {
            Log.i("Metawear Controler", "Device Disconnected");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), R.string.toast_disconnected, Toast.LENGTH_SHORT).show();
                    if(switching){
                        switching = false;
                        bluetoothDevice = btAdapter.getRemoteDevice(sharedPreferences.getString("ble_mac_address", null));
                        reconnect = true;
                        connectDevice(bluetoothDevice);
                    }
                }
            });

        }
    };

    /**
     * Local methods to add and remove device addresses from the menu
     */
    private void setAdaptersToDisconnected() {
        for (String adapter : adapters) {
            int menuId = sharedPreferences.getInt(adapter + "_menu_id", -1);
            MenuItem menuItem = menu.findItem(menuId);
            if (menuItem == null) {
                menu.add(0, menuId, 0, adapter).getItemId();
            } else {
                menuItem.setTitle(adapter);
            }
        }
    }

    public void forgetDevice(String bluetoothAddress) {
        removeBluetoothFromMenu(bluetoothAddress);
        new Delete().from(TemperatureSample.class).where(Condition.column(TemperatureSample$Table.MACADDRESS).eq(bluetoothAddress)).async().execute();
        mwBoard = null;
        mGraphFragment.zeroOutReadings();
    }

    public void addBluetoothToMenuAndConnectionStatus(String bluetoothAddress) {
        int existingMenuId = sharedPreferences.getInt(bluetoothAddress + "_menu_id", -1);
        adapters = sharedPreferences.getStringSet("saved_adapters", null);

        if (existingMenuId == -1) {
            int menuId = (int) (Math.random() * 1000000000);
            editor.putInt(bluetoothAddress + "_menu_id", menu.add(0, menuId, 0, bluetoothAddress + " Connected").getItemId());
            if (adapters == null) {
                adapters = new LinkedHashSet<>();
            } else {
                setAdaptersToDisconnected();
            }
        } else {
            setAdaptersToDisconnected();
            menu.findItem(existingMenuId).setTitle(bluetoothAddress + " Connected");
        }

        MenuItem connectMenuItem = menu.findItem(R.id.action_connect);
        connectMenuItem.setTitle(R.string.disconnect);
        TextView connectionStatus = (TextView) findViewById(R.id.connection_status);
        connectionStatus.setText(getText(R.string.metawear_connected));

        adapters.add(bluetoothAddress);
        editor.putString("ble_mac_address", bluetoothAddress);
        editor.apply();
        editor.commit();
        editor.putStringSet("saved_adapters", adapters);
        editor.apply();
        editor.commit();
    }

    private void setStatusToDisconnected() {
        MenuItem connectMenuItem = menu.findItem(R.id.action_connect);
        connectMenuItem.setTitle(R.string.connect);
        editor.remove("ble_mac_address");
        editor.commit();
        TextView connectionStatus = (TextView) findViewById(R.id.connection_status);
        connectionStatus.setText(getText(R.string.no_metawear_connected));
    }

    public void removeBluetoothFromMenu(String bluetoothAddress) {
        adapters.remove(bluetoothAddress);
        int menuId = sharedPreferences.getInt(bluetoothAddress + "_menu_id", -1);
        menu.removeItem(menuId);
        setStatusToDisconnected();
        editor.putStringSet("saved_adapters", adapters);
        editor.apply();
        editor.commit();
        editor.remove(bluetoothAddress + "_menu_id");
        editor.apply();
        editor.commit();
    }

    /**
     * local helper method for disconnecting from a board
     */
    private void disconnectAdapter() {
        mwBoard.disconnect();
        mwBoard = null;
        setStatusToDisconnected();
    }

    public MetaWearBoard getMwBoard() {
        return mwBoard;
    }

    public BluetoothDevice getActivityBluetoothDevice() {
        return bluetoothDevice;
    }

    public Menu getMenu() {
        return menu;
    }
}
