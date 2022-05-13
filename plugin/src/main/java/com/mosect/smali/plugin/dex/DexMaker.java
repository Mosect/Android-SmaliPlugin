package com.mosect.smali.plugin.dex;

import com.mosect.smali.plugin.parser.SmaliBlockNode;
import com.mosect.smali.plugin.util.IOUtils;
import com.mosect.smali.plugin.util.TextUtils;

import org.jf.smali.Smali;
import org.jf.smali.SmaliOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DexMaker {

    private final int index;
    private final String name;
    private final Map<String, File> smaliFileMap = new HashMap<>();
    private final SimpleSmaliParser smaliParser = new SimpleSmaliParser();
    private int apiLevel = 15;

    public DexMaker(int index) {
        if (index < 1 || index > 99) {
            throw new IllegalArgumentException("Invalid dex index: " + index);
        }
        this.index = index;
        if (index == 1) {
            name = "classes";
        } else {
            name = "classes" + index;
        }
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public void setApiLevel(int apiLevel) {
        this.apiLevel = apiLevel;
    }

    public void addSmaliFile(String className, File smaliFile) throws IOException, SmaliException {
        String safeClassName;
        if (TextUtils.isEmpty(className)) {
            SmaliBlockNode blockNode = smaliParser.parse(smaliFile);
            safeClassName = blockNode.getClassName();
        } else {
            safeClassName = className;
        }
//        System.out.printf("AddSmaliFile: [%s]{%s}%n", safeClassName, smaliFile.getAbsolutePath());
        smaliFileMap.put(safeClassName, smaliFile);
    }

    public File getSmaliFile(String className) {
        return smaliFileMap.get(className);
    }

    public File removeSmaliFile(String className) {
        return smaliFileMap.remove(className);
    }

    /**
     * Get all classes
     *
     * @return All classes, class name list
     */
    public Set<Map.Entry<String, File>> allClasses() {
        return smaliFileMap.entrySet();
    }

    public boolean makeDex(File outFile) throws IOException {
        IOUtils.initParent(outFile);
        SmaliOptions options = new SmaliOptions();
        options.apiLevel = apiLevel;
        options.jobs = 10;
        options.outputDexFile = outFile.getAbsolutePath();
        List<String> files = new ArrayList<>();
        for (File file : smaliFileMap.values()) {
            files.add(file.getAbsolutePath());
        }
        return Smali.assemble(options, files);
    }
}
