package com.openqualityhub.service;

import com.openqualityhub.dto.request.LoginRequest;
import com.openqualityhub.dto.request.RegisterRequest;
import com.openqualityhub.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);
}
