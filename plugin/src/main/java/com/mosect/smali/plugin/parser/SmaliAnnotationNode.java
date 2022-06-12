package com.mosect.smali.plugin.parser;

public class SmaliAnnotationNode extends SmaliBlockNode {

    @Override
    public SmaliAnnotationNode createEmpty() {
        return new SmaliAnnotationNode();
    }

    @Override
    public String getType() {
        return "annotation";
    }
}
