package de.robv.android.xposed.callbacks;

/* loaded from: /storage/emulated/0/Download/XposedBridgeAPI-82.dex */
public interface IXUnhook<T> {
    T getCallback();

    void unhook();
}
