package com.chaitu.demo.myntra;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Driver {

    private static final String URL1 = "https://www.myntra.com/studio/influencer?id=tFLOgMr19O&affiliateId=tFLOgMr19O&shared=true&utm_medium=social_share_ugc_profile&utm_source=ugc_affiliate&utm_campaign=tFLOgMr19O&affiliate_id=tFLOgMr19O";

    private static final int INITIAL_WAIT_MS = 1200;
    private static final int CLICK_WAIT_MS = 1000;
    private static int totalClicks = 0;
    private static final int POST_SCROLL_WAIT_MS = 500;
    private static final int MAX_POSTS = 50;
    private static final int MAX_PRODUCT_SCROLLS = 10;
    private static final int MAX_POST_SCROLL_ATTEMPTS = 10;
    private static final int MAX_FIRST_TRAY_ATTEMPTS = 6;
    private static final int TRAYS_PER_POST = 5;

    public static void main(String[] args)
            throws Exception {
        long startedAt = System.nanoTime();
        System.out.println("starting123 .. "+ LocalDateTime.now());
        VisitPosts.visitPosts();
        processPostTrayClicks();

        printElapsedSeconds(startedAt);
    }

    public static void processPostTrayClicks()
            throws Exception {

        long startedAt = System.nanoTime();
        System.out.println("starting123 .. "+ LocalDateTime.now());

        if (!openAndPrepareFeed()) {
            printElapsedSeconds(startedAt);
            return;
        }

        processFeed();
        System.out.println("DONE clicking , totalClicks : "+totalClicks);
        printElapsedSeconds(startedAt);
    }

    private static boolean openAndPrepareFeed()
            throws Exception {

        AdbUtils.openLink(URL1);
        Thread.sleep(INITIAL_WAIT_MS);
        AdbUtils.dumpUI();

        if (!UiParser.hasProductTray()) {
            AdbUtils.scrollLittleUntilTray(550, MAX_FIRST_TRAY_ATTEMPTS);
        }

        AdbUtils.dumpUI();
        return UiParser.hasProductTray();
    }

    private static void processFeed()
            throws Exception {

        Set<String> seenPostKeys = new HashSet<>();
        int totalPosts = UiParser.parseProfilePostCount();
        int processedPosts = 0;

        for (int postRound = 0;
             postRound < MAX_POSTS && (totalPosts < 0 || processedPosts < totalPosts);
             postRound++) {

            AdbUtils.dumpUI();
            String currentPostKey = UiParser.parsePostKey();

            if (!currentPostKey.isBlank() && !seenPostKeys.add(currentPostKey)) {
                break;
            }

            System.out.println("Post " + postRound
                    + (currentPostKey.isBlank() ? "" : " -> " + currentPostKey));

            ensureProductTrayVisible();
            clickFirstVisibleTraysForPost(TRAYS_PER_POST);
            processedPosts++;

            if (!advanceToNextPost(seenPostKeys, currentPostKey)) {
                break;
            }
        }
    }

    private static void ensureProductTrayVisible()
            throws Exception {

        for (int attempt = 0; attempt < MAX_FIRST_TRAY_ATTEMPTS; attempt++) {

            AdbUtils.dumpUI();

            if (UiParser.hasProductTray()) {
                return;
            }

            AdbUtils.scrollLittleUntilTray(550, 1);
            Thread.sleep(500);
        }
    }

    private static void processProductTray()
            throws Exception {

        Set<String> clickedProductsInPost = new HashSet<>();
        String lastTraySignature = "";
        int stagnantRounds = 0;

        for (int trayRound = 0; trayRound < MAX_PRODUCT_SCROLLS; trayRound++) {

            AdbUtils.dumpUI();

            List<UiParser.Product> products =
                    UiParser.parse();

            System.out.println("  Tray pass " + (trayRound + 1)
                    + " | visible products: " + products.size());

            int swipeY = getSwipeY(products);
            if (swipeY < 0) {
                break;
            }

            int newProducts = clickNewProducts(products, clickedProductsInPost);

            String traySignature = buildTraySignature(products);
            if (traySignature.equals(lastTraySignature)) {
                stagnantRounds++;
            } else {
                stagnantRounds = 0;
            }

            lastTraySignature = traySignature;

            if (newProducts == 0 && stagnantRounds >= 1) {
                break;
            }

            AdbUtils.scrollRight(swipeY);
            Thread.sleep(CLICK_WAIT_MS);

            if (stagnantRounds >= 2) {
                break;
            }
        }
    }

    private static void clickFirstVisibleTraysForPost(int traysToClick)
            throws Exception {

        int targetCount = Math.max(1, Math.min(traysToClick, 5));
        Set<String> clickedProductsInPost = new HashSet<>();

        AdbUtils.dumpUI();
        List<UiParser.Product> products = UiParser.parse();

        int visibleToClick = Math.min(targetCount, products.size());
        System.out.println("  Visible trays: " + products.size()
                + " | clicking first " + visibleToClick);

        for (int i = 0; i < visibleToClick; i++) {

            UiParser.Product product = products.get(i);
            String key = product.brand + "|" + product.bounds;

            if (!clickedProductsInPost.add(key)) {
                continue;
            }

            System.out.println("    Clicked tray " + (i + 1)
                    + ": " + product.brand + " -> " + product.price +" at : "+ LocalDateTime.now());

            AdbUtils.tap(product.x, product.y);
            Thread.sleep(CLICK_WAIT_MS);

            AdbUtils.clicktoBackButoon();
            Thread.sleep(CLICK_WAIT_MS);

            totalClicks++;
           // waitForProductTrayVisible(MAX_FIRST_TRAY_ATTEMPTS);
        }
    }

    private static UiParser.Product findNextUnclickedProduct(
            List<UiParser.Product> products,
            Set<String> clickedProductsInPost) {

        for (UiParser.Product product : products) {

            String key = product.brand + "|" + product.bounds;

            if (clickedProductsInPost.contains(key)) {
                continue;
            }

            return product;
        }

        return null;
    }

    private static int getSwipeY(List<UiParser.Product> products) {

        if (products.isEmpty()) {
            return -1;
        }

        int swipeY = 0;

        for (UiParser.Product product : products) {
            swipeY = Math.max(swipeY, product.y);
        }

        return swipeY;

    }

    private static int clickNewProducts(
            List<UiParser.Product> products,
            Set<String> clickedProductsInPost) throws Exception {

        int newProducts = 0;

        for (UiParser.Product p : products) {

            String key = p.brand + "|" + p.bounds;

            if (!clickedProductsInPost.add(key)) {
                continue;
            }

            newProducts++;
            System.out.println("    Clicked product: " + p.brand + " -> " + p.price);

            AdbUtils.tap(p.x, p.y);
            Thread.sleep(CLICK_WAIT_MS);

            AdbUtils.clicktoBackButoon();
            Thread.sleep(500);

            totalClicks++;
            waitForProductTrayVisible(MAX_FIRST_TRAY_ATTEMPTS);
        }

        return newProducts;
    }

    private static String buildTraySignature(List<UiParser.Product> products) {

        StringBuilder signature = new StringBuilder();
        for (UiParser.Product product : products) {
            signature.append(product.brand)
                    .append('@')
                    .append(product.bounds)
                    .append('|');
        }
        return signature.toString();

    }

    private static void waitForProductTrayVisible(int maxAttempts)
            throws Exception {

        for (int attempt = 0; attempt < maxAttempts; attempt++) {

            AdbUtils.dumpUI();

            if (UiParser.hasProductTray()) {
                Thread.sleep(CLICK_WAIT_MS);
                return;
            }

            Thread.sleep(CLICK_WAIT_MS);
        }
    }

    private static boolean advanceToNextPost(
            Set<String> seenPostKeys,
            String currentPostKey)
            throws Exception {

        int[] scrollSteps = {420, 260, 180};

        for (int attempt = 0; attempt < MAX_POST_SCROLL_ATTEMPTS; attempt++) {

            int distancePx = scrollSteps[Math.min(attempt, scrollSteps.length - 1)];
            AdbUtils.scrollLittle(distancePx);
            Thread.sleep(POST_SCROLL_WAIT_MS);

            AdbUtils.dumpUI();
            if (!UiParser.hasProductTray()) {
                continue;
            }

            String nextPostKey = UiParser.parsePostKey();
            if (nextPostKey.isBlank()) {
                continue;
            }

            if (!nextPostKey.equals(currentPostKey) && !seenPostKeys.contains(nextPostKey)) {
                return true;
            }
        }

        return false;
    }

    private static void printElapsedSeconds(long startedAt) {

        long elapsedNanos = System.nanoTime() - startedAt;
        double elapsedSeconds = elapsedNanos / 1_000_000_000.0;

        System.out.printf("Total time: %.2f seconds%n", elapsedSeconds);
        System.out.printf("Total time: %.2f minutes %n", elapsedSeconds/60.0);
    }

}
