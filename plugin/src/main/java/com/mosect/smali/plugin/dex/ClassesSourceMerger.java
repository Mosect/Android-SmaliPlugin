package com.mosect.smali.plugin.dex;

import com.mosect.smali.plugin.parser.SmaliAnnotationNode;
import com.mosect.smali.plugin.parser.SmaliBlockNode;
import com.mosect.smali.plugin.parser.SmaliNode;
import com.mosect.smali.plugin.parser.SmaliToken;
import com.mosect.smali.plugin.util.TextUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 合并java和dex smali源码
 */
public class ClassesSourceMerger {

    private final static String DELETE = "com.mosect.smali.annotation.Delete";
    private final static String IGNORE = "com.mosect.smali.annotation.Ignore";
    private final static String MERGE = "com.mosect.smali.annotation.Merge";
    private final static String ORIGINAL = "com.mosect.smali.annotation.Original";
    private final static String REPLACE = "com.mosect.smali.annotation.Replace";

    private final SimpleSmaliParser parser = new SimpleSmaliParser();
    private ClassesSource javaSource;
    private File tempDir;
    private final Map<String, DexMaker> dexMakerMap = new HashMap<>();

    public void setJavaSource(ClassesSource javaSource) {
        this.javaSource = javaSource;
    }

    public void setTempDir(File tempDir) {
        this.tempDir = tempDir;
    }

    public void addDexMaker(DexMaker dexMaker) {
        DexMaker old = dexMakerMap.put(dexMaker.getName(), dexMaker);
        if (null != old) {
            throw new IllegalArgumentException("Exist DexMaker: " + dexMaker.getName());
        }
    }

    public void merge() throws IOException, SmaliException {
        if (null == javaSource) {
            throw new IllegalArgumentException("javaSource not set");
        }
        if (null == tempDir) {
            throw new IllegalArgumentException("tempDir not set");
        }
        if (dexMakerMap.isEmpty()) {
            throw new IllegalArgumentException("dexMaker not set");
        }

        Map<String, File> javaSmaliFiles = loadSmaliFiles(javaSource);
        List<DexMaker> dexMakerList = new ArrayList<>(dexMakerMap.values());
        dexMakerList.sort(Comparator.comparing(DexMaker::getName));

        for (Map.Entry<String, File> entry : javaSmaliFiles.entrySet()) {
            String className = entry.getKey();
            SmaliBlockNode javaBlockNode = parser.parse(entry.getValue());
            HashSet<String> javaAnnotations = getAnnotations(javaBlockNode);
            if (javaAnnotations.contains(DELETE)) {
                // 删除
                for (DexMaker dexMaker : dexMakerList) {
                    dexMaker.removeSmaliFile(className);
                }
            } else if (javaAnnotations.contains(MERGE)) {
                // 合并
                DexMaker dexMaker = findDexMaker(dexMakerList, className);
                if (null == dexMaker) {
                    throw new SmaliException("MissingClass: " + className);
                }
                File originalFile = dexMaker.getSmaliFile(className);
                SmaliBlockNode originalBlockNode = parser.parse(originalFile);
                // 合并字段
                mergeSmali(javaBlockNode, originalBlockNode, "field");
                // 合并方法
                mergeSmali(javaBlockNode, originalBlockNode, "method");
            } else if (javaAnnotations.contains(ORIGINAL)) {
                // 使用原本smali
                DexMaker dexMaker = findDexMaker(dexMakerList, className);
                if (null == dexMaker) {
                    throw new SmaliException("MissingClass: " + className);
                }
            } else if (javaAnnotations.contains(REPLACE)) {
                // 替换
                File file = writeSmaliFile(tempDir, className, javaBlockNode);
                DexMaker dexMaker = findDexMaker(dexMakerList, className);
                if (null == dexMaker) dexMaker = dexMakerList.get(0);
                dexMaker.addSmaliFile(className, file);
            } else if (!javaAnnotations.contains(IGNORE)) {
                // 非忽略类
                File file = writeSmaliFile(tempDir, className, javaBlockNode);
                DexMaker dexMaker = dexMakerList.get(0);
                dexMaker.addSmaliFile(className, file);
            }
        }
    }

