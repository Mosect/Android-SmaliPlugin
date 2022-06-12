package com.mosect.smali.plugin.dex;

import com.mosect.smali.plugin.parser.SmaliAnnotationNode;
import com.mosect.smali.plugin.parser.SmaliBlockNode;
import com.mosect.smali.plugin.parser.SmaliNode;
import com.mosect.smali.plugin.parser.SmaliToken;
import com.mosect.smali.plugin.util.IOUtils;
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
import java.util.Set;

/**
 * 将dex和smali源码合并
 */
public class SmaliMerger {

    private final static String COPY = "com.mosect.smali.annotation.Copy";
    private final static String DELETE = "com.mosect.smali.annotation.Delete";
    private final static String IGNORE = "com.mosect.smali.annotation.Ignore";
    private final static String MERGE = "com.mosect.smali.annotation.Merge";
    private final static String ORIGINAL = "com.mosect.smali.annotation.Original";
    private final static String REPLACE = "com.mosect.smali.annotation.Replace";

    private final SimpleSmaliParser parser = new SimpleSmaliParser();
    private ClassesSource javaSource;
    private File tempDir;
    private final Map<Integer, DexMaker> dexMakerMap = new HashMap<>();
    private final ClassOperation classOperation = new ClassOperation();
    private final ClassPosition classPosition = new ClassPosition();

    public void setJavaSource(ClassesSource javaSource) {
        this.javaSource = javaSource;
    }

    public void setTempDir(File tempDir) {
        this.tempDir = tempDir;
    }

    public void addDexMaker(DexMaker dexMaker) {
        DexMaker old = dexMakerMap.put(dexMaker.getIndex(), dexMaker);
        if (null != old) {
            throw new IllegalArgumentException("Exist DexMaker: " + dexMaker.getName());
        }
    }

    public void addOperationFile(File file) throws IOException {
        classOperation.load(file);
    }

    public void addPositionFile(File file) throws IOException {
        classPosition.load(file);
    }

    public List<DexMaker> merge() throws IOException, SmaliException {
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
        dexMakerList.sort(Comparator.comparing(DexMaker::getIndex));

        for (Map.Entry<String, File> entry : javaSmaliFiles.entrySet()) {
            String className = entry.getKey();
            if (className.startsWith("com.mosect.smali.annotation.")) {
                continue;
            }

            SmaliBlockNode javaBlockNode = parser.parse(entry.getValue());
            Map<String, SmaliAnnotationNode> javaAnnotations = getAnnotations(javaBlockNode);
            if (classOperation.matchDelete(className) || javaAnnotations.containsKey(DELETE)) {
                // 删除
                for (DexMaker dexMaker : dexMakerList) {
                    dexMaker.removeSmaliFile(className);
                }
            } else if (classOperation.matchOriginal(className) || javaAnnotations.containsKey(ORIGINAL)) {
                // 使用原本smali
                DexMaker dexMaker = findDexMaker(dexMakerList, className);
                if (null == dexMaker) {
                    throw new SmaliException("MissingClass: " + className);
                }
            } else if (classOperation.matchReplace(className) || javaAnnotations.containsKey(REPLACE)) {
                // 替换
                File file = writeSmaliFile(tempDir, className, javaBlockNode);
                DexMaker dexMaker = findDexMaker(dexMakerList, className);
                if (null == dexMaker) {
                    dexMaker = dexMakerList.get(0);
                }
                dexMaker.addSmaliFile(className, file);
            } else {
                MemberOperation memberOperation = classOperation.matchMerge(className);
                if (null != memberOperation || javaAnnotations.containsKey(MERGE)) {
                    // 合并
                    DexMaker dexMaker = findDexMaker(dexMakerList, className);
                    if (null == dexMaker) {
                        throw new SmaliException("MissingClass: " + className);
                    }
                    File originalFile = dexMaker.getSmaliFile(className);
                    SmaliBlockNode originalBlockNode = parser.parse(originalFile);
                    // 合并字段
                    mergeSmali(memberOperation, javaBlockNode, originalBlockNode, "field");
                    // 合并方法
                    mergeSmali(memberOperation, javaBlockNode, originalBlockNode, "method");
                    File file = writeSmaliFile(tempDir, className, originalBlockNode);
                    dexMaker.addSmaliFile(className, file);
                } else if (!classOperation.matchIgnore(className) && !javaAnnotations.containsKey(IGNORE)) {
                    // 非忽略类
                    File file = writeSmaliFile(tempDir, className, javaBlockNode);
                    DexMaker dexMaker = dexMakerList.get(0);
                    dexMaker.addSmaliFile(className, file);
                }
            }
        }

        // change class dex index
        DexMaker newDexMaker = null;
        DexMaker endDexMaker = dexMakerList.get(dexMakerList.size() - 1);
        int newDexMakerIndex = endDexMaker.getIndex() + 1;
        for (DexMaker dexMaker : dexMakerList) {
            Set<Map.Entry<String, File>> classSet = new HashSet<>(dexMaker.allClasses());
            for (Map.Entry<String, File> classItem : classSet) {
                String className = classItem.getKey();
                File smaliFile = classItem.getValue();
                ClassPosition.PositionItem pi = classPosition.find(dexMaker.getIndex(), className);
                if (null != pi) {
                    if (pi.getToIndex() == ClassPosition.INDEX_NEW) {
                        // Move class to new dex
                        if (null == newDexMaker) {
                            newDexMaker = new DexMaker(newDexMakerIndex);
                            dexMakerMap.put(newDexMakerIndex, newDexMaker);
                        }
                        newDexMaker.addSmaliFile(className, smaliFile);
                        dexMaker.removeSmaliFile(className);
                    } else if (pi.getToIndex() == ClassPosition.INDEX_END) {
                        // Move class to end dex
                        if (dexMaker.getIndex() != endDexMaker.getIndex()) {
                            endDexMaker.addSmaliFile(className, smaliFile);
                            dexMaker.removeSmaliFile(className);
                        }
                    } else if (pi.getToIndex() != dexMaker.getIndex()) {
                        // Move class to particular dex
                        DexMaker toDexMaker = dexMakerMap.get(pi.getToIndex());
                        if (null == toDexMaker) {
                            toDexMaker = new DexMaker(pi.getToIndex());
                            dexMakerMap.put(pi.getToIndex(), toDexMaker);
                        }
                        toDexMaker.addSmaliFile(className, smaliFile);
                        dexMaker.removeSmaliFile(className);
                    }
                }
            }
        }

        dexMakerList = new ArrayList<>(dexMakerMap.values());
        dexMakerList.sort(Comparator.comparing(DexMaker::getIndex));
        return dexMakerList;
    }

