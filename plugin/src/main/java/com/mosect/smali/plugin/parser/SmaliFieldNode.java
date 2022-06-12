package com.mosect.smali.plugin.parser;

import java.util.List;

public class SmaliFieldNode extends SmaliBlockNode {

    @Override
    public SmaliFieldNode createEmpty() {
        return new SmaliFieldNode();
    }

    @Override
    public String getId() {
        if (getChildCount() > 0) {
            List<SmaliNode> nodes = getChildren();
            SmaliToken word = null;
            for (int i = 0; i < nodes.size(); i++) {
                SmaliNode node = nodes.get(i);
                if ("token".equals(node.getType())) {
                    SmaliToken token = (SmaliToken) node;
                    if ("word".equals(token.getTokenType())) {
                        word = token;
                    } else if ("symbol".equals(token.getTokenType()) && ":".equals(token.getText())) {
                        if (null != word) {
                            return word.getText();
                        }
                        break;
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
