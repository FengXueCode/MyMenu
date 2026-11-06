package com.fengxue.menu.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 认证相关请求/响应 DTO。
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "用户名不能为空")
            @Pattern(regexp = "[A-Za-z0-9_]{3,20}", message = "用户名需为 3-20 位字母、数字或下划线")
            String username,
            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
            String password,
            @Size(max = 32, message = "昵称最多 32 字")
            String nickname) {
    }

    public record LoginRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password) {
    }

    public record UserVO(Long id, String username, String nickname, String avatar) {
    }

    public record AuthResponse(String token, UserVO user) {
    }
}
