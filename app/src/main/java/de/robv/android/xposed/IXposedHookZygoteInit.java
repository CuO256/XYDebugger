package de.robv.android.xposed;

/* loaded from: /storage/emulated/0/Download/XposedBridgeAPI-82.dex */
public interface IXposedHookZygoteInit {
    void initZygote(StartupParam startupParam) throws Throwable;

    public static final class StartupParam {
        public String modulePath;
        public boolean startsSystemServer;

        StartupParam() {
            throw new RuntimeException("Stub!");
        }
    }
}
