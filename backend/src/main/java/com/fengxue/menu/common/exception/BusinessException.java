package com.fengxue.menu.common.exception;

import com.fengxue.menu.common.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 自定义业务异常。
 *
 * <p>业务代码中主动抛出该异常，由 {@link GlobalExceptionHandler} 统一转换为返回体。</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务状态码。
     */
    private final int code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
