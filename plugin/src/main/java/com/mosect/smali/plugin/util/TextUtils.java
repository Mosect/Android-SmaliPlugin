package com.mosect.smali.plugin.util;

public final class TextUtils {

    private TextUtils() {
    }

    public static boolean isEmpty(CharSequence cs) {
        return null == cs || cs.length() <= 0;
    }

    public static String convertToRegex(String text) {
        StringBuilder builder = new StringBuilder((int) (text.length() * 1.618f));
        builder.append('^');
        int offset = 0;
        while (offset < text.length()) {
            if (match(text, offset, "**")) {
                builder.append("\\S*");
                offset += 2;
            } else {
                char ch = text.charAt(offset);
                switch (ch) {
                    case '*':
                        builder.append("[^.]*");
                        break;
                    case '^':
                    case '.':
                    case '[':
                    case ']':
                    case '\\':
                    case '$':
                    case '(':
                    case ')':
                    case '{':
                    case '}':
                    case '?':
                    case '+':
                    case '|':
                        builder.append('\\').append(ch);
                        break;
                    default:
                        builder.append(ch);
                        break;
                }
                ++offset;
            }
        }
        builder.append('$');
        return builder.toString();
    }

    private static boolean match(String target, int offset, String str) {
        if (target.length() - offset >= str.length()) {
            for (int i = 0; i < str.length(); i++) {
                if (target.charAt(offset + i) != str.charAt(i)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
