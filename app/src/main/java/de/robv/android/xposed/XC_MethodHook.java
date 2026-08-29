package de.robv.android.xposed;

import de.robv.android.xposed.callbacks.IXUnhook;
import de.robv.android.xposed.callbacks.XCallback;
import java.lang.reflect.Member;

/* loaded from: /storage/emulated/0/Download/XposedBridgeAPI-82.dex */
public abstract class XC_MethodHook extends XCallback {

    public static final class MethodHookParam extends XCallback.Param {
        public Object[] args = null;
        public Member method;
        public Object thisObject;

        MethodHookParam() {
            throw new RuntimeException("Stub!");
        }

        public Object getResult() {
            throw new RuntimeException("Stub!");
        }

        public void setResult(Object result) {
            throw new RuntimeException("Stub!");
        }

        public Throwable getThrowable() {
            throw new RuntimeException("Stub!");
        }

        public boolean hasThrowable() {
            throw new RuntimeException("Stub!");
        }

        public void setThrowable(Throwable throwable) {
            throw new RuntimeException("Stub!");
        }

        public Object getResultOrThrowable() throws Throwable {
            throw new RuntimeException("Stub!");
        }
    }

    public class Unhook implements IXUnhook<XC_MethodHook> {
        Unhook() {
            throw new RuntimeException("Stub!");
        }

        public Member getHookedMethod() {
            throw new RuntimeException("Stub!");
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // de.robv.android.xposed.callbacks.IXUnhook
        public XC_MethodHook getCallback() {
            throw new RuntimeException("Stub!");
        }

        @Override // de.robv.android.xposed.callbacks.IXUnhook
        public void unhook() {
            throw new RuntimeException("Stub!");
        }
    }

    public XC_MethodHook() {
        throw new RuntimeException("Stub!");
    }

    public XC_MethodHook(int priority) {
        throw new RuntimeException("Stub!");
    }

    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        throw new RuntimeException("Stub!");
    }

    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        throw new RuntimeException("Stub!");
    }
}
