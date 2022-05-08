package com.mosect.smali.plugin.parser;

public class SmaliParseError {

    private final String type;
    private final String message;
    private final int charIndex;
    private int lineIndex;
    private int lineOffset;

    public SmaliParseError(String type, String message, int charIndex) {
        this.type = type;
        this.message = message;
        this.charIndex = charIndex;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public int getCharIndex() {
        return charIndex;
    }

    public int getLineIndex() {
        return lineIndex;
    }

    public void setLineIndex(int lineIndex) {
        this.lineIndex = lineIndex;
    }

    public void setLineOffset(int lineOffset) {
        this.lineOffset = lineOffset;
    }

    public int getLineOffset() {
        return lineOffset;
    }

    @Override
    public String toString() {
        return "SmaliParseError{" +
                "type='" + type + '\'' +
                ", message='" + message + '\'' +
                ", charIndex=" + charIndex +
                ", lineIndex=" + lineIndex +
                ", lineOffset=" + lineOffset +
                '}';
    }
}
