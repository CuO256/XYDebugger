package de.robv.android.xposed.callbacks;

import android.content.res.XResources;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.callbacks.XCallback;

/* loaded from: /storage/emulated/0/Download/XposedBridgeAPI-82.dex */
public abstract class XC_InitPackageResources extends XCallback implements IXposedHookInitPackageResources {

    public static final class InitPackageResourcesParam extends XCallback.Param {
        public String packageName;
        public XResources res;

        InitPackageResourcesParam() {
            throw new RuntimeException("Stub!");
        }
    }

    XC_InitPackageResources() {
        throw new RuntimeException("Stub!");
    }
}
