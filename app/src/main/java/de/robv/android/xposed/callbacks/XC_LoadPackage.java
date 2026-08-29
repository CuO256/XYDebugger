package de.robv.android.xposed.callbacks;

import android.content.pm.ApplicationInfo;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XCallback;

/* loaded from: /storage/emulated/0/Download/XposedBridgeAPI-82.dex */
public abstract class XC_LoadPackage extends XCallback implements IXposedHookLoadPackage {

    public static final class LoadPackageParam extends XCallback.Param {
        public ApplicationInfo appInfo;
        public ClassLoader classLoader;
        public boolean isFirstApplication;
        public String packageName;
        public String processName;

        LoadPackageParam() {
            throw new RuntimeException("Stub!");
        }
    }

    XC_LoadPackage() {
        throw new RuntimeException("Stub!");
    }
}
