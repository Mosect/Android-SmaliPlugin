package com.mosect.smali.plugin.parser;

public class SmaliEndNode extends SmaliNode {

    public String getBlockName() {
        if (getChildCount() > 0) {
            boolean start = false;
            for (SmaliNode node : getChildren()) {
                if ("token".equals(node.getType())) {
                    SmaliToken token = (SmaliToken) node;
                    if (start) {
                        if ("word".equals(token.getTokenType())) {
                            return token.getText();
                        }
                    } else {
                        if ("block".equals(token.getTokenType()) && ".end".equals(token.getText())) {
                            start = true;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public SmaliEndNode copy() {
        SmaliEndNode node = new SmaliEndNode();
        if (getChildCount() > 0) {
            for (SmaliNode child : getChildren()) {
                node.getChildren().add(child.copy());
            }
        }
        return node;
    }

    @Override
    public String getType() {
        return "end";
    }
}
