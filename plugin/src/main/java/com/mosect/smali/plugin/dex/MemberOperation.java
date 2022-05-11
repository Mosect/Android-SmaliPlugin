package com.mosect.smali.plugin.dex;

import com.mosect.smali.plugin.util.RegexMatcher;
import com.mosect.smali.plugin.util.TextUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

public class MemberOperation {

    private final Map<Key, HashSet<RegexMatcher>> data = new HashMap<>();

    public boolean addLine(String line) {
        String[] fs = line.split("\\s+");
        if (fs.length != 2) {
            return false;
        } else {
            String type;
            String regex;
            if (fs[1].matches("^[^()]+\\([^()]*\\)$")) {
                // 方法
                type = "method";
                regex = TextUtils.convertToRegex(fs[1]);
            } else if (fs[1].matches("^[^()]+$")) {
                // 字段
                type = "field";
                regex = TextUtils.convertToRegex(fs[1]);
            } else {
                return false;
            }

            String action;
            switch (fs[0]) {
                case "delete":
                case "d":
                    action = "d";
                    break;
                case "ignore":
                case "i":
                    action = "i";
                    break;
                case "replace":
                case "r":
                    action = "r";
                    break;
                case "original":
                case "o":
                    action = "o";
                    break;
                default:
                    return false;
            }

            Key key = new Key(type, action);
            HashSet<RegexMatcher> regexList = data.computeIfAbsent(key, k -> new HashSet<>());
            regexList.add(new RegexMatcher(regex));
            return true;
        }
    }

    public boolean matchDelete(String type, String id) {
        return match(type, "d", id);
    }

    public boolean matchIgnore(String type, String id) {
        return match(type, "i", id);
    }

    public boolean matchReplace(String type, String id) {
        return match(type, "r", id);
    }

    public boolean matchOriginal(String type, String id) {
        return match(type, "o", id);
    }

    private boolean match(String type, String action, String id) {
        Key key = new Key(type, action);
        HashSet<RegexMatcher> regexList = data.get(key);
        if (null != regexList) {
            for (RegexMatcher regex : regexList) {
                if (regex.matches(id)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static class Key {
        private final String type;
        private final String action;

        public Key(String type, String action) {
            this.type = type;
            this.action = action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return type.equals(key.type) && action.equals(key.action);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, action);
        }

        @Override
        public String toString() {
            return "Key{" +
                    "type='" + type + '\'' +
                    ", action='" + action + '\'' +
                    '}';
        }
    }

}
