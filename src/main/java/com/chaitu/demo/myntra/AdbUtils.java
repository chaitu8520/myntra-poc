package com.chaitu.demo.myntra;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AdbUtils {

    private static final String ADB =
            "C:\\Program Files\\BlueStacks_nxt\\HD-Adb.exe";

    private static final String DEVICE =
            "127.0.0.1:5555";
    private static final String UI_XML_PATH =
            "ui.xml";
    private static final Path UI_DUMP_DIR =
            Paths.get("C:\\myntra");
    private static int dumpIndex = 1;

    public static void execute(String... command)
            throws IOException, InterruptedException {

        ProcessBuilder builder = new ProcessBuilder(command);

        Process process = builder.start();

        process.waitFor();
    }
    public static void scrollLittle() throws Exception {

        scrollLittle(420);

    }

    public static void scrollLittle(int distancePx) throws Exception {

        swipeVertical(distancePx);

    }

    public static void scrollUntilVisibleText(
            String text,
            int distancePx,
            int maxAttempts) throws Exception {

        for (int attempt = 0; attempt < maxAttempts; attempt++) {

            if (UiParser.hasVisibleText(text)) {
                return;
            }

            swipeVertical(distancePx);
            Thread.sleep(1200);
            dumpUI();
        }
    }

    public static boolean scrollLittleUntilTray(int distancePx, int maxAttempts)
            throws Exception {

        for (int attempt = 0; attempt < maxAttempts; attempt++) {

            swipeVertical(distancePx);

            Thread.sleep(1500);
            dumpUI();

            if (UiParser.hasProductTray()) {
                return true;
            }
        }

        return UiParser.hasProductTray();

    }

    private static void swipeVertical(int distancePx) throws Exception {

        int startX = 720;
        int startY = 760;
        int endX = 720;
        int endY = Math.max(180, startY - distancePx);
        int duration = Math.max(250, Math.min(1400, distancePx * 3));

        execute(
                ADB,
                "-s",
                DEVICE,
                "shell",
                "input",
                "swipe",
                String.valueOf(startX),   // startX
                String.valueOf(startY),   // startY
                String.valueOf(endX),   // endX
                String.valueOf(endY),   // endY
                String.valueOf(duration)    // duration(ms)
        );

    }

    public static void openLink(String url)
            throws Exception {

        execute(
                ADB,
                "-s",
                DEVICE,
                "shell",
                "am",
                "start",
                "-a",
                "android.intent.action.VIEW",
                "-d",
                url
        );
    }

    public static void dumpUI() throws Exception {

        Files.createDirectories(UI_DUMP_DIR);

        String numberedDumpPath =
                UI_DUMP_DIR.resolve("ui_" + dumpIndex + ".xml").toString();
        dumpIndex++;

        execute(
                ADB,
                "-s",
                DEVICE,
                "shell",
                "uiautomator",
                "dump",
                "/sdcard/ui.xml"
        );

        execute(
                ADB,
                "-s",
                DEVICE,
                "pull",
                "/sdcard/ui.xml",
                numberedDumpPath
        );

        execute(
                ADB,
                "-s",
                DEVICE,
                "pull",
                "/sdcard/ui.xml",
                UI_XML_PATH
        );

    }
    public static void tap(int x,int y)
            throws Exception{

        execute(
                ADB,
                "-s",
                DEVICE,
                "shell",
                "input",
                "tap",
                String.valueOf(x),
                String.valueOf(y)
        );

    }

    public static void scrollRight(int y) throws Exception {

        int swipeY = Math.max(200, Math.min(y, 880));

        execute(
                ADB,
                "-s",
                DEVICE,
                "shell",
                "input",
                "swipe",
                "850",   // startX
                String.valueOf(swipeY),   // startY
                "150",   // endX
                String.valueOf(swipeY),   // endY
                "900"    // duration(ms)
        );

    }

    public static void clicktoBackButoon()
            throws Exception {

        execute(
                ADB,
                "-s",
                DEVICE,
                "shell",
                "input",
                "keyevent",
                "4"
        );
    }

}
