package de.robv.android.xposed.callbacks;

import android.content.res.XResources;
import android.view.View;
import de.robv.android.xposed.callbacks.XCallback;

/* loaded from: /storage/emulated/0/Download/XposedBridgeAPI-82.dex */
public abstract class XC_LayoutInflated extends XCallback {
    public abstract void handleLayoutInflated(LayoutInflatedParam layoutInflatedParam) throws Throwable;

    public static final class LayoutInflatedParam extends XCallback.Param {
        public XResources res;
        public XResources.ResourceNames resNames;
        public String variant;
        public View view;

        LayoutInflatedParam() {
            throw new RuntimeException("Stub!");
        }
    }

    public class Unhook implements IXUnhook<XC_LayoutInflated> {
        Unhook() {
            throw new RuntimeException("Stub!");
        }

        public int getId() {
            throw new RuntimeException("Stub!");
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // de.robv.android.xposed.callbacks.IXUnhook
        public XC_LayoutInflated getCallback() {
            throw new RuntimeException("Stub!");
        }

        @Override // de.robv.android.xposed.callbacks.IXUnhook
        public void unhook() {
            throw new RuntimeException("Stub!");
        }
    }

    public XC_LayoutInflated() {
        throw new RuntimeException("Stub!");
    }

    public XC_LayoutInflated(int priority) {
        throw new RuntimeException("Stub!");
    }
}
