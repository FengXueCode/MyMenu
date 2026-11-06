package com.fengxue.menu.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BilibiliFolderParserTest {

    @Test
    void parsesFidFromFolderUrl() {
        assertEquals("123456",
                BilibiliFolderParser.parseMediaId("https://space.bilibili.com/1/favlist?fid=123456&ctype=21"));
        assertEquals("789", BilibiliFolderParser.parseMediaId("https://space.bilibili.com/1/favlist?media_id=789"));
    }

    @Test
    void acceptsNumericFolderId() {
        assertEquals("42", BilibiliFolderParser.parseMediaId("42"));
    }

    @Test
    void rejectsInvalidInput() {
        assertNull(BilibiliFolderParser.parseMediaId("https://space.bilibili.com/1/favlist"));
        assertNull(BilibiliFolderParser.parseMediaId(""));
        assertNull(BilibiliFolderParser.parseMediaId(null));
    }
}
