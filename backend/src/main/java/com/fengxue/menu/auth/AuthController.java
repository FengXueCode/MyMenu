package com.fengxue.menu.auth;

import com.fengxue.menu.common.Result;
import com.fengxue.menu.common.util.CurrentUser;
import com.fengxue.menu.common.util.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RateLimiter rateLimiter;

    @PostMapping("/register")
    public Result<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request,
                                                  HttpServletRequest httpRequest) {
        rateLimiter.check("register:" + clientIp(httpRequest), 5, Duration.ofMinutes(1));
        return Result.success(authService.register(request));
    }

    @PostMapping("/login")
    public Result<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request,
                                               HttpServletRequest httpRequest) {
        rateLimiter.check("login:" + clientIp(httpRequest), 10, Duration.ofMinutes(1));
        return Result.success(authService.login(request));
    }

    @GetMapping("/me")
    public Result<AuthDtos.UserVO> me() {
        return Result.success(authService.me(CurrentUser.require()));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
