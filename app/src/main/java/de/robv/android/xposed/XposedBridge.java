package de.robv.android.xposed;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.util.Set;

/* loaded from: /storage/emulated/0/Download/XposedBridgeAPI-82.dex */
public final class XposedBridge {
    public static final ClassLoader BOOTCLASSLOADER = null;

    @Deprecated
    public static int XPOSED_BRIDGE_VERSION;

    public static native int getXposedVersion();

    XposedBridge() {
        throw new RuntimeException("Stub!");
    }

    public static synchronized void log(String text) {
        throw new RuntimeException("Stub!");
    }

    public static synchronized void log(Throwable t) {
        throw new RuntimeException("Stub!");
    }

    public static XC_MethodHook.Unhook hookMethod(Member hookMethod, XC_MethodHook callback) {
        throw new RuntimeException("Stub!");
    }

    @Deprecated
    public static void unhookMethod(Member hookMethod, XC_MethodHook callback) {
        throw new RuntimeException("Stub!");
    }

    public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        throw new RuntimeException("Stub!");
    }

    public static Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> hookClass, XC_MethodHook callback) {
        throw new RuntimeException("Stub!");
    }

    public static Object invokeOriginalMethod(Member method, Object thisObject, Object[] args) throws IllegalAccessException, IllegalArgumentException, NullPointerException, InvocationTargetException {
        throw new RuntimeException("Stub!");
    }
}
