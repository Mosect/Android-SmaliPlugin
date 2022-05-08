package com.mosect.smali.plugin.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SmaliParser {

    public SmaliParseResult<List<SmaliToken>> parseTokens(CharSequence text, int start, int end) {
        List<SmaliToken> result = new ArrayList<>();
        List<SmaliParseError> errors = new ArrayList<>();

        int offset = start;
        while (offset < end) {
            if (match(text, offset, end, "#")) {
                // 注释
                int commentEnd = findCommentEnd(text, offset + 1, end);
                String comment = text.subSequence(offset, commentEnd).toString();
                result.add(new SmaliToken(comment, "comment"));
                offset = commentEnd;
            } else if (match(text, offset, end, "\r\n")) {
                // 换行
                result.add(new SmaliToken("\r\n", "line.crlf"));
                offset += 2;
            } else if (match(text, offset, end, "..")) {
                // .. 运算符
                result.add(new SmaliToken("..", "symbol"));
                offset += 2;
            } else if (match(text, offset, end, "\"")) {
                // 字符串
                int stringEnd = findStringEnd(text, offset + 1, end, errors);
                String string = text.subSequence(offset, stringEnd).toString();
                result.add(new SmaliToken(string, "string"));
                offset = stringEnd;
            } else if (match(text, offset, end, "'")) {
                // 字符
                int charEnd = findCharEnd(text, offset + 1, end, errors);
                String cs = text.subSequence(offset, charEnd).toString();
                result.add(new SmaliToken(cs, "char"));
                offset = charEnd;
            } else {
                char ch = text.charAt(offset);
                switch (ch) {
                    case '\r':
                        result.add(new SmaliToken("\r", "line.cr"));
                        ++offset;
                        break;
                    case '\n':
                        result.add(new SmaliToken("\n", "line.lf"));
                        ++offset;
                        break;
                    case '=':
                    case '(':
                    case ')':
                    case '{':
                    case '}':
                    case ',':
                        result.add(new SmaliToken(String.valueOf(ch), "symbol"));
                        ++offset;
                        break;
                    case '\t':
                    case ' ':
                        result.add(new SmaliToken(String.valueOf(ch), "whitespace"));
                        ++offset;
                        break;
                    default:
                        int wordEnd = findWordEnd(text, offset, end);
                        String word = text.subSequence(offset, wordEnd).toString();
                        if (word.startsWith(".")) {
                            result.add(new SmaliToken(word, "block"));
                        } else {
                            result.add(new SmaliToken(word, "word"));
                        }
                        offset = wordEnd;
                        break;
                }
            }
        }

        return new SmaliParseResult<>(result, errors);
    }

    public SmaliParseResult<SmaliBlockNode> parseDocument(CharSequence text, int start, int end, List<SmaliToken> tokens) {
        List<SmaliParseError> errors = new ArrayList<>();

        // 整理SmaliEndBlock
        List<SmaliNode> nodes = new ArrayList<>(tokens);
        SmaliBlockNode rootBlockNode = new SmaliBlockNode();
        int offset = 0;
        while (offset < nodes.size()) {
            SmaliToken token = (SmaliToken) nodes.get(offset);
            if ("block".equals(token.getTokenType()) && ".end".equals(token.getText())) {
                int wordIndex = findWordToken(nodes, offset + 1);
                SmaliEndNode endNode = new SmaliEndNode();
                if (wordIndex < 0) {
                    endNode.getChildren().add(token);
                    rootBlockNode.getChildren().add(endNode);
                    errors.add(new SmaliParseError("END:Missing end word", "Missing end word", rootBlockNode.length()));
                    ++offset;
                } else {
                    for (int i = offset; i <= wordIndex; i++) {
                        endNode.getChildren().add(nodes.get(i));
                    }
                    rootBlockNode.getChildren().add(endNode);
                    offset = wordIndex + 1;
                }
            } else {
                rootBlockNode.getChildren().add(token);
                ++offset;
            }
        }

        // 创建BlockNode
        generateBlockNodes(rootBlockNode.getChildren(), 0, rootBlockNode.getChildCount(), rootBlockNode);
        generateBlockNodes2(rootBlockNode);

        // 调整块区域
        adjustBlockNode(rootBlockNode);

        return new SmaliParseResult<>(rootBlockNode, errors);
    }

    protected void adjustBlockNode(SmaliBlockNode blockNode) {
        if (blockNode.getChildCount() > 0) {
            List<SmaliNode> children = blockNode.getChildren();
            for (int i = 0; i < children.size(); i++) {
                SmaliNode child = children.get(i);
                if ("block".equals(child.getType())) {
                    SmaliBlockNode childBlock = (SmaliBlockNode) child;
                    SmaliBlockNode converted = convertBlockNode(childBlock);
                    if (converted != childBlock) {
                        children.set(i, converted);
                    }
                    adjustBlockNode(converted);
                }
            }
        }
    }

    protected SmaliBlockNode convertBlockNode(SmaliBlockNode blockNode) {
        String name = blockNode.getBlockName();
        if (null != name) {
            switch (name) {
                case ".class":
                    return createClassNode(blockNode.getChildren(), "class");
                case ".implement":
                    return createClassNode(blockNode.getChildren(), "implement");
                case ".enum":
                    return createClassNode(blockNode.getChildren(), "enum");
                case ".method":
                    SmaliMethodNode methodNode = new SmaliMethodNode();
                    methodNode.setChildren(blockNode.getChildren());
                    return methodNode;
                case ".field":
                    SmaliFieldNode fieldNode = new SmaliFieldNode();
                    fieldNode.setChildren(blockNode.getChildren());
                    return fieldNode;
                case ".annotation":
                    SmaliAnnotationNode annotationNode = new SmaliAnnotationNode();
                    annotationNode.setChildren(blockNode.getChildren());
                    return annotationNode;
            }
        }
        return blockNode;
    }

    protected SmaliClassNode createClassNode(List<SmaliNode> children, String classType) {
        SmaliClassNode classNode = new SmaliClassNode(classType);
        classNode.setChildren(children);
        return classNode;
    }

    protected void generateBlockNodes2(SmaliBlockNode blockNode) {
        List<SmaliNode> nodes = blockNode.getChildren();
        List<SmaliNode> children = new ArrayList<>(nodes.size());
        int offset = 0;
        while (offset < nodes.size()) {
            SmaliNode node = nodes.get(offset);
            if ("block".equals(node.getType())) {
                SmaliBlockNode nextBlock = (SmaliBlockNode) node;
                generateBlockNodes2(nextBlock);
                children.add(nextBlock);
                ++offset;
            } else if ("token".equals(node.getType())) {
                SmaliToken token = (SmaliToken) node;
                if ("block".equals(token.getTokenType())) {
                    int blockEnd = findBlockEnd(nodes, offset + 1);
                    SmaliBlockNode nextBlock = new SmaliBlockNode();
                    for (int i = offset; i < blockEnd; i++) {
                        nextBlock.getChildren().add(nodes.get(i));
                    }
                    children.add(nextBlock);
                    offset = blockEnd;
                } else {
                    children.add(token);
                    ++offset;
                }
            } else {
                children.add(node);
                ++offset;
            }
        }
        blockNode.setChildren(children);
    }

    protected int findBlockEnd(List<SmaliNode> nodes, int start) {
        int validIndex = -1;
        _for:
        for (int i = start; i < nodes.size(); i++) {
            SmaliNode node = nodes.get(i);
            if ("token".equals(node.getType())) {
                SmaliToken token = (SmaliToken) node;
                switch (token.getTokenType()) {
                    case "whitespace":
                    case "comment":
                        break;
                    case "block":
                    case "line.cr":
                    case "line.crlf":
                    case "line.lf":
                        break _for;
                    default:
                        validIndex = i;
                        break;
                }
            } else {
                validIndex = i;
            }
        }
        if (validIndex >= 0) return validIndex + 1;
        return nodes.size();
    }

    protected void generateBlockNodes(List<SmaliNode> nodes, int start, int end, SmaliNode out) {
        int offset = end - 1;
        List<SmaliNode> children = new ArrayList<>();
        while (offset >= start) {
            SmaliNode node = nodes.get(offset);
            if ("end".equals(node.getType())) {
                SmaliEndNode endNode = (SmaliEndNode) node;
                String blockName = endNode.getBlockName();
                if (null == blockName) {
                    children.add(node);
                    --offset;
                    continue;
                }

                int blockStartIndex = findBlockToken(nodes, start, offset, blockName);
                if (blockStartIndex < 0) {
                    blockStartIndex = start;
                }
                SmaliBlockNode nextBlock = new SmaliBlockNode();
                nextBlock.getChildren().add(nodes.get(blockStartIndex));
                generateBlockNodes(nodes, blockStartIndex + 1, offset, nextBlock);
                nextBlock.getChildren().add(nodes.get(offset));
                children.add(nextBlock);
                offset = blockStartIndex - 1;
            } else {
                children.add(node);
                --offset;
            }
        }
        Collections.reverse(children);
        out.getChildren().addAll(children);
    }

    protected int findBlockToken(List<SmaliNode> nodes, int start, int end, String blockName) {
        String startTag = "." + blockName;
        for (int i = end - 1; i >= start; i--) {
            SmaliNode node = nodes.get(i);
            if ("token".equals(node.getType())) {
                SmaliToken token = (SmaliToken) node;
                if ("block".equals(token.getTokenType())) {
                    if (startTag.equals(token.getText())) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    protected int findWordToken(List<SmaliNode> nodes, int start) {
        for (int i = start; i < nodes.size(); i++) {
            SmaliNode node = nodes.get(i);
            if ("token".equals(node.getType())) {
                SmaliToken token = (SmaliToken) node;
                if ("word".equals(token.getTokenType())) {
                    return i;
                }
            }
        }
        return -1;
    }

    protected int findCommentEnd(CharSequence text, int start, int end) {
        for (int i = start; i < end; i++) {
            if (matchLine(text, i, end)) return i;
        }
        return end;
    }

    protected int findStringEnd(CharSequence text, int start, int end, List<SmaliParseError> outErrors) {
        int offset = start;
        while (offset < end) {
            if (matchLine(text, offset, end)) {
                outErrors.add(new SmaliParseError("STRING:UNEXPECTED_LINE_CHAR", "Unexpected line char", offset));
                return offset;
            } else if (match(text, offset, end, "\"")) {
                // 字符串结束
                return offset + 1;
            }
            offset = nextCharIndex(text, offset, end, outErrors);
        }
        outErrors.add(new SmaliParseError("STRING:MISSING_END", "Missing string end", end));
        return end;
    }

    protected int findCharEnd(CharSequence text, int start, int end, List<SmaliParseError> outErrors) {
        int offset = start;
        int charCount = 0;
        while (offset < end) {
            if (matchLine(text, offset, end)) {
                outErrors.add(new SmaliParseError("CHAR:UNEXPECTED_LINE_CHAR", "Unexpected line char", offset));
                return offset;
            } else if (match(text, offset, end, "'")) {
                // 字符结束
                if (charCount == 0) {
                    outErrors.add(new SmaliParseError("CHAR:MISSING_CONTENT", "Missing char content", start + 1));
                } else if (charCount > 1) {
                    outErrors.add(new SmaliParseError("CHAR:MULTIPLE_CHARS", "Multiple chars", start + 1));
                }
                return offset + 1;
            }
            offset = nextCharIndex(text, offset, end, outErrors);
            ++charCount;
        }
        outErrors.add(new SmaliParseError("CHAR:MISSING_END", "Missing char end", end));
        return end;
    }

    protected boolean matchLine(CharSequence text, int start, int end) {
        return match(text, start, end, "\r") || match(text, start, end, "\n");
    }

    protected int nextCharIndex(CharSequence text, int start, int end, List<SmaliParseError> outErrors) {
        if (match(text, start, end, "\\u")) {
            // 16进制字符
            int offset = start + 2;
            for (int i = 0; i < 4; i++) {
                int index = offset + i;
                if (index < end) {
                    char ch = text.charAt(index);
                    boolean hex = (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F');
                    if (!hex) {
                        SmaliParseError error = new SmaliParseError("CHAR:INVALID_HEX_CHAR", "Invalid hex char", index);
                        outErrors.add(error);
                        return index;
                    }
                } else {
                    SmaliParseError error = new SmaliParseError("CHAR:INVALID_HEX_CHAR_LENGTH", "Invalid hex char length", index);
                    outErrors.add(error);
                    return index;
                }
            }
            return offset + 4;
        } else if (match(text, start, end, "\\")) {
            // 转义
            int offset = start + 1;
            int size = 0;
            boolean octEscape = false; // 8进制转义
            for (int i = 0; i < 3; i++) {
                int index = offset + i;
                if (index < end) {
                    ++size;
                    char ch = text.charAt(index);
                    if (size == 1) {
                        octEscape = ch >= '0' && ch <= '7';
                    } else {
                        if (octEscape) {
                            // 8进制转义
                            boolean oct = ch >= '0' && ch <= '7';
                            if (!oct) {
                                return index;
                            }
                        } else {
                            // 非8进制转义
                            return index;
                        }
                    }
                } else {
                    if (size == 0) {
                        SmaliParseError error = new SmaliParseError("CHAR:INVALID_ESCAPE_CHAR_LENGTH", "Invalid hex char length", index);
                        outErrors.add(error);
                    }
                    return index;
                }
            }
            return offset + 3;
        }
        return start + 1;
    }

    protected int findWordEnd(CharSequence text, int start, int end) {
        for (int i = start; i < end; i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '\r':
                case '\n':
                case '\t':
                case ' ':
                case '=':
                case '(':
                case ')':
                case '{':
                case '}':
                case ',':
                    return i;
            }
        }
        return end;
    }

    protected boolean match(CharSequence text, int offset, int end, String target) {
        if (end - offset >= target.length()) {
            for (int i = 0; i < target.length(); i++) {
                if (target.charAt(i) != text.charAt(offset + i)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
