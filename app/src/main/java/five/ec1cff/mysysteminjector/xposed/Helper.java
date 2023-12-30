package five.ec1cff.mysysteminjector.xposed;

import android.annotation.SuppressLint;

import androidx.annotation.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XposedBridge;
import sun.misc.Unsafe;

// https://github.com/LSPosed/AndroidHiddenApiBypass/blob/2e46e453c83035d201a90cc05cfd2a7aa0922fa7/library/src/main/java/org/lsposed/hiddenapibypass/HiddenApiBypass.java#L206
@SuppressLint({"SoonBlockedPrivateApi", "DiscouragedPrivateApi"})
public class Helper {
    private final static Method deoptimizeMethod;

    static {
        Method m = null;
        try {
            m = XposedBridge.class.getDeclaredMethod("deoptimizeMethod", Member.class);
        } catch (Throwable t) {
            XposedBridge.log("cannot get deoptimizeMethod");
        }
        deoptimizeMethod = m;
    }

    static void deoptimizeMethod(Class<?> c, String n) throws InvocationTargetException, IllegalAccessException {
        for (Method m : c.getDeclaredMethods()) {
            if (deoptimizeMethod != null && m.getName().equals(n)) {
                deoptimizeMethod.invoke(null, m);
            }
        }
    }

    private static final Unsafe unsafe;
    private static final long methodsOffset;
    private static final long artMethodSize;
    private static final long artMethodBias;
    private static final long artOffset;
    private static final long infoOffset;
    private static final long memberOffset;

    private static class NeverCall {
        private static void a() {
        }

        private static void b() {
        }
    }

    static {
        try {
            unsafe = (Unsafe) Unsafe.class.getDeclaredMethod("getUnsafe").invoke(null);
            assert unsafe != null;
            artOffset = unsafe.objectFieldOffset(MethodHandle.class.getDeclaredField("artFieldOrMethod"));
            infoOffset = unsafe.objectFieldOffset(Class.forName("java.lang.invoke.MethodHandleImpl").getDeclaredField("info"));
            methodsOffset = unsafe.objectFieldOffset(Class.class.getDeclaredField("methods"));
            memberOffset = unsafe.objectFieldOffset(Class.forName("java.lang.invoke.MethodHandleImpl$HandleInfo").getDeclaredField("member"));
            Method mA = Helper.NeverCall.class.getDeclaredMethod("a");
            Method mB = Helper.NeverCall.class.getDeclaredMethod("b");
            mA.setAccessible(true);
            mB.setAccessible(true);
            MethodHandle mhA = MethodHandles.lookup().unreflect(mA);
            MethodHandle mhB = MethodHandles.lookup().unreflect(mB);
            long aAddr = unsafe.getLong(mhA, artOffset);
            long bAddr = unsafe.getLong(mhB, artOffset);
            long aMethods = unsafe.getLong(Helper.NeverCall.class, methodsOffset);
            artMethodSize = bAddr - aAddr;
            artMethodBias = aAddr - aMethods - artMethodSize;
        } catch (Throwable t) {
            XposedBridge.log("failed to initialize helper");
            throw new ExceptionInInitializerError(t);
        }
    }

    @Nullable
    public static Constructor<?> getClInit(Class<?> clazz) {
        if (clazz.isPrimitive() || clazz.isArray()) return null;
        MethodHandle mh;
        try {
            Method mA = Helper.NeverCall.class.getDeclaredMethod("a");
            mA.setAccessible(true);
            mh = MethodHandles.lookup().unreflect(mA);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
        long methods = unsafe.getLong(clazz, methodsOffset);
        if (methods == 0) return null;
        int numMethods = unsafe.getInt(methods);
        for (int i = 0; i < numMethods; i++) {
            long method = methods + i * artMethodSize + artMethodBias;
            unsafe.putLong(mh, artOffset, method);
            unsafe.putObject(mh, infoOffset, null);
            try {
                MethodHandles.lookup().revealDirect(mh);
            } catch (Throwable ignored) {
            }
            MethodHandleInfo info = (MethodHandleInfo) unsafe.getObject(mh, infoOffset);
            Executable member = (Executable) unsafe.getObject(info, memberOffset);
            if (member instanceof Constructor && (member.getModifiers() & Modifier.STATIC) != 0) return (Constructor<?>) member;
        }
        return null;
    }
}
