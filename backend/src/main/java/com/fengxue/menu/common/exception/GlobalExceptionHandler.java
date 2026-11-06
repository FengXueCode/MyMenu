package com.fengxue.menu.common.exception;

import com.fengxue.menu.common.Result;
import com.fengxue.menu.common.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * <p>统一把异常转换为 {@link Result}，并尽量返回与业务码语义一致的 HTTP 状态码。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return ResponseEntity.status(resolveHttpStatus(e.getCode()))
                .body(Result.failure(e.getCode(), e.getMessage()));
    }

    /**
     * @RequestBody 参数校验失败。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        return buildValidationResponse(e.getBindingResult());
    }

    /**
     * 查询参数对象（@ModelAttribute）绑定校验失败。
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        return buildValidationResponse(e.getBindingResult());
    }

    /**
     * 方法参数（@RequestParam / @PathVariable）校验失败。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(this::formatConstraintViolation)
                .distinct()
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return ResponseEntity.badRequest().body(Result.failure(ResultCode.PARAM_ERROR.getCode(), message));
    }

    /**
     * 缺少请求参数。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParameter(MissingServletRequestParameterException e) {
        String message = "缺少请求参数: " + e.getParameterName();
        log.warn(message);
        return ResponseEntity.badRequest().body(Result.failure(ResultCode.PARAM_ERROR.getCode(), message));
    }

    /**
     * 参数类型转换失败。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "参数类型不正确: " + e.getName();
        log.warn("{}", message);
        return ResponseEntity.badRequest().body(Result.failure(ResultCode.PARAM_ERROR.getCode(), message));
    }

    /**
     * 请求体 JSON 无法解析。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(Result.failure(ResultCode.PARAM_ERROR.getCode(), "请求体格式不正确"));
    }

    /**
     * 请求方法不支持。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.failure(ResultCode.METHOD_NOT_ALLOWED));
    }

    /**
     * 唯一索引冲突。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一约束冲突: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Result.failure(ResultCode.CONFLICT.getCode(), "数据已存在，请勿重复提交"));
    }

    /**
     * 其他数据库异常。
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Result<Void>> handleDataAccess(DataAccessException e) {
        log.error("数据库操作异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.failure(ResultCode.DATABASE_ERROR));
    }

    /**
     * 静态资源 / 未知路径 404。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResource(NoResourceFoundException e) {
        log.warn("资源不存在: {} {}", e.getHttpMethod(), e.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.failure(ResultCode.NOT_FOUND));
    }

    /**
     * 兜底异常，避免把堆栈信息直接暴露给调用方。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.failure(ResultCode.INTERNAL_ERROR));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<Result<Void>> buildValidationResponse(BindingResult bindingResult) {
        String message = bindingResult.getFieldErrors().stream()
                .map(this::formatFieldError)
                .distinct()
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = ResultCode.PARAM_ERROR.getMessage();
        }
        log.warn("参数校验失败: {}", message);
        return ResponseEntity.badRequest()
                .body(Result.failure(ResultCode.PARAM_ERROR.getCode(), message));
    }

    private String formatConstraintViolation(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + ": " + violation.getMessage();
    }

    private HttpStatus resolveHttpStatus(int code) {
        int httpCode = code / 100;
        return switch (httpCode) {
            case 400, 401, 403, 404, 405, 409, 429 -> HttpStatus.valueOf(httpCode);
            case 503 -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> code >= 500 ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST;
        };
    }
}
