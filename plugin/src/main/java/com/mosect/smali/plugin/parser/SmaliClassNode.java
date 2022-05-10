package com.mosect.smali.plugin.parser;

public class SmaliClassNode extends SmaliBlockNode {

    public String getClassType() {
        return null;
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
