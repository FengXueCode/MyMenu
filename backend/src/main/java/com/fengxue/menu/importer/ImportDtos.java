package com.fengxue.menu.importer;

import jakarta.validation.constraints.Size;

/**
 * 视频导入 DTO。
 */
public final class ImportDtos {

    private ImportDtos() {
    }

    public record ResolveRequest(
            @Size(max = 1000, message = "链接过长") String url,
            @Size(max = 3000, message = "分享文案过长") String shareText,
            @Size(max = 8000, message = "补充文案过长") String extraText) {
    }
}
