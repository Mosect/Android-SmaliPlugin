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

    public abstract SmaliNode copy();

    public abstract String getType();

    public boolean match(SmaliNodeMatcher matcher) {
        for (int i = 0; i < getChildCount(); i++) {
            matcher.reset();
            SmaliNode child = getChildren().get(i);
            if (isIgnoreNode(child)) continue;
            for (int j = i; j < getChildCount(); j++) {
                SmaliNode cur = getChildren().get(j);
                if (isIgnoreNode(cur)) continue;
                int matchValue = matcher.match(this, cur, j);
                if (matchValue == 0) break;
                if (matchValue == 2) {
                    // 匹配成功
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean isIgnoreNode(SmaliNode node) {
        if ("token".equals(node.getType())) {
            SmaliToken token = (SmaliToken) node;
            return "comment".equals(token.getTokenType()) || "whitespace".equals(token.getTokenType());
        }
        return false;
    }

    @Override
    public String toString() {
        return "SmaliNode{type=" + getType() + "}";
    }
}
