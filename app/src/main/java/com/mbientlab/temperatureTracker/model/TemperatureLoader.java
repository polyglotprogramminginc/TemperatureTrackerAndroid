package com.mbientlab.temperatureTracker.model;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.raizlabs.android.dbflow.runtime.FlowContentObserver;
import com.raizlabs.android.dbflow.sql.language.Select;

import java.util.List;
import java.util.concurrent.locks.Condition;

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
 * Created by Lance Gleason of Polyglot Programming LLC. on 12/21/15.
 * http://www.polyglotprogramminginc.com
 * https://github.com/lgleasain
 * Twitter: @lgleasain
 */
public class TemperatureLoader extends AsyncTaskLoader<List<TemperatureSample>> {

    private List<TemperatureSample> mData;
    boolean isLoadingData;
    FlowContentObserver mObserver;
    String macAddress;

    public TemperatureLoader(Context context){
        super(context);
    }

    @Override
    public void deliverResult(List<TemperatureSample> data){
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            onReleaseResources(data);
            return;
        }

        // Hold a reference to the old data so it doesn't get garbage collected.
        // We must protect it until the new data has been delivered.
        List<TemperatureSample> oldData = mData;
        mData = data;

        if (isStarted()) {
            // If the Loader is in a started state, deliver the results to the
            // client. The superclass method does this for us.
            super.deliverResult(data);
        }

        // Invalidate the old data as we don't need it any more.
        if (oldData != null && oldData != data) {
            onReleaseResources(oldData);
        }
    }

    @Override
    protected void onStartLoading() {
        if (mData != null) {
            // Deliver any previously loaded data immediately.
            deliverResult(mData);
        }

        // Begin monitoring the underlying data source.
        if (mObserver == null) {
            mObserver = new FlowContentObserver();

            // registers for callbacks from the specified table
            mObserver.registerForContentChanges(getContext(), TemperatureSample.class);
        }

        if (takeContentChanged() || mData == null) {
            // When the observer detects a change, it should call onContentChanged()
            // on the Loader, which will cause the next call to takeContentChanged()
            // to return true. If this is ever the case (or if the current data is
            // null), we force a new load.
            forceLoad();
        }
    }

    protected void onReleaseResources(List<TemperatureSample> apps) {
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override public void onCanceled(List<TemperatureSample> apps) {
        super.onCanceled(apps);

        // At this point we can release the resources associated with 'apps'
        // if needed.
        onReleaseResources(apps);
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release the resources associated with 'apps'
        // if needed.
        if (mData != null) {
            onReleaseResources(mData);
            mData = null;
        }

        // Stop monitoring for changes.
        if (mObserver != null) {
            mObserver.unregisterForContentChanges(getContext());
            mObserver = null;
        }
    }

    @Override public List<TemperatureSample> loadInBackground() {
        // Retrieve all known applications.
        isLoadingData = true;
        List<TemperatureSample> persistedSamples = new Select().from(TemperatureSample.class).where(
                com.raizlabs.android.dbflow.sql.builder.Condition.column(TemperatureSample$Table.MACADDRESS
                ).eq(macAddress)).orderBy(false, TemperatureSample$Table.DATE).limit(60).queryList();
        // Done!
        isLoadingData = false;
        return persistedSamples;
    }

    public boolean isLoadingData() {
        return isLoadingData;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
}
