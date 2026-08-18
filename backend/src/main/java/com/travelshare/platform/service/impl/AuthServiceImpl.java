package com.travelshare.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelshare.platform.dto.LoginRequest;
import com.travelshare.platform.dto.RegisterRequest;
import com.travelshare.platform.entity.User;
import com.travelshare.platform.exception.BusinessException;
import com.travelshare.platform.mapper.UserMapper;
import com.travelshare.platform.security.JwtService;
import com.travelshare.platform.service.AuthService;
import com.travelshare.platform.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public LoginVO login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw BusinessException.unauthorized("账号或密码错误");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) throw BusinessException.forbidden("账号已被停用");
        return toLogin(user);
    }

    @Override
    @Transactional
    public LoginVO register(RegisterRequest request) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, request.username())) > 0) {
            throw BusinessException.badRequest("用户名已被使用");
        }
        if (request.email() != null && !request.email().isBlank() &&
                userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, request.email())) > 0) {
            throw BusinessException.badRequest("邮箱已被使用");
        }
        User user = new User();
        user.setUsername(request.username()); user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname()); user.setEmail(request.email());
        user.setAvatar("/uploads/demo/avatars/avatar-001.png"); user.setRole("USER"); user.setStatus(1);
        user.setVisitedCities(0); user.setGuideCount(0); user.setRouteCount(0); user.setFollowerCount(0);
        user.setFollowingCount(0); user.setReceivedLikes(0); user.setDeleted(0);
        user.setCreateTime(LocalDateTime.now()); user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return toLogin(user);
    }

    private LoginVO toLogin(User user) {
        return new LoginVO(jwtService.generate(user.getUsername(), user.getRole(), user.getId()), user.getId(),
                user.getUsername(), user.getNickname(), user.getAvatar(), user.getRole());
    }
}

