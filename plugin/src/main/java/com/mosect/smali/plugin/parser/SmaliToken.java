package com.mosect.smali.plugin.parser;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class SmaliToken extends SmaliNode {

    public static SmaliToken lineCr() {
        return createLineToken("\r");
    }

    public static SmaliToken lineCrlf() {
        return createLineToken("\r\n");
    }

    public static SmaliToken lineLf() {
        return createLineToken("\n");
    }

    public static SmaliToken line() {
        return createLineToken(System.lineSeparator());
    }

    private static SmaliToken createLineToken(String str) {
        switch (str) {
            case "\r":
                return new SmaliToken(str, "line.cr");
            case "\n":
                return new SmaliToken(str, "line.lf");
            case "\r\n":
                return new SmaliToken(str, "line.crlf");
            default:
                throw new IllegalArgumentException("Unsupported line text");
        }
    }

    private final String tokenType;
    private final String text;

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
    public SmaliNode copy() {
        return new SmaliToken(text, tokenType);
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

    @Override
    public String toString() {
        return "SmaliToken{" +
                "tokenType='" + tokenType + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
