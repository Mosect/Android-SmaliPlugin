package com.mosect.smali.plugin.dex;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class DexHandler {

    private final Map<Integer, HashSet<File>> originalSourceDirMap = new HashMap<>();
    private final HashSet<File> javaDexFiles = new HashSet<>();
    private File tempDir;
    private int apiLevel = 15;

    public void setApiLevel(int apiLevel) {
        this.apiLevel = apiLevel;
    }

    public void addOriginalSourceDir(int dexIndex, File dir) {
        HashSet<File> files = originalSourceDirMap.computeIfAbsent(dexIndex, k -> new HashSet<>());
        files.add(dir);
    }

    public void addJavaDexFile(File dexFile) {
        javaDexFiles.add(dexFile);
    }

    public void setTempDir(File tempDir) {
        this.tempDir = tempDir;
    }

    public void run() throws IOException, SmaliException {
        if (originalSourceDirMap.isEmpty()) {
            throw new IllegalArgumentException("originalSourceDir not set");
        }
        if (javaDexFiles.isEmpty()) {
            throw new IllegalArgumentException("javaDexFile not set");
        }
        if (null == tempDir) {
            throw new IllegalArgumentException("tempDir not set");
        }

        File javaTempDir = new File(tempDir, "smali");
        javaTempDir.mkdirs();

        System.out.println("DexHandler:dexDecode");
        DexDecoder dexDecoder = new DexDecoder();
        dexDecoder.setApiLevel(apiLevel);
        for (File file : javaDexFiles) {
            dexDecoder.addDexFile(file);
        }
        List<File> javaClassesDirs = dexDecoder.decode(javaTempDir);

        // 创建java的classesSource
        ClassesSource javaClassesSource = new ClassesSource();
        for (File dir : javaClassesDirs) {
            javaClassesSource.addDir(dir);
        }

        // 构建dexMaker
        List<DexMaker> dexMakerList = new ArrayList<>();
        for (Map.Entry<Integer, HashSet<File>> entry : originalSourceDirMap.entrySet()) {
            int dexIndex = entry.getKey();
            String name = dexIndex == 1 ? "classes" : "classes" + dexIndex;
            DexMaker dexMaker = new DexMaker(name);
            ClassesSource classesSource = new ClassesSource();
            for (File dir : entry.getValue()) {
                classesSource.addDir(dir);
            }
            List<ClassesSource.SmaliFileInfo> fileInfoList = classesSource.listAll();
            for (ClassesSource.SmaliFileInfo fileInfo : fileInfoList) {
                dexMaker.addSmaliFile(fileInfo.getClassName(), fileInfo.getFile());
            }
            dexMaker.setApiLevel(apiLevel);
            System.out.println("DexHandler:dexMaker " + dexMaker.getName());
            dexMakerList.add(dexMaker);
        }

        // 合并smali
        System.out.println("DexHandler:mergeSmali");
        File mergedTempDir = new File(tempDir, "merged");
        ClassesSourceMerger merger = new ClassesSourceMerger();
        merger.setJavaSource(javaClassesSource);
        merger.setTempDir(mergedTempDir);
        for (DexMaker dexMaker : dexMakerList) {
            merger.addDexMaker(dexMaker);
        }
        merger.merge();

        // 构建dex
        File classesDir = new File(tempDir, "classes");
        for (DexMaker dexMaker : dexMakerList) {
            File file = new File(classesDir, dexMaker.getName() + ".dex");
            System.out.printf("DexHandler:makeDex[%s] %s%n", dexMaker.getName(), file.getAbsolutePath());
            dexMaker.makeDex(file);
        }
    }
}
