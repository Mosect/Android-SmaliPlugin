package com.mosect.smali.plugin.dex;

import com.mosect.smali.plugin.util.RegexMatcher;
import com.mosect.smali.plugin.util.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

public class ClassOperation {

    private final Map<String, HashSet<ClassItem>> data = new HashMap<>();

    public void load(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            ClassItem classItem = null;
            _while:
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) continue;

                if (line.matches("^\\s+\\S+$")) {
                    if (null == classItem || null == classItem.memberOperation) {
                        invalidLine(line);
                    } else {
                        boolean ok = classItem.memberOperation.addLine(line);
                        if (!ok) {
                            invalidLine(line);
                        }
                    }
                } else {
                    String[] fs = line.split("\\s+");
                    if (fs.length != 2) {
                        invalidLine(line);
                    } else {
                        String regex = TextUtils.convertToRegex(fs[1]);
                        String type;
                        boolean member = false;
                        switch (fs[0]) {
                            case "delete":
                            case "d":
                                type = "d";
                                break;
                            case "ignore":
                            case "i":
                                type = "i";
                                break;
                            case "merge":
                            case "m":
                                member = true;
                                type = "m";
                                break;
                            case "original":
                            case "o":
                                type = "o";
                                break;
                            case "replace":
                            case "r":
                                type = "r";
                                break;
                            default:
                                invalidLine(line);
                                continue _while;
                        }
                        HashSet<ClassItem> classItems = data.computeIfAbsent(type, k -> new HashSet<>());
                        classItem = new ClassItem(regex, member);
                        classItems.add(classItem);
                    }
                }
            }
        }
    }

    private void invalidLine(String line) {
        System.err.printf("ClassOperation:invalidLine {%s}%n", line);
    }

    public boolean matchDelete(String className) {
        return null != find("d", className);
    }

    public boolean matchIgnore(String className) {
        return null != find("i", className);
    }

    public boolean matchReplace(String className) {
        return null != find("r", className);
    }

    public MemberOperation matchMerge(String className) {
        ClassItem classItem = find("m", className);
        if (null != classItem) {
            return classItem.memberOperation;
        }
        return null;
    }

    public boolean matchOriginal(String className) {
        return null != find("o", className);
    }

    private ClassItem find(String type, String className) {
        HashSet<ClassItem> classItems = data.get(type);
        if (null != classItems) {
            for (ClassItem classItem : classItems) {
                if (classItem.matchClassName(className)) {
                    return classItem;
                }
            }
        }
        return null;
    }

    private static class ClassItem {

        private final String regex;
        private RegexMatcher regexMatcher;
        private MemberOperation memberOperation;

        private ClassItem(String regex, boolean member) {
            this.regex = regex;
            if (member) {
                memberOperation = new MemberOperation();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClassItem classItem = (ClassItem) o;
            return regex.equals(classItem.regex);
        }

        @Override
        public int hashCode() {
            return Objects.hash(regex);
        }

        private boolean matchClassName(String className) {
            if (null == regexMatcher) regexMatcher = new RegexMatcher(regex);
            return regexMatcher.matches(className);
        }
    }
}
