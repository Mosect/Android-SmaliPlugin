package com.mosect.smali.plugin;

import static org.junit.Assert.assertEquals;

import com.mosect.smali.plugin.dex.DexHandler;
import com.mosect.smali.plugin.dex.DexMaker;
import com.mosect.smali.plugin.parser.SmaliBlockNode;
import com.mosect.smali.plugin.parser.SmaliNode;
import com.mosect.smali.plugin.parser.SmaliParseError;
import com.mosect.smali.plugin.parser.SmaliParseResult;
import com.mosect.smali.plugin.parser.SmaliParser;
import com.mosect.smali.plugin.parser.SmaliToken;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainTest {

    @Test
    public void testParseTokens() throws Exception {
        File dir = new File("C:\\Users\\MosectAdmin\\es-file-explorer-4-2-8-7-1");
        List<File> smaliFiles = new ArrayList<>(128);
        listSmaliFiles(dir, smaliFiles);
        for (File file : smaliFiles) {
            parseSmaliFile(file);
        }
    }

    @Test
    public void testFile() throws Exception {
        File file = new File("C:\\Users\\MosectAdmin\\es-file-explorer-4-2-8-7-1\\smali\\androidx\\mediarouter\\media\\RemoteControlClientCompat$PlaybackInfo.smali");
        parseSmaliFile(file);
    }

    @Test
    public void testDexMaker() throws Exception {
        File dir = new File("E:\\Temp\\2022012118\\official-menglar-1.1.28-2022011810\\smali");
        List<File> smaliFiles = new ArrayList<>(128);
        listSmaliFiles(dir, smaliFiles);
        DexMaker dexMaker = new DexMaker("classes");
        for (File file : smaliFiles) {
            dexMaker.addSmaliFile(null, file);
        }
        File dexFile = new File("build/classes.dex");
        boolean ok = dexMaker.makeDex(dexFile);
        Assert.assertTrue(ok);
    }

    @Test
    public void testDexHandler() throws Exception {
        DexHandler dexHandler = new DexHandler();
        dexHandler.addJavaDexFile(new File("E:\\Temp\\2022012118\\classes.dex"));
        dexHandler.addOriginalSourceDir(1, new File("E:\\Temp\\2022012118\\app1\\smali"));
        File tempFile = new File("build/temp");
        deleteFile(tempFile);
        dexHandler.setTempDir(tempFile);
        dexHandler.setApiLevel(15);
        dexHandler.run();
    }

    private void deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles(file1 -> {
                String name = file1.getName();
                return !".".equals(name) && !"..".equals(name);
            });
            if (null != files) {
                for (File child : files) {
                    deleteFile(child);
                }
            }
            file.delete();
        } else if (file.isFile()) {
            file.delete();
        }
    }

    private void parseSmaliFile(File file) throws IOException {
//        System.out.println("Parse: " + file.getAbsolutePath());
        String text = readFile(file);
        SmaliParser parser = new SmaliParser();
        SmaliParseResult<List<SmaliToken>> result = parser.parseTokens(text, 0, text.length());
        assertEquals(result.getErrors().size(), 0);
        for (SmaliToken token : result.getResult()) {
            if ("word".equals(token.getTokenType())) {
                boolean valid = token.getText().matches("^[-.a-zA-Z0-9_\\$/;:<>\\[\\]]+$");
                if (!valid) {
                    System.err.println("InvalidWord: " + token.getText());
                    throw new IllegalStateException("Invalid word");
                }
            }
        }
        SmaliParseResult<SmaliBlockNode> blockResult = parser.parseDocument(text, 0, text.length(), result.getResult());
        for (SmaliNode node : blockResult.getResult().getChildren()) {
            if ("annotation".equals(node.getType())) {
                System.out.println("ExistAnnotation: " + file.getAbsolutePath());
                break;
            }
        }
        if (blockResult.getErrors().size() > 0) {
            for (SmaliParseError error : blockResult.getErrors()) {
                System.err.println(error);
            }
            throw new IllegalStateException("Exist error");
        }
    }

    private String readFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream temp = new ByteArrayOutputStream(512)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) >= 0) {
                temp.write(buffer, 0, len);
            }
            return temp.toString(StandardCharsets.UTF_8);
        }
    }

    private void listSmaliFiles(File dir, List<File> out) {
        File[] files = dir.listFiles(file -> file.isFile() && file.getName().endsWith(".smali"));
        if (null != files) {
            Collections.addAll(out, files);
        }
        File[] dirs = dir.listFiles(file -> file.isDirectory() && !".".equals(file.getName()) && !"..".equals(file.getName()));
        if (null != dirs) {
            for (File childDir : dirs) {
                listSmaliFiles(childDir, out);
            }
        }
    }
}
