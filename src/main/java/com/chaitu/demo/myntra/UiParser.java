package com.chaitu.demo.myntra;

import org.w3c.dom.*;
import javax.xml.parsers.*;

import java.io.File;
import java.util.*;
import java.util.regex.*;

public class UiParser {

    public static class Product {

        public String brand;
        public String price;
        public int x;
        public int y;
        public String bounds;

        public Product(String brand, String price, int x, int y, String bounds) {
            this.brand = brand;
            this.price = price;
            this.x = x;
            this.y = y;
            this.bounds = bounds;
        }

        @Override
        public String toString() {
            return brand + " -> " + price;
        }
    }

    public static List<Product> parse() throws Exception {

        List<Product> products = new ArrayList<>();
        Set<String> seenBounds = new HashSet<>();

        DocumentBuilder builder =
                DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder();

        Document document =
                builder.parse(new File("ui.xml"));

        NodeList nodes =
                document.getElementsByTagName("node");

        Pattern pattern =
                Pattern.compile("\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]");

        for (int i = 0; i < nodes.getLength(); i++) {

            Element brandNode = (Element) nodes.item(i);

            if (!"product_tray_product_brand".equals(
                    brandNode.getAttribute("content-desc")))
                continue;

            String brand =
                    brandNode.getAttribute("text");

            String brandBounds =
                    brandNode.getAttribute("bounds");

            Matcher matcher =
                    pattern.matcher(brandBounds);

            if (!matcher.find())
                continue;

            Element clickable = findClickableAncestor(brandNode);
            if (clickable == null) {
                continue;
            }

            String clickBounds =
                    clickable.getAttribute("bounds");

            if (!seenBounds.add(clickBounds)) {
                continue;
            }

            Matcher m2 =
                    pattern.matcher(clickBounds);

            if (!m2.find()) {
                continue;
            }

            int x1 = Integer.parseInt(m2.group(1));
            int y1 = Integer.parseInt(m2.group(2));
            int x2 = Integer.parseInt(m2.group(3));
            int y2 = Integer.parseInt(m2.group(4));

            String price = findDescendantText(clickable,
                    "product_tray_product_mrp");

            if (price.isBlank()) {
                price = findDescendantText(clickable,
                        "product_tray_product_discounted_price");
            }
            if (price.isBlank()) {
                price = "price unavailable";
            }

            products.add(

                    new Product(

                            brand,
                            price,

                            (x1 + x2) / 2,

                            (y1 + y2) / 2,

                            clickBounds

                    )

            );

        }

        return products;

    }

    public static String parsePostKey() throws Exception {

        DocumentBuilder builder =
                DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder();

        Document document =
                builder.parse(new File("ui.xml"));

        NodeList nodes =
                document.getElementsByTagName("node");

        Pattern pattern =
                Pattern.compile("\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]");

        List<TextBox> footerTexts = new ArrayList<>();

        for (int i = 0; i < nodes.getLength(); i++) {

            Element node = (Element) nodes.item(i);
            String text = node.getAttribute("text");

            if (text == null || text.isBlank()) {
                continue;
            }

            if (text.matches("\\d+(\\.\\d+)?")) {
                continue;
            }

            if ("Following".equals(text) ||
                    "Posts".equals(text) ||
                    "Share Profile".equals(text) ||
                    "Spotlight Profile".equals(text)) {
                continue;
            }

            String bounds = node.getAttribute("bounds");
            Matcher matcher = pattern.matcher(bounds);
            if (!matcher.find()) {
                continue;
            }

            int y1 = Integer.parseInt(matcher.group(2));
            int y2 = Integer.parseInt(matcher.group(4));
            int x1 = Integer.parseInt(matcher.group(1));
            int x2 = Integer.parseInt(matcher.group(3));

            if (y1 < 560 || y1 > 760) {
                continue;
            }

            if (x2 > 500) {
                continue;
            }

            if (isNoiseText(text)) {
                continue;
            }

            footerTexts.add(new TextBox(text, x1, y1));
        }

        footerTexts.sort(
                Comparator.comparingInt((TextBox t) -> t.y)
                        .thenComparingInt(t -> t.x));

        List<String> parts = new ArrayList<>();
        for (TextBox box : footerTexts) {
            if (!parts.contains(box.text)) {
                parts.add(box.text);
            }
        }

        if (parts.isEmpty()) {
            return "";
        }

        if (parts.size() == 1) {
            return parts.get(0);
        }

        return parts.get(0) + "|" + parts.get(1);

    }

