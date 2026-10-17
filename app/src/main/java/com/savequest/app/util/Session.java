package com.savequest.app.util;

/**
 * Status "sudah lewat lock screen". Berpindah layar di dalam app atau membuka picker/share
 * tidak boleh mengunci ulang, jadi onStop memeriksa jendela "suppress" ini.
 */
public final class Session {
    private Session() {}

    public static volatile boolean unlocked = false;
    private static volatile long suppressUntil = 0;
    private static volatile boolean external = false;

    /** Pindah antar Activity internal. */
    public static void markInternal() {
        suppressUntil = System.currentTimeMillis() + 2000;
    }

    /** Membuka picker file, galeri, share sheet, atau dialog izin. */
    public static void markExternal() {
        external = true;
        suppressUntil = System.currentTimeMillis() + 10 * 60_000L;
    }

    public static boolean isSuppressed() {
        return System.currentTimeMillis() < suppressUntil;
    }

    public static void clearExternal() {
        if (external) {
            external = false;
            suppressUntil = 0;
        }
    }
}
