package com.fengxue.menu.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一业务状态码。
 *
 * <p>编码规则：HTTP 语义码 * 100 + 业务序号，例如 40401 表示用户不存在。</p>
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),

    PARAM_ERROR(400, "参数校验失败"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    CONFLICT(409, "资源冲突"),
    RATE_LIMITED(429, "操作太频繁，请稍后再试"),

    INTERNAL_ERROR(500, "服务器内部错误"),
    DATABASE_ERROR(50001, "数据库操作失败"),
    AI_CALL_FAILED(50201, "AI 服务调用失败"),
    SERVICE_UNAVAILABLE(503, "依赖服务不可用"),

    USER_NOT_FOUND(40401, "用户不存在"),
    USERNAME_EXISTS(40901, "用户名已存在"),
    USER_UPDATE_CONFLICT(40902, "用户信息已被修改，请刷新后重试"),
    CATEGORY_NOT_FOUND(40402, "分类不存在"),
    CATEGORY_IN_USE(40903, "该分类下还有菜谱，无法删除"),
    RECIPE_NOT_FOUND(40403, "菜谱不存在"),
    RECIPE_FORBIDDEN(40301, "只能修改自己的菜谱"),
    PROVIDER_NOT_FOUND(40404, "AI 配置不存在"),
    AI_NOT_CONFIGURED(40010, "请先在设置中配置 AI 模型"),
    IMPORT_FAILED(40011, "链接解析失败"),
    FILE_INVALID(40012, "文件不合法");

    private final int code;
    private final String message;
}