    public static boolean hasProductTray() throws Exception {

        DocumentBuilder builder =
                DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder();

        Document document =
                builder.parse(new File("ui.xml"));

        NodeList nodes =
                document.getElementsByTagName("node");

        for (int i = 0; i < nodes.getLength(); i++) {

            Element node = (Element) nodes.item(i);

            if ("product_tray_product_brand".equals(
                    node.getAttribute("content-desc"))) {
                return true;
            }
        }

        return false;

    }

    public static boolean hasVisibleText(String expectedText) throws Exception {

        if (expectedText == null || expectedText.isBlank()) {
            return false;
        }

        DocumentBuilder builder =
                DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder();

        Document document =
                builder.parse(new File("ui.xml"));

        NodeList nodes =
                document.getElementsByTagName("node");

        for (int i = 0; i < nodes.getLength(); i++) {

            Element node = (Element) nodes.item(i);
            String text = node.getAttribute("text");

            if (text == null) {
                continue;
            }

            if (expectedText.equalsIgnoreCase(text.trim())) {
                return true;
            }
        }

        return false;

    }

    public static int parseProfilePostCount() throws Exception {

        DocumentBuilder builder =
                DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder();

        Document document =
                builder.parse(new File("ui.xml"));

        NodeList nodes =
                document.getElementsByTagName("node");

        Pattern pattern =
                Pattern.compile("\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]");

        for (int i = 0; i < nodes.getLength(); i++) {

            Element node = (Element) nodes.item(i);
            String text = node.getAttribute("text");
            if (text == null) {
                continue;
            }

            String trimmed = text.trim();
            if (!trimmed.matches("\\d+")) {
                continue;
            }

            String bounds = node.getAttribute("bounds");
            Matcher matcher = pattern.matcher(bounds);
            if (!matcher.find()) {
                continue;
            }

            int x1 = Integer.parseInt(matcher.group(1));
            int y1 = Integer.parseInt(matcher.group(2));
            int x2 = Integer.parseInt(matcher.group(3));
            int y2 = Integer.parseInt(matcher.group(4));

            if (y1 >= 380 && y2 <= 460 && x1 >= 1050 && x2 <= 1205) {
                return Integer.parseInt(trimmed);
            }
        }

        return -1;

    }

    private static Element findClickableAncestor(Element node) {

        Node current = node;

        while (current != null && current.getNodeType() == Node.ELEMENT_NODE) {

            Element element = (Element) current;

            if ("true".equals(element.getAttribute("clickable"))) {
                return element;
            }

            current = current.getParentNode();
        }

        return null;

    }

    private static String findDescendantText(Element root, String contentDesc) {

        if (root == null) {
            return "";
        }

        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {

            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element element = (Element) child;
            if (contentDesc.equals(element.getAttribute("content-desc"))) {
                String text = element.getAttribute("text");
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }

            String nested = findDescendantText(element, contentDesc);
            if (!nested.isBlank()) {
                return nested;
            }
        }

        return "";

    }

    private static boolean isNoiseText(String text) {

        return "Following".equals(text) ||
                "Posts".equals(text) ||
                "Share Profile".equals(text) ||
                "Spotlight Profile".equals(text) ||
                "Shop the Look".equals(text) ||
                "0 Views".equals(text) ||
                "0".equals(text) ||
                "3".equals(text) ||
                "Shop All".equals(text) ||
                "Shop ALL".equals(text);

    }

    private static class TextBox {

        private final String text;
        private final int x;
        private final int y;

        private TextBox(String text, int x, int y) {
            this.text = text;
            this.x = x;
            this.y = y;
        }

    }

}
