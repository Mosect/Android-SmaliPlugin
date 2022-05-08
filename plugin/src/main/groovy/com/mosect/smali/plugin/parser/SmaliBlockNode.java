package com.mosect.smali.plugin.parser;

/**
 * 区块节点
 */
public class SmaliBlockNode extends SmaliNode {

    public String getBlockName() {
        if (getChildCount() > 0) {
            for (SmaliNode node : getChildren()) {
                if ("token".equals(node.getType())) {
                    SmaliToken token = (SmaliToken) node;
                    if ("block".equals(token.getTokenType())) {
                        return token.getText();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String getType() {
        return "block";
    }
}
