package de.robv.android.xposed;

import de.robv.android.xposed.XC_MethodHook;

/* loaded from: /storage/emulated/0/Download/XposedBridgeAPI-82.dex */
public abstract class XC_MethodReplacement extends XC_MethodHook {
    public static final XC_MethodReplacement DO_NOTHING = null;

    protected abstract Object replaceHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) throws Throwable;

    public XC_MethodReplacement() {
        throw new RuntimeException("Stub!");
    }

    public XC_MethodReplacement(int priority) {
        throw new RuntimeException("Stub!");
    }

    public static XC_MethodReplacement returnConstant(Object result) {
        throw new RuntimeException("Stub!");
    }

    public static XC_MethodReplacement returnConstant(int priority, Object result) {
        throw new RuntimeException("Stub!");
    }
}
