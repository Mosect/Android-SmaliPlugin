package com.mosect.smali.plugin.util;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexMatcher {

    private final String regex;
    private Pattern pattern = null;
    private boolean complied;

    public RegexMatcher(String regex) {
        this.regex = regex;
    }

    public boolean matches(String text) {
        if (!complied) {
            try {
                pattern = Pattern.compile(regex);
            } catch (Exception ignored) {
            }
            complied = true;
        }
        if (null != pattern) {
            Matcher matcher = pattern.matcher(text);
            return matcher.matches();
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegexMatcher that = (RegexMatcher) o;
        return regex.equals(that.regex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(regex);
    }

    @Override
    public String toString() {
        return "RegexMatcher{" +
                "regex='" + regex + '\'' +
                '}';
    }
}
