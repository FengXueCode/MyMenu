package com.fengxue.menu.config;

import com.fengxue.menu.common.Result;
import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.util.CurrentUser;
import com.fengxue.menu.common.util.JsonSupport;
import com.fengxue.menu.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * JWT 鉴权拦截器：只保护 /api/** 下的业务接口，静态资源与登录注册放行。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeUnauthorized(response);
            return false;
        }
        try {
            Long userId = jwtUtil.parseUserId(authorization.substring(7).trim());
            CurrentUser.set(userId);
            return true;
        } catch (BusinessException e) {
            writeUnauthorized(response);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        CurrentUser.clear();
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JsonSupport.toJson(
                Result.failure(ResultCode.UNAUTHORIZED.getCode(), "登录已过期，请重新登录")));
    }
}
