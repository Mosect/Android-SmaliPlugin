package com.mosect.smali.plugin.parser;

import java.util.List;

public class SmaliFieldNode extends SmaliBlockNode {

    @Override
    public String getId() {
        if (getChildCount() > 0) {
            List<SmaliNode> nodes = getChildren();
            boolean start = false;
            for (int i = nodes.size() - 1; i >= 0; i--) {
                SmaliNode node = nodes.get(i);
                if ("token".equals(node.getType())) {
                    SmaliToken token = (SmaliToken) node;
                    if (start) {
                        if ("word".equals(token.getTokenType())) {
                            return token.getText();
                        }
                    } else {
                        if ("symbol".equals(token.getTokenType()) && ":".equals(token.getText())) {
                            start = true;
                        }
                    }
                }
            }
        }

        return null;
    }

    @Override
    public String getType() {
        return "field";
    }
}
