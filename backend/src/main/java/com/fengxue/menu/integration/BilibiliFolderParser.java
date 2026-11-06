package com.fengxue.menu.integration;

/**
 * B 站收藏夹链接解析：支持 …/favlist?fid=123、media_id=123 或纯数字。
 */
public final class BilibiliFolderParser {

    private BilibiliFolderParser() {
    }

    public static String parseMediaId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim();
        if (text.matches("\\d{1,20}")) {
            return text;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?:fid|media_id)=(\\d{1,20})")
                .matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static boolean isBilibiliFolderUrl(String raw) {
        return raw != null && (raw.contains("bilibili.com") || raw.matches("\\d{1,20}"));
    }
}
