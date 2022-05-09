package com.mosect.smali.plugin.dex;

public class SmaliException extends Exception {

    private final String type;
    private final int lineIndex;
    private final int lineOffset;

    public SmaliException(String message) {
        this(message, null, 0, 0);
    }

    public SmaliException(String message, String type, int lineIndex, int lineOffset) {
        super(message);
        this.type = type;
        this.lineIndex = lineIndex;
        this.lineOffset = lineOffset;
    }

    public String getType() {
        return type;
    }

    public int getLineIndex() {
        return lineIndex;
    }

    public int getLineOffset() {
        return lineOffset;
    }
}
