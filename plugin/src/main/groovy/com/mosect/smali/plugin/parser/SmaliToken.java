package com.mosect.smali.plugin.parser;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class SmaliToken extends SmaliNode {

    private final String text;
    private final String tokenType;

    public SmaliToken(String text, String tokenType) {
        this.text = text;
        this.tokenType = tokenType;
    }

    public String getText() {
        return text;
    }

    public String getTokenType() {
        return tokenType;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public int length() {
        return text.length();
    }

    @Override
    public List<SmaliNode> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public void setChildren(List<SmaliNode> children) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void append(Appendable appendable) throws IOException {
        appendable.append(text);
    }

    @Override
    public String getType() {
        return "token";
    }
}
