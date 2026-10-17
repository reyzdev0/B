package com.savequest.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** PIN 6 digit, disimpan sebagai SHA-256(salt + pin), bukan teks asli. */
public class PinStore {
    public static final int LENGTH = 6;
    private final SharedPreferences prefs;

    public PinStore(Context ctx) {
        prefs = ctx.getSharedPreferences("security", Context.MODE_PRIVATE);
    }

    /** PIN versi lama (tanpa panjang tetap) dianggap belum ada, user diminta membuat PIN baru. */
    public boolean hasPin() {
        return prefs.contains("hash") && prefs.getInt("len", 0) == LENGTH;
    }

    public void setPin(String pin) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String s = Base64.getEncoder().encodeToString(salt);
        prefs.edit().putString("salt", s).putString("hash", hash(s, pin))
                .putInt("len", LENGTH).apply();
    }

    public boolean verify(String pin) {
        String salt = prefs.getString("salt", "");
        return hash(salt, pin).equals(prefs.getString("hash", ""));
    }

    private String hash(String salt, String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest((salt + pin).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
