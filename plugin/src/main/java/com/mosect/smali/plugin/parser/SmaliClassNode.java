package com.mosect.smali.plugin.parser;

public class SmaliClassNode extends SmaliBlockNode {

    private final String classType;

    public SmaliClassNode(String classType) {
        this.classType = classType;
    }

    public String getClassType() {
        return classType;
    }

    @Override
    public String getId() {
        return getClassName();
    }

    @Override
    public String getType() {
        return "class";
    }
}
