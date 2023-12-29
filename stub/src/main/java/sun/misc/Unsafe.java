package sun.misc;

public class Unsafe {
    public static Unsafe getUnsafe() {
        throw new RuntimeException("");
    }
    public native long getLong(Object obj, long offset);
    public native void putLong(Object obj, long offset, long newValue);
    public native Object getObject(Object obj, long offset);
    public native void putObject(Object obj, long offset, Object newValue);
    public native int getInt(long address);
    public long objectFieldOffset(java.lang.reflect.Field field) {
        throw new RuntimeException("Stub!");
    }

}
