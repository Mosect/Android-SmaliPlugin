package com.mosect.smali.plugin.parser;

import java.util.List;

public class SmaliParseResult<T> {

    private final T result;
    private final List<SmaliParseError> errors;

    public SmaliParseResult(T result, List<SmaliParseError> errors) {
        this.result = result;
        this.errors = errors;
    }

    public T getResult() {
        return result;
    }

    public List<SmaliParseError> getErrors() {
        return errors;
    }
}
