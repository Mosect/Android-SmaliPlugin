package com.mosect.smali.plugin.dex;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class ClassesSource {

    private final HashSet<File> dirs = new HashSet<>();

    public void addDir(File dir) {
        dirs.add(dir);
    }

    /**
     * 获取smali文件
     *
     * @param dir       目录
     * @param className 类名
     * @return smali文件
     */
    public static File getSmaliFile(File dir, String className) {
        String path = getClassPath(className);
        return new File(dir, path);
    }

    /**
     * 查找smali文件
     *
     * @param className 类名
     * @return smali文件，找不到返回null
     */
    public File findSmaliFile(String className) {
        String path = getClassPath(className);
        for (File dir : dirs) {
            File file = new File(dir, path);
            if (file.exists() && file.isFile()) {
                return file;
            }
        }
        return null;
    }

    /**
     * 列出所有smali文件
     *
     * @return 所有smali文件
     */
    public List<SmaliFileInfo> listAll() {
        List<SmaliFileInfo> list = new ArrayList<>(128);
        for (File dir : dirs) {
            listByDir(dir, list, "");
        }
        return list;
    }

    private static String getClassPath(String className) {
        return className.replace('.', '/') + ".smali";
    }

    private void listByDir(File dir, List<SmaliFileInfo> out, String namePrefix) {
        File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile() && file.getName().endsWith(".smali");
            }
        });
        if (null != files) {
            for (File file : files) {
                String name = file.getName();
                String className = namePrefix + name.substring(0, name.length() - ".smali".length());
                out.add(new SmaliFileInfo(className, file));
            }
        }
        File[] dirs = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                String name = file.getName();
                if (file.isDirectory()) {
                    return !".".equals(name) && !"..".equals(name);
                }
                return false;
            }
        });
        if (null != dirs) {
            for (File childDir : dirs) {
                String nextNamePrefix = namePrefix + childDir.getName() + ".";
                listByDir(childDir, out, nextNamePrefix);
            }
        }
    }

    public static class SmaliFileInfo {

        private final String className;
        private final File file;

        public SmaliFileInfo(String className, File file) {
            this.className = className;
            this.file = file;
        }

        public String getClassName() {
            return className;
        }

        public File getFile() {
            return file;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SmaliFileInfo that = (SmaliFileInfo) o;
            return className.equals(that.className);
        }

        @Override
        public int hashCode() {
            return Objects.hash(className);
        }

        @Override
        public String toString() {
            return "SmaliFileInfo{" +
                    "className='" + className + '\'' +
                    ", file=" + file +
                    '}';
        }
    }
}
