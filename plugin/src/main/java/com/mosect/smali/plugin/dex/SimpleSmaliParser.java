package com.mosect.smali.plugin.dex;

import com.mosect.smali.plugin.parser.SmaliBlockNode;
import com.mosect.smali.plugin.parser.SmaliClassNode;
import com.mosect.smali.plugin.parser.SmaliParseError;
import com.mosect.smali.plugin.parser.SmaliParseResult;
import com.mosect.smali.plugin.parser.SmaliParser;
import com.mosect.smali.plugin.util.TextUtils;

import java.io.File;
import java.io.IOException;

public class SimpleSmaliParser extends SmaliParser {

    public SmaliBlockNode parse(File smaliFile) throws IOException, SmaliException {
        SmaliParseResult<SmaliBlockNode> result = parseFileWithUtf8(smaliFile);
        if (!result.getErrors().isEmpty()) {
            SmaliParseError error = result.getErrors().get(0);
            throw createException(smaliFile, error);
        }
        SmaliClassNode classNode = result.getResult().findNode("class");
        if (null == classNode) {
            throw createException(smaliFile, new SmaliParseError("CLASS:MISSING_CLASS_NODE", "Missing class node", 0));
        }
        if (TextUtils.isEmpty(classNode.getClassName())) {
            throw createException(smaliFile, new SmaliParseError("CLASS:INVALID_CLASS_NODE", "Invalid class node", 0));
        }
        return result.getResult();
    }

    private SmaliException createException(File file, SmaliParseError error) {
        String errorMsg = String.format(
                "ErrorSmali{%s}[%s:%s]>>> %s",
                file.getAbsolutePath(),
                error.getLineIndex() + 1,
                error.getLineOffset() + 1,
                error.getMessage()
        );
        return new SmaliException(errorMsg, error.getType(), error.getLineIndex(), error.getLineOffset());
    }
}