    private void mergeSmali(
            MemberOperation memberOperation,
            SmaliBlockNode javaBlockNode,
            SmaliBlockNode originalBlockNode,
            String type) throws SmaliException {
        if (javaBlockNode.getChildCount() > 0) {
            List<SmaliBlockNode> deleteNodes = new ArrayList<>();
            List<SmaliBlockNode> addNodes = new ArrayList<>();
            for (SmaliNode childNode : javaBlockNode.getChildren()) {
                if (Objects.equals(childNode.getType(), type)) {
                    SmaliBlockNode childBlockNode = (SmaliBlockNode) childNode;
                    String id = childBlockNode.getId();
                    if (TextUtils.isEmpty(id)) {
                        continue;
                    }
                    Map<String, SmaliAnnotationNode> annotations = getAnnotations(childBlockNode);
                    if ("method".equals(type) && annotations.containsKey(COPY)) {
                        // 执行复制操作
                        SmaliAnnotationNode copyAnnotation = annotations.get(COPY);
                        CopyValueMatcher matcher = new CopyValueMatcher();
                        if (copyAnnotation.match(matcher)) {
                            String dstName = matcher.getValue();
                            SmaliBlockNode blockNode = findBlockNode(originalBlockNode, type, id);
                            SmaliBlockNode copy = blockNode.copy();
                            MethodNameMatcher methodNameMatcher = new MethodNameMatcher();
                            if (copy.match(methodNameMatcher)) {
                                SmaliToken nameToken = new SmaliToken(dstName, "word");
                                copy.getChildren().set(methodNameMatcher.getNameNodeIndex(), nameToken);
                                addNodes.add(copy);
                            }
                        }
                    }
                    if ((null != memberOperation && memberOperation.matchDelete(type, id)) || annotations.containsKey(DELETE)) {
                        SmaliBlockNode blockNode = findBlockNode(originalBlockNode, type, id);
                        deleteNodes.add(blockNode);
                    } else if ((null != memberOperation && memberOperation.matchOriginal(type, id)) || annotations.containsKey(ORIGINAL)) {
                        findBlockNode(originalBlockNode, type, id);
                    } else if ((null != memberOperation && memberOperation.matchReplace(type, id)) || annotations.containsKey(REPLACE)) {
                        SmaliBlockNode blockNode = findBlockNode(originalBlockNode, type, id);
                        int index = originalBlockNode.getChildren().indexOf(blockNode);
                        originalBlockNode.getChildren().set(index, childBlockNode);
                    } else if ((null != memberOperation && !memberOperation.matchIgnore(type, id)) && !annotations.containsKey(IGNORE)) {
                        // 非忽略，直接添加
                        originalBlockNode.getChildren().add(childBlockNode);
                        originalBlockNode.getChildren().add(SmaliToken.line());
                    }
                }
            }
            if (!deleteNodes.isEmpty()) {
                originalBlockNode.getChildren().removeAll(deleteNodes);
            }
            if (!addNodes.isEmpty()) {
                originalBlockNode.getChildren().addAll(addNodes);
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
        IOUtils.initParent(file);
        StringBuilder builder = new StringBuilder();
        blockNode.append(builder);
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter bw = new BufferedWriter(osw)) {
            bw.write(builder.toString());
        }
        return file;
    }

    protected Map<String, SmaliAnnotationNode> getAnnotations(SmaliBlockNode blockNode) {
        Map<String, SmaliAnnotationNode> result = new HashMap<>();
        if (blockNode.getChildCount() > 0) {
            for (SmaliNode node : blockNode.getChildren()) {
                if ("annotation".equals(node.getType())) {
                    SmaliAnnotationNode annotationNode = (SmaliAnnotationNode) node;
                    String className = annotationNode.getClassName();
                    if (!TextUtils.isEmpty(className) && className.startsWith("com.mosect.smali.annotation.")) {
                        result.put(className, annotationNode);
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
                System.err.printf("ReplaceClasses: %s >>> %s%n", fileInfo.getFile(), old);
            }
        }
        return map;
    }
}
