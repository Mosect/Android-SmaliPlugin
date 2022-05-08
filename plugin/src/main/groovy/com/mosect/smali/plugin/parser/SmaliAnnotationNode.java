package com.mosect.smali.plugin.parser;

public class SmaliAnnotationNode extends SmaliBlockNode {

    @Override
    public String getType() {
        return "annotation";
    }
}
