package de.robv.android.xposed;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import java.io.File;
import java.util.Map;
import java.util.Set;

/* loaded from: /storage/emulated/0/Download/XposedBridgeAPI-82.dex */
public final class XSharedPreferences implements SharedPreferences {
    public XSharedPreferences(File prefFile) {
        throw new RuntimeException("Stub!");
    }

    public XSharedPreferences(String packageName) {
        throw new RuntimeException("Stub!");
    }

    public XSharedPreferences(String packageName, String prefFileName) {
        throw new RuntimeException("Stub!");
    }

    @SuppressLint({"SetWorldReadable"})
    public boolean makeWorldReadable() {
        throw new RuntimeException("Stub!");
    }

    public File getFile() {
        throw new RuntimeException("Stub!");
    }

    public synchronized void reload() {
        throw new RuntimeException("Stub!");
    }

    public synchronized boolean hasFileChanged() {
        throw new RuntimeException("Stub!");
    }

    @Override // android.content.SharedPreferences
    @Deprecated
    public SharedPreferences.Editor edit() {
        throw new RuntimeException("Stub!");
    }

    @Override // android.content.SharedPreferences
    @Deprecated
    public void registerOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        throw new RuntimeException("Stub!");
    }

    @Override // android.content.SharedPreferences
    @Deprecated
    public void unregisterOnSharedPreferenceChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        throw new RuntimeException("Stub!");
    }

    @Override // android.content.SharedPreferences
    public Map<String, ?> getAll() {
        throw new RuntimeException("Stub!");
    }

    @Override // android.content.SharedPreferences
    public String getString(String key, String defValue) {
        throw new RuntimeException("Stub!");
    }

    @Override // android.content.SharedPreferences
    public Set<String> getStringSet(String key, Set<String> defValues) {
        throw new RuntimeException("Stub!");
    }

    @Override // android.content.SharedPreferences
    public int getInt(String key, int defValue) {
        throw new RuntimeException("Stub!");
    }

    @Override // android.content.SharedPreferences
    public long getLong(String key, long defValue) {
        throw new RuntimeException("Stub!");
    }

    @Override // android.content.SharedPreferences
    public float getFloat(String key, float defValue) {
        throw new RuntimeException("Stub!");
    }

    @Override // android.content.SharedPreferences
    public boolean getBoolean(String key, boolean defValue) {
        throw new RuntimeException("Stub!");
    }

    @Override // android.content.SharedPreferences
    public boolean contains(String key) {
        throw new RuntimeException("Stub!");
    }
}
