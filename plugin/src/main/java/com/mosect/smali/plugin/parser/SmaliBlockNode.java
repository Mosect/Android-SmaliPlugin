package com.mosect.smali.plugin.parser;

import java.util.Objects;

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

    @SuppressWarnings("unchecked")
    public <T extends SmaliNode> T findNode(String nodeType) {
        if (getChildCount() > 0) {
            for (SmaliNode node : getChildren()) {
                if (Objects.equals(node.getType(), nodeType)) {
                    return (T) node;
                }
            }
        }
        return null;
    }

    public String getClassName() {
        if (getChildCount() > 0) {
            int mode = 0;
            StringBuilder builder = new StringBuilder(64);
            _for:
            for (SmaliNode node : getChildren()) {
                if ("token".equals(node.getType())) {
                    SmaliToken token = (SmaliToken) node;
                    switch (mode) {
                        case 0:
                            if ("block".equals(token.getTokenType())) {
                                mode = 1;
                            }
                            break;
                        case 1:
                            if ("word".equals(token.getTokenType())) {
                                String text = token.getText();
                                boolean end = text.endsWith(";");
                                if (text.startsWith("L")) {
                                    mode = 2;
                                    if (end) {
                                        builder.append(text, 1, text.length() - 1);
                                    } else {
                                        builder.append(text, 1, text.length());
                                    }
                                }
                            }
                            break;
                        case 2:
                            if (!"whitespace".equals(token.getTokenType()) && !"comment".equals(token.getTokenType())) {
                                if ("word".equals(token.getTokenType())) {
                                    String text = token.getText();
                                    boolean end = text.endsWith(";");
                                    if (end) {
                                        builder.append(text, 0, text.length() - 1);
                                        break _for;
                                    } else {
                                        builder.append(text);
                                    }
                                } else {
                                    break _for;
                                }
                            }
                            break;
                    }
                }
            }
            for (int i = 0; i < builder.length(); i++) {
                char ch = builder.charAt(i);
                if (ch == '/') {
                    builder.setCharAt(i, '.');
                }
            }
            return builder.toString();
        }
        return null;
    }

    public String getId() {
        return null;
    }

    @Override
    public String getType() {
        return "block";
    }
}
