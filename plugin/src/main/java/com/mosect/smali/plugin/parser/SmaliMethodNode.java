package com.mosect.smali.plugin.parser;

import java.io.IOException;
import java.util.List;

public class SmaliMethodNode extends SmaliBlockNode {

    @Override
    public String getId() {
        if (getChildCount() > 0) {
            int mode = 0;
            List<SmaliNode> nodes = getChildren();
            int end = -1;
            int start = -1;
            _for:
            for (int i = nodes.size() - 1; i >= 0; i--) {
                SmaliNode node = nodes.get(i);
                if ("token".equals(node.getType())) {
                    SmaliToken token = (SmaliToken) node;
                    switch (mode) {
                        case 0:
                            if ("symbol".equals(token.getTokenType()) && ")".equals(token.getText())) {
                                end = i + 1;
                                mode = 1;
                            }
                            break;
                        case 1:
                            if ("symbol".equals(token.getTokenType()) && "(".equals(token.getText())) {
                                mode = 2;
                            }
                            break;
                        case 2:
                            if ("word".equals(token.getTokenType())) {
                                start = i;
                                break _for;
                            }
                            break;
                    }
                }
            }
            if (end >= 0 && start >= 0) {
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
