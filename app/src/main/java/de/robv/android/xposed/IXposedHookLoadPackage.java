package de.robv.android.xposed;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/* loaded from: /storage/emulated/0/Download/XposedBridgeAPI-82.dex */
public interface IXposedHookLoadPackage {
    void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable;
}
