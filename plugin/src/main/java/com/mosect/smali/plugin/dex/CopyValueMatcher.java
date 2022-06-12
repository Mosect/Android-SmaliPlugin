package com.mosect.smali.plugin.dex;

import com.mosect.smali.plugin.parser.SmaliNode;
import com.mosect.smali.plugin.parser.SmaliNodeMatcher;
import com.mosect.smali.plugin.parser.SmaliToken;

public class CopyValueMatcher implements SmaliNodeMatcher {

    private int state = 0;
    private String value;

    @Override
    public void reset() {
        state = 0;
        value = null;
    }

    public String getValue() {
        return value;
    }

    @Override
    public int match(SmaliNode parent, SmaliNode node, int index) {
        switch (state) {
            case 0:
                if ("token".equals(node.getType())) {
                    SmaliToken token = (SmaliToken) node;
                    if ("word".equals(token.getTokenType()) && "value".equals(token.getText())) {
                        state = 1;
                    }
                }
                if (state != 1) return 0;
                return 1;
            case 1:
                if ("token".equals(node.getType())) {
                    SmaliToken token = (SmaliToken) node;
                    if ("symbol".equals(token.getTokenType()) && "=".equals(token.getText())) {
                        state = 2;
                    }
                }
                if (state != 2) return 0;
                return 1;
            case 2:
                if ("token".equals(node.getType())) {
                    SmaliToken token = (SmaliToken) node;
                    if ("string".equals(token.getTokenType())) {
                        state = 3;
                        String text = token.getText();
                        value = text.substring(1, text.length() - 1);
                    }
                }
                if (state != 3) return 0;
                return 2;
        }
        return 0;
    }
}
