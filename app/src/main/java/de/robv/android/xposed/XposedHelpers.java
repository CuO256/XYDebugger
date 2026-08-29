package de.robv.android.xposed;

import android.content.res.Resources;
import de.robv.android.xposed.XC_MethodHook;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/* loaded from: /storage/emulated/0/Download/XposedBridgeAPI-82.dex */
public final class XposedHelpers {

    public static final class ClassNotFoundError extends Error {
        ClassNotFoundError() {
            throw new RuntimeException("Stub!");
        }
    }

    public static final class InvocationTargetError extends Error {
        InvocationTargetError() {
            throw new RuntimeException("Stub!");
        }
    }

    XposedHelpers() {
        throw new RuntimeException("Stub!");
    }

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        throw new RuntimeException("Stub!");
    }

    public static Class<?> findClassIfExists(String className, ClassLoader classLoader) {
        throw new RuntimeException("Stub!");
    }

    public static Field findField(Class<?> clazz, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static Field findFieldIfExists(Class<?> clazz, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static Field findFirstFieldByExactType(Class<?> clazz, Class<?> type) {
        throw new RuntimeException("Stub!");
    }

    public static XC_MethodHook.Unhook findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        throw new RuntimeException("Stub!");
    }

    public static XC_MethodHook.Unhook findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        throw new RuntimeException("Stub!");
    }

    public static Method findMethodExact(Class<?> clazz, String methodName, Object... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Method findMethodExactIfExists(Class<?> clazz, String methodName, Object... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Method findMethodExact(String className, ClassLoader classLoader, String methodName, Object... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Method findMethodExactIfExists(String className, ClassLoader classLoader, String methodName, Object... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Method findMethodExact(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Method[] findMethodsByExactParameters(Class<?> clazz, Class<?> returnType, Class<?>... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Object... args) {
        throw new RuntimeException("Stub!");
    }

    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object[] args) {
        throw new RuntimeException("Stub!");
    }

    public static Class<?>[] getParameterTypes(Object... args) {
        throw new RuntimeException("Stub!");
    }

    public static Class<?>[] getClassesAsArray(Class<?>... clazzes) {
        throw new RuntimeException("Stub!");
    }

    public static Constructor<?> findConstructorExact(Class<?> clazz, Object... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Constructor<?> findConstructorExactIfExists(Class<?> clazz, Object... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Constructor<?> findConstructorExact(String className, ClassLoader classLoader, Object... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Constructor<?> findConstructorExactIfExists(String className, ClassLoader classLoader, Object... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Constructor<?> findConstructorExact(Class<?> clazz, Class<?>... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static XC_MethodHook.Unhook findAndHookConstructor(Class<?> clazz, Object... parameterTypesAndCallback) {
        throw new RuntimeException("Stub!");
    }

    public static XC_MethodHook.Unhook findAndHookConstructor(String className, ClassLoader classLoader, Object... parameterTypesAndCallback) {
        throw new RuntimeException("Stub!");
    }

    public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Class<?>... parameterTypes) {
        throw new RuntimeException("Stub!");
    }

    public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Object... args) {
        throw new RuntimeException("Stub!");
    }

    public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Class<?>[] parameterTypes, Object[] args) {
        throw new RuntimeException("Stub!");
    }

    public static void setObjectField(Object obj, String fieldName, Object value) {
        throw new RuntimeException("Stub!");
    }

    public static void setBooleanField(Object obj, String fieldName, boolean value) {
        throw new RuntimeException("Stub!");
    }

    public static void setByteField(Object obj, String fieldName, byte value) {
        throw new RuntimeException("Stub!");
    }

    public static void setCharField(Object obj, String fieldName, char value) {
        throw new RuntimeException("Stub!");
    }

    public static void setDoubleField(Object obj, String fieldName, double value) {
        throw new RuntimeException("Stub!");
    }

    public static void setFloatField(Object obj, String fieldName, float value) {
        throw new RuntimeException("Stub!");
    }

    public static void setIntField(Object obj, String fieldName, int value) {
        throw new RuntimeException("Stub!");
    }

    public static void setLongField(Object obj, String fieldName, long value) {
        throw new RuntimeException("Stub!");
    }

    public static void setShortField(Object obj, String fieldName, short value) {
        throw new RuntimeException("Stub!");
    }

    public static Object getObjectField(Object obj, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static Object getSurroundingThis(Object obj) {
        throw new RuntimeException("Stub!");
    }

    public static boolean getBooleanField(Object obj, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static byte getByteField(Object obj, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static char getCharField(Object obj, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static double getDoubleField(Object obj, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static float getFloatField(Object obj, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static int getIntField(Object obj, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static long getLongField(Object obj, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static short getShortField(Object obj, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) {
        throw new RuntimeException("Stub!");
    }

    public static void setStaticBooleanField(Class<?> clazz, String fieldName, boolean value) {
        throw new RuntimeException("Stub!");
    }

    public static void setStaticByteField(Class<?> clazz, String fieldName, byte value) {
        throw new RuntimeException("Stub!");
    }

    public static void setStaticCharField(Class<?> clazz, String fieldName, char value) {
        throw new RuntimeException("Stub!");
    }

    public static void setStaticDoubleField(Class<?> clazz, String fieldName, double value) {
        throw new RuntimeException("Stub!");
    }

    public static void setStaticFloatField(Class<?> clazz, String fieldName, float value) {
        throw new RuntimeException("Stub!");
    }

    public static void setStaticIntField(Class<?> clazz, String fieldName, int value) {
        throw new RuntimeException("Stub!");
    }

    public static void setStaticLongField(Class<?> clazz, String fieldName, long value) {
        throw new RuntimeException("Stub!");
    }

    public static void setStaticShortField(Class<?> clazz, String fieldName, short value) {
        throw new RuntimeException("Stub!");
    }

    public static Object getStaticObjectField(Class<?> clazz, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static boolean getStaticBooleanField(Class<?> clazz, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static byte getStaticByteField(Class<?> clazz, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static char getStaticCharField(Class<?> clazz, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static double getStaticDoubleField(Class<?> clazz, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static float getStaticFloatField(Class<?> clazz, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static int getStaticIntField(Class<?> clazz, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static long getStaticLongField(Class<?> clazz, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static short getStaticShortField(Class<?> clazz, String fieldName) {
        throw new RuntimeException("Stub!");
    }

    public static Object callMethod(Object obj, String methodName, Object... args) {
        throw new RuntimeException("Stub!");
    }

    public static Object callMethod(Object obj, String methodName, Class<?>[] parameterTypes, Object... args) {
        throw new RuntimeException("Stub!");
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        throw new RuntimeException("Stub!");
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object... args) {
        throw new RuntimeException("Stub!");
    }

    public static Object newInstance(Class<?> clazz, Object... args) {
        throw new RuntimeException("Stub!");
    }

    public static Object newInstance(Class<?> clazz, Class<?>[] parameterTypes, Object... args) {
        throw new RuntimeException("Stub!");
    }

    public static Object setAdditionalInstanceField(Object obj, String key, Object value) {
        throw new RuntimeException("Stub!");
    }

    public static Object getAdditionalInstanceField(Object obj, String key) {
        throw new RuntimeException("Stub!");
    }

    public static Object removeAdditionalInstanceField(Object obj, String key) {
        throw new RuntimeException("Stub!");
    }

    public static Object setAdditionalStaticField(Object obj, String key, Object value) {
        throw new RuntimeException("Stub!");
    }

    public static Object getAdditionalStaticField(Object obj, String key) {
        throw new RuntimeException("Stub!");
    }

    public static Object removeAdditionalStaticField(Object obj, String key) {
        throw new RuntimeException("Stub!");
    }

    public static Object setAdditionalStaticField(Class<?> clazz, String key, Object value) {
        throw new RuntimeException("Stub!");
    }

    public static Object getAdditionalStaticField(Class<?> clazz, String key) {
        throw new RuntimeException("Stub!");
    }

    public static Object removeAdditionalStaticField(Class<?> clazz, String key) {
        throw new RuntimeException("Stub!");
    }

    public static byte[] assetAsByteArray(Resources res, String path) throws IOException {
        throw new RuntimeException("Stub!");
    }

    public static String getMD5Sum(String file) throws IOException {
        throw new RuntimeException("Stub!");
    }

    public static int incrementMethodDepth(String method) {
        throw new RuntimeException("Stub!");
    }

    public static int decrementMethodDepth(String method) {
        throw new RuntimeException("Stub!");
    }

    public static int getMethodDepth(String method) {
        throw new RuntimeException("Stub!");
    }
}
