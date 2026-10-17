package com.savequest.app.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class Money {
    private Money() {}

    public static String rp(long value) {
        NumberFormat f = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        f.setMaximumFractionDigits(0);
        return f.format(value);
    }

    public static long parse(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
