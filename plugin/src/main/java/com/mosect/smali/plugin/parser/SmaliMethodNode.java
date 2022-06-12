package com.mosect.smali.plugin.parser;

import java.io.IOException;
import java.util.List;

public class SmaliMethodNode extends SmaliBlockNode {

    @Override
    public SmaliMethodNode createEmpty() {
        return new SmaliMethodNode();
    }

    @Override
    public String getId() {
        if (getChildCount() > 0) {
            List<SmaliNode> nodes = getChildren();
            int end = -1;
            int start = -1;
            int wordIndex = -1;
            _for:
            for (int i = 0; i < nodes.size(); i++) {
                SmaliNode node = nodes.get(i);
                if ("token".equals(node.getType())) {
                    SmaliToken token = (SmaliToken) node;
                    if (start < 0) {
                        if ("symbol".equals(token.getTokenType()) && "(".equals(token.getText())) {
                            if (wordIndex < 0) return null;
                            start = wordIndex;
                        } else if ("word".equals(token.getTokenType())) {
                            wordIndex = i;
                        }
                    } else {
                        if ("symbol".equals(token.getTokenType()) && ")".equals(token.getText())) {
                            end = i + 1;
                            break;
                        } else {
                            switch (token.getTokenType()) {
                                case "comment":
                                case "whitespace":
                                case "word":
                                    break;
                                default:
                                    break _for;
                            }
                        }
                    }
                }
            }
            if (end >= 0) {
                try {
                    StringBuilder builder = new StringBuilder(128);
                    for (int i = start; i < end; i++) {
                        SmaliNode node = nodes.get(i);
                        if ("token".equals(node.getType())) {
                            SmaliToken token = (SmaliToken) node;
                            if ("word".equals(token.getTokenType()) || "symbol".equals(token.getTokenType())) {
                                token.append(builder);
                            }
                        }
                    }
                    return builder.toString();
                } catch (IOException ignored) {
                }
            }
        }

        return null;
    }

    @Override
    public String getType() {
        return "method";
    }
}
