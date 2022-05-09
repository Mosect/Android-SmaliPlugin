package com.mosect.smali.plugin.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class SmaliNode {

    private List<SmaliNode> children;

    public int getChildCount() {
        if (null != children) return children.size();
        return 0;
    }

    public List<SmaliNode> getChildren() {
        if (null == children) children = new ArrayList<>();
        return children;
    }

    public void setChildren(List<SmaliNode> children) {
        this.children = children;
    }

    public void append(Appendable appendable) throws IOException {
        if (getChildCount() > 0) {
            for (SmaliNode child : getChildren()) {
                child.append(appendable);
            }
        }
    }

    public int length() {
        int len = 0;
        if (getChildCount() > 0) {
            for (SmaliNode child : getChildren()) {
                len += child.length();
            }
        }
        return len;
    }

    public abstract String getType();
}
