package de.robv.android.xposed.callbacks;

import android.os.Bundle;

/* loaded from: /storage/emulated/0/Download/XposedBridgeAPI-82.dex */
public abstract class XCallback {
    public static final int PRIORITY_DEFAULT = 50;
    public static final int PRIORITY_HIGHEST = 10000;
    public static final int PRIORITY_LOWEST = -10000;
    public final int priority;

    public static abstract class Param {
        @Deprecated
        protected Param() {
            throw new RuntimeException("Stub!");
        }

        public synchronized Bundle getExtra() {
            throw new RuntimeException("Stub!");
        }

        public Object getObjectExtra(String key) {
            throw new RuntimeException("Stub!");
        }

        public void setObjectExtra(String key, Object o) {
            throw new RuntimeException("Stub!");
        }
    }

    @Deprecated
    public XCallback() {
        throw new RuntimeException("Stub!");
    }
}
