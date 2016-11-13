package com.jyoti.screencapture;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by jyotisakhare on 13/11/16.
 */


public class SyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static SyncAdapter sSyncAdapter = null;

    /*
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new SyncAdapter(getApplicationContext(), true);
            }
        }
    }

    /*
     * {@inheritDoc}
     */
    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}

