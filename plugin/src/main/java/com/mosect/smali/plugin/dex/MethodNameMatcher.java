package com.mosect.smali.plugin.dex;

import com.mosect.smali.plugin.parser.SmaliNode;
import com.mosect.smali.plugin.parser.SmaliNodeMatcher;
import com.mosect.smali.plugin.parser.SmaliToken;

public class MethodNameMatcher implements SmaliNodeMatcher {

    private int state = 0;
    private int nameNodeIndex = -1;

    @Override
    public void reset() {
        state = 0;
        nameNodeIndex = -1;
    }

    public int getNameNodeIndex() {
        return nameNodeIndex;
    }

    @Override
    public int match(SmaliNode parent, SmaliNode node, int index) {
        switch (state) {
            case 0:
                if ("token".equals(node.getType())) {
                    SmaliToken token = (SmaliToken) node;
                    if ("word".equals(token.getTokenType())) {
                        state = 1;
                        nameNodeIndex = index;
                    }
                }
                if (state != 1) return 0;
                return 1;
            case 1:
                if ("token".equals(node.getType())) {
                    SmaliToken token = (SmaliToken) node;
                    if ("symbol".equals(token.getTokenType()) && "(".equals(token.getText())) {
                        state = 2;
                    }
                }
                if (state != 2) return 0;
                return 2;
        }
        return 0;
    }
}
