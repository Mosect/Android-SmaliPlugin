package com.mosect.smali.plugin.dex;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DexHandler {

    private final Map<Integer, HashSet<File>> originalSourceDirMap = new HashMap<>();
    private final HashSet<File> javaDexFiles = new HashSet<>();
    private File tempDir;
    private int apiLevel = 15;
    private final List<File> operationFiles = new ArrayList<>();
    private final List<File> positionFiles = new ArrayList<>();

    public void setApiLevel(int apiLevel) {
        this.apiLevel = apiLevel;
    }

    public void addOriginalSourceDir(int dexIndex, File dir) {
        HashSet<File> files = originalSourceDirMap.computeIfAbsent(dexIndex, k -> new HashSet<>());
        files.add(dir);
    }

    public Set<Integer> allOriginalSourceClasses() {
        return originalSourceDirMap.keySet();
    }

    public void addJavaDexFile(File dexFile) {
        javaDexFiles.add(dexFile);
    }

    public void setTempDir(File tempDir) {
        this.tempDir = tempDir;
    }

    public void addOperationFile(File file) {
        operationFiles.add(file);
    }

    public void addPositionFile(File file) {
        positionFiles.add(file);
    }

    /**
     * 执行dex处理任务
     *
     * @return 最终dex输出的目录
     * @throws IOException    读写异常
     * @throws SmaliException smali异常
     */
    public File run() throws IOException, SmaliException {
        if (null == tempDir) {
            throw new IllegalArgumentException("tempDir not set");
        }

        File javaTempDir = new File(tempDir, "smali");
        javaTempDir.mkdirs();

        List<File> javaClassesDirs = null;
        if (javaDexFiles.size() > 0) {
            System.out.println("DexHandler:dexDecode");
            DexDecoder dexDecoder = new DexDecoder();
            dexDecoder.setApiLevel(apiLevel);
            for (File file : javaDexFiles) {
                dexDecoder.addDexFile(file);
            }
            javaClassesDirs = dexDecoder.decode(javaTempDir);
        }

        // 创建java的classesSource
        ClassesSource javaClassesSource = new ClassesSource();
        if (null != javaClassesDirs) {
            for (File dir : javaClassesDirs) {
                javaClassesSource.addDir(dir);
            }
        }

        // 构建dexMaker
        List<DexMaker> dexMakerList = new ArrayList<>();
        for (Map.Entry<Integer, HashSet<File>> entry : originalSourceDirMap.entrySet()) {
            int dexIndex = entry.getKey();
            String name = dexIndex == 1 ? "classes" : "classes" + dexIndex;
            DexMaker dexMaker = new DexMaker(dexIndex);
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
        if (null == originalSourceDirMap.get(1)) {
            // 不存在主dex构建器
            DexMaker dexMaker = new DexMaker(1);
            dexMaker.setApiLevel(apiLevel);
            dexMakerList.add(0, dexMaker);
        }

        // 合并smali
        System.out.println("DexHandler:mergeSmali");
        File mergedTempDir = new File(tempDir, "merged");
        SmaliMerger merger = new SmaliMerger();
        merger.setJavaSource(javaClassesSource);
        merger.setTempDir(mergedTempDir);
        for (DexMaker dexMaker : dexMakerList) {
            merger.addDexMaker(dexMaker);
        }
        for (File file : operationFiles) {
            if (file.isFile()) {
                System.out.printf("DexHandler:addOperationFile {%s}%n", file.getAbsolutePath());
                merger.addOperationFile(file);
            }
        }
        for (File file : positionFiles) {
            if (file.isFile()) {
                System.out.printf("DexHandler:addPositionFile {%s}%n", file.getAbsolutePath());
                merger.addPositionFile(file);
            }
        }
        dexMakerList = merger.merge();

        // 构建dex
        File classesDir = new File(tempDir, "classes");
        for (DexMaker dexMaker : dexMakerList) {
            File file = new File(classesDir, dexMaker.getName() + ".dex");
            System.out.printf("DexHandler:makeDex[%s] %s%n", dexMaker.getName(), file.getAbsolutePath());
            dexMaker.makeDex(file);
        }
        return classesDir;
    }
}
