package de.robv.android.xposed.services;

import java.io.IOException;
import java.io.InputStream;

/* loaded from: /storage/emulated/0/Download/XposedBridgeAPI-82.dex */
public abstract class BaseService {
    public static final int F_OK = 0;
    public static final int R_OK = 4;
    public static final int W_OK = 2;
    public static final int X_OK = 1;

    public abstract boolean checkFileAccess(String str, int i);

    public abstract FileResult readFile(String str, int i, int i2, long j, long j2) throws IOException;

    public abstract FileResult readFile(String str, long j, long j2) throws IOException;

    public abstract byte[] readFile(String str) throws IOException;

    public abstract FileResult statFile(String str) throws IOException;

    BaseService() {
        throw new RuntimeException("Stub!");
    }

    public boolean hasDirectFileAccess() {
        throw new RuntimeException("Stub!");
    }

    public boolean checkFileExists(String filename) {
        throw new RuntimeException("Stub!");
    }

    public long getFileSize(String filename) throws IOException {
        throw new RuntimeException("Stub!");
    }

    public long getFileModificationTime(String filename) throws IOException {
        throw new RuntimeException("Stub!");
    }

    public InputStream getFileInputStream(String filename) throws IOException {
        throw new RuntimeException("Stub!");
    }

    public FileResult getFileInputStream(String filename, long previousSize, long previousTime) throws IOException {
        throw new RuntimeException("Stub!");
    }
}
