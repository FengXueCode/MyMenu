package com.fengxue.menu.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fengxue.menu.common.ResultCode;
import com.fengxue.menu.common.exception.BusinessException;
import com.fengxue.menu.config.JwtUtil;
import com.fengxue.menu.mapper.UserMapper;
import com.fengxue.menu.preference.PreferenceService;
import com.fengxue.menu.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PreferenceService preferenceService;

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        Long exists = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, request.username()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(defaultNickname(request.nickname(), request.username()));
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        preferenceService.createDefault(user.getId());
        return issue(user);
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
        return issue(user);
    }

    public AuthDtos.UserVO me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return toVo(user);
    }

    private AuthDtos.AuthResponse issue(User user) {
        String token = jwtUtil.createToken(user.getId());
        return new AuthDtos.AuthResponse(token, toVo(user));
    }

    private AuthDtos.UserVO toVo(User user) {
        return new AuthDtos.UserVO(user.getId(), user.getUsername(), user.getNickname(), user.getAvatar());
    }

    private String defaultNickname(String nickname, String username) {
        if (nickname != null && !nickname.isBlank()) {
            return nickname.trim();
        }
        return "厨房新手_" + username;
    }
}
