package com.mosect.smali.plugin.util;

public final class TextUtils {

    private TextUtils() {
    }

    public static boolean isEmpty(CharSequence cs) {
        return null == cs || cs.length() <= 0;
    }
}
