package com.travelshare.platform.service;
import com.travelshare.platform.dto.LoginRequest;
import com.travelshare.platform.dto.RegisterRequest;
import com.travelshare.platform.vo.LoginVO;
public interface AuthService {
    LoginVO login(LoginRequest request);
    LoginVO register(RegisterRequest request);
}

