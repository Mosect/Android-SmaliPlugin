package com.mosect.smali.plugin.util;

import java.io.File;
import java.io.IOException;

public final class IOUtils {

    private IOUtils() {
    }

    public static void initParent(File file) throws IOException {
        File parent = file.getParentFile();
        if (null != parent && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Can't create directory {" + parent.getAbsolutePath() + "}");
        }
    }
}
