package com.jyoti.screencapture;

/**
 * Created by jyotisakhare on 13/11/16.
 */

public enum SyncStatus {
    SYNCED(1),
    NEVER_SYNCED(2);

    private int code;

    SyncStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