    private void mergeSmali(SmaliBlockNode javaBlockNode, SmaliBlockNode originalBlockNode, String type) throws SmaliException {
        if (javaBlockNode.getChildCount() > 0) {
            List<SmaliBlockNode> deleteNodes = new ArrayList<>();
            for (SmaliNode childNode : javaBlockNode.getChildren()) {
                if (Objects.equals(childNode.getType(), type)) {
                    SmaliBlockNode childBlockNode = (SmaliBlockNode) childNode;
                    String id = childBlockNode.getId();
                    if (TextUtils.isEmpty(id)) {
                        continue;
                    }
                    HashSet<String> annotations = getAnnotations(childBlockNode);
                    if (annotations.contains(DELETE)) {
                        SmaliBlockNode blockNode = findBlockNode(originalBlockNode, type, id);
                        deleteNodes.add(blockNode);
                    } else if (annotations.contains(ORIGINAL)) {
                        findBlockNode(originalBlockNode, type, id);
                    } else if (annotations.contains(REPLACE)) {
                        SmaliBlockNode blockNode = findBlockNode(originalBlockNode, type, id);
                        int index = originalBlockNode.getChildren().indexOf(blockNode);
                        originalBlockNode.getChildren().set(index, childBlockNode);
                    } else if (!annotations.contains(IGNORE)) {
                        // 非忽略，直接添加
                        originalBlockNode.getChildren().add(childBlockNode);
                        originalBlockNode.getChildren().add(SmaliToken.line());
                    }
                }
            }
            if (!deleteNodes.isEmpty()) {
                originalBlockNode.getChildren().removeAll(deleteNodes);
            }
        }
    }

    private SmaliBlockNode findBlockNode(SmaliBlockNode target, String type, String id) throws SmaliException {
        if (target.getChildCount() > 0) {
            for (SmaliNode childNode : target.getChildren()) {
                if (Objects.equals(type, childNode.getType())) {
                    SmaliBlockNode childBlockNode = (SmaliBlockNode) childNode;
                    if (Objects.equals(id, childBlockNode.getId())) {
                        return childBlockNode;
                    }
                }
            }
        }
        throw new SmaliException("InvalidSmali: Missing " + type + ":" + id);
    }

    private DexMaker findDexMaker(List<DexMaker> dexMakerList, String className) {
        for (DexMaker dexMaker : dexMakerList) {
            File file = dexMaker.getSmaliFile(className);
            if (null != file) {
                return dexMaker;
            }
        }
        return null;
    }

    private File writeSmaliFile(File dir, String className, SmaliBlockNode blockNode) throws IOException {
        removeAnnotations(blockNode);
        File file = ClassesSource.getSmaliFile(dir, className);
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Can't create directory: " + parent.getAbsolutePath());
        }
        StringBuilder builder = new StringBuilder();
        blockNode.append(builder);
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter bw = new BufferedWriter(osw)) {
            bw.write(builder.toString());
        }
        return file;
    }

    protected HashSet<String> getAnnotations(SmaliBlockNode blockNode) {
        HashSet<String> result = new HashSet<>();
        if (blockNode.getChildCount() > 0) {
            for (SmaliNode node : blockNode.getChildren()) {
                if ("annotation".equals(node.getType())) {
                    SmaliAnnotationNode annotationNode = (SmaliAnnotationNode) node;
                    String className = annotationNode.getClassName();
                    if (!TextUtils.isEmpty(className) && className.startsWith("com.mosect.smali.annotation.")) {
                        result.add(className);
                    }
                }
            }
        }
        return result;
    }

    protected void removeAnnotations(SmaliBlockNode blockNode) {
        if (blockNode.getChildCount() > 0) {
            List<SmaliAnnotationNode> nodes = new ArrayList<>();
            for (SmaliNode node : blockNode.getChildren()) {
                switch (node.getType()) {
                    case "annotation":
                        SmaliAnnotationNode annotationNode = (SmaliAnnotationNode) node;
                        String className = annotationNode.getClassName();
                        if (!TextUtils.isEmpty(className) && className.startsWith("com.mosect.smali.annotation.")) {
                            nodes.add(annotationNode);
                        }
                        break;
                    case "field":
                    case "method":
                        removeAnnotations((SmaliBlockNode) node);
                        break;
                }
            }
            blockNode.getChildren().removeAll(nodes);
        }
    }

    private Map<String, File> loadSmaliFiles(ClassesSource source) throws IOException {
        List<ClassesSource.SmaliFileInfo> list = source.listAll();
        Map<String, File> map = new HashMap<>();
        for (ClassesSource.SmaliFileInfo fileInfo : list) {
            File old = map.put(fileInfo.getClassName(), fileInfo.getFile());
            if (null != old) {
                throw new IOException(String.format(
                        "MultipleClass[%s]: %s, %s",
                        fileInfo.getClassName(),
                        fileInfo.getFile().getAbsolutePath(),
                        old.getAbsolutePath()
                ));
            }
        }
        return map;
    }
}
