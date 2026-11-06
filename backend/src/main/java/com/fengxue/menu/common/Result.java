package com.fengxue.menu.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一返回体。
 *
 * <p>约定：HTTP 状态码表达传输层结果，{@code code} 表达业务结果；
 * 前端判断成功与否时优先判断 {@code code == 200}。</p>
 *
 * @param <T> 业务数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一返回体")
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "业务状态码，200 表示成功", example = "200")
    private int code;

    @Schema(description = "提示信息", example = "操作成功")
    private String message;

    @Schema(description = "业务数据")
    private T data;

    @Schema(description = "服务端时间戳（毫秒）", example = "1762310400000")
    private long timestamp;

    /**
     * 成功（无业务数据）。
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 成功（带业务数据）。
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(),
                data, System.currentTimeMillis());
    }

    /**
     * 失败（使用预定义状态码）。
     */
    public static <T> Result<T> failure(ResultCode resultCode) {
        return failure(resultCode.getCode(), resultCode.getMessage());
    }

    /**
     * 失败（自定义状态码与提示）。
     */
    public static <T> Result<T> failure(int code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }
}
