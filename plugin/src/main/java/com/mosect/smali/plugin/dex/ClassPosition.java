package com.mosect.smali.plugin.dex;

import com.mosect.smali.plugin.util.RegexMatcher;
import com.mosect.smali.plugin.util.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;

/**
 * Class's dex index handle
 */
public class ClassPosition {

    public final static int INDEX_NEW = -1;
    public final static int INDEX_END = -2;

    private final HashSet<PositionItem> data = new HashSet<>();

    public void load(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }

                String[] fs = line.split("\\s+");
                if (fs.length == 2) {
                    int targetIndex;
                    if ("new".equals(fs[0])) {
                        targetIndex = -1;
                    } else if ("end".equals(fs[0])) {
                        targetIndex = -2;
                    } else if ("main".equals(fs[0])) {
                        targetIndex = 1;
                    } else if (fs[0].matches("^[0-9]+$")) {
                        targetIndex = Integer.parseInt(fs[0]);
                        if (targetIndex <= 0 || targetIndex > 99) {
                            invalidLine(line);
                            continue;
                        }
                    } else {
                        invalidLine(line);
                        continue;
                    }

                    int classIndex;
                    String classPath;
                    if (fs[1].matches("^[0-9]+:\\S+$")) {
                        int si = fs[1].indexOf(":");
                        String indexStr = fs[1].substring(0, si);
                        classPath = fs[1].substring(si + 1);
                        classIndex = Integer.parseInt(indexStr);
                        if (TextUtils.isEmpty(classPath)) {
                            invalidLine(line);
                            continue;
                        }
                    } else {
                        classIndex = 0;
                        classPath = fs[1];
                    }

                    String regex = TextUtils.convertToRegex(classPath);
                    PositionItem positionItem = new PositionItem(regex, classIndex, targetIndex);
                    data.add(positionItem);
                } else {
                    invalidLine(line);
                }
            }
        }
    }

    private void invalidLine(String line) {
        System.err.printf("ClassPosition:invalidLine {%s}%n", line);
    }

    public PositionItem find(int dexIndex, String className) {
        for (PositionItem pi : data) {
            if ((pi.getFromIndex() == 0 || pi.getFromIndex() == dexIndex) && pi.matchClassName(className)) {
                return pi;
            }
        }
        return null;
    }

    public static class PositionItem {

        private final String regex;
        private final int fromIndex;
        private final int toIndex;
        private RegexMatcher regexMatcher;

        private PositionItem(String regex, int fromIndex, int toIndex) {
            this.regex = regex;
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
        }

        public int getFromIndex() {
            return fromIndex;
        }

        public int getToIndex() {
            return toIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PositionItem that = (PositionItem) o;
            return regex.equals(that.regex);
        }

        @Override
        public int hashCode() {
            return Objects.hash(regex);
        }

        @Override
        public String toString() {
            return "OperationItem{" +
                    "regex='" + regex + '\'' +
                    ", fromIndex=" + fromIndex +
                    ", toIndex=" + toIndex +
                    '}';
        }

        private boolean matchClassName(String className) {
            if (null == regexMatcher) {
                regexMatcher = new RegexMatcher(regex);
            }
            return regexMatcher.matches(className);
        }
    }
}
