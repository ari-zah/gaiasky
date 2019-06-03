/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.desktop.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.Logger;

import java.io.*;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class CrashReporter {

    public static void reportCrash(Throwable t, Logger.Log logger) {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-HHmmss");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        String stackTrace = sw.toString();

        Date now = new Date();

        Array<String> crashInfo = new Array<>();
        crashInfo.add("#");
        crashInfo.add("# GAIA SKY CRASH REPORT");
        crashInfo.add("# " + now);
        crashInfo.add("");
        crashInfo.add("## STACK TRACE");
        crashInfo.add(stackTrace);

        appendSystemInfo(crashInfo);

        for (String str : crashInfo)
            print(logger, str);

        // File reporting
        File crashDir = SysUtils.getCrashReportsDir();
        crashDir.mkdirs();

        File crashReportFile = new File(crashDir, "gaiasky_crash_" + df.format(now) + ".txt");
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(crashReportFile));
            for (String str : crashInfo) {
                writer.write(str);
                writer.newLine();
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Writing crash report crashed... Inception level 1 achieved! :_D", e);
            } else {
                System.err.println("Writing crash report crashed... Inception level 1 achieved! :_D");
                e.printStackTrace(System.err);
            }
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();

                String crf1 = "Crash report file saved to: " + crashReportFile.getPath();
                String crf2 = "Please attach this file to the bug report";
                String crf3 = "Create a bug report here: https://gitlab.com/langurmonkey/gaiasky/issues";
                int len = Math.max(crf1.length(), Math.max(crf2.length(), crf3.length()));
                char[] chars = new char[len];
                Arrays.fill(chars, '#');
                String separatorLine = new String(chars);
                print(logger, "");
                print(logger, separatorLine);
                print(logger, crf1);
                print(logger, crf2);
                print(logger, crf3);
                print(logger, separatorLine);
                print(logger, "");
            } catch (Exception e) {
            }
        }
    }

    private static void print(Logger.Log logger, String str) {
        if (logger == null) {
            System.err.println(str);
        } else {
            logger.error(str);
        }
    }

    private static void appendSystemInfo(Array<String> strArray) {
        /* Gaia Sky info */
        if (GlobalConf.version != null) {
            strArray.add("");
            strArray.add("## GAIA SKY INFORMATION");
            strArray.add("Version: " + GlobalConf.version.version);
            strArray.add("Build: " + GlobalConf.version.build);
            strArray.add("Builder: " + GlobalConf.version.builder);
            strArray.add("System: " + GlobalConf.version.system);
            strArray.add("Build time: " + GlobalConf.version.buildtime);
        }

        /* Java info */
        strArray.add("");
        strArray.add("## JAVA INFORMATION");
        strArray.add("Java version: " + System.getProperty("java.version"));
        strArray.add("Java runtime name: " + System.getProperty("java.runtime.name"));
        strArray.add("Java VM name: " + System.getProperty("java.vm.name"));
        strArray.add("Java VM version: " + System.getProperty("java.vm.version"));
        strArray.add("Java VM vendor: " + System.getProperty("java.vm.vendor"));

        /* OS info */
        strArray.add("");
        strArray.add("## OS INFORMATION");
        strArray.add("OS name: " + System.getProperty("os.name"));
        strArray.add("OS version: " + System.getProperty("os.version"));
        strArray.add("OS architecture: " + System.getProperty("os.arch"));

        strArray.add("");
        try {
            /* GL info */
            strArray.add("## GL INFORMATION");
            strArray.add("Graphcis device: " + Gdx.gl.glGetString(GL20.GL_RENDERER));
            strArray.add("GL vendor: " + Gdx.gl.glGetString(GL20.GL_VENDOR));
            strArray.add("GL version: " + Gdx.gl.glGetString(GL20.GL_VERSION));
            strArray.add("GLSL version: " + Gdx.gl.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));

            String extensions = Gdx.gl.glGetString(GL20.GL_EXTENSIONS);
            IntBuffer buf = BufferUtils.newIntBuffer(16);
            if (extensions == null || extensions.isEmpty()) {
                Gdx.gl.glGetIntegerv(GL30.GL_NUM_EXTENSIONS, buf);
                int next = buf.get(0);
                String[] extensionsstr = new String[next];
                for (int i = 0; i < next; i++) {
                    extensionsstr[i] = Gdx.gl30.glGetStringi(GL30.GL_EXTENSIONS, i);
                }
                extensions = arrayToStr(extensionsstr, " ");
            }
            strArray.add("GL extensions: " + extensions);

            Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, buf);
            int maxSize = buf.get(0);
            strArray.add("GL max texture size: " + maxSize);
        } catch (Exception e) {
            strArray.add("## GL INFORMATION not available");
        }

        strArray.add("");
        strArray.add("## SYSTEM INFORMATION");
        strArray.add("Available processors (cores): " + Runtime.getRuntime().availableProcessors());
        strArray.add("Free memory (bytes): " + Runtime.getRuntime().freeMemory());
        long maxMemory = Runtime.getRuntime().maxMemory();
        strArray.add("Maximum memory (bytes): " + (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));
        strArray.add("Total memory available to JVM (bytes): " + Runtime.getRuntime().totalMemory());
        File[] roots = File.listRoots();
        for (File root : roots) {
            strArray.add("File system root: " + root.getAbsolutePath());
            strArray.add("Total space (bytes): " + root.getTotalSpace());
            strArray.add("Free space (bytes): " + root.getFreeSpace());
            strArray.add("Usable space (bytes): " + root.getUsableSpace());
        }
    }

    private static String arrayToStr(String[] arr, String sep) {
        String buff = new String();
        for (int i = 0; i < arr.length; i++) {
            buff += arr[i] + sep;
        }
        return buff;
    }

}
