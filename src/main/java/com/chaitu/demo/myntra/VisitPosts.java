package com.chaitu.demo.myntra;

import java.util.ArrayList;
import java.util.List;

public class VisitPosts {
    private static final String ADB =
            "C:\\Program Files\\BlueStacks_nxt\\HD-Adb.exe";
        private static  String url = "https://www.myntra.com/studio/studiopost/{postId}?utm_source=ugc_affiliate&utm_medium=social_share_ugc_post&utm_campaign=tFLOgMr19O&affiliate_id=tFLOgMr19O&share=true";

    static List<String> postsId = new ArrayList<>(List.of("15369835", "15335771", "15536241", "15370432", "15526120", "15525808", "15525412", "15524436", "15464154",
            "15462937", "15461809", "15461587", "15461158", "15460626", "15460581",
            "15459955", "15459744", "15459501", "15459198", "15458853", "15434944", "15406827",
            "15404740", "15403443", "15400107", "15399510", "15399322", "15399134", "15398524",
            "15398427", "15398264", "15344389", "14111585", "14086373", "14085805", "14083709",
            "9622041", "9621997", "6650396"));

    public  static void visitPosts() {
        int opened=0;

            for (String postId : postsId) {
                String postUrl = url.replace("{postId}", postId);
                try {
                    Thread.sleep(1000);
                    AdbUtils.openLink(postUrl);
                    Thread.sleep(1000);
                    System.out.println(opened++);// Wait for 2 seconds before opening the next post
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

    }
}
