package com.fengxue.menu.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 分页返回体。
 *
 * @param <T> 列表元素类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页结果")
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页数据")
    private List<T> records;

    @Schema(description = "总记录数", example = "100")
    private long total;

    @Schema(description = "当前页码，从 1 开始", example = "1")
    private long pageNum;

    @Schema(description = "每页条数", example = "10")
    private long pageSize;

    @Schema(description = "总页数", example = "10")
    private long pages;

    public static <T> PageResult<T> of(List<T> records, long total, long pageNum, long pageSize) {
        long pages = pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageResult<>(records, total, pageNum, pageSize, pages);
    }
}
