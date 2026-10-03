package com.b2c.flash_sale_b2c_UTC2.auth.service;

import com.b2c.flash_sale_b2c_UTC2.auth.dto.AuthResponse;
import com.b2c.flash_sale_b2c_UTC2.auth.dto.LoginRequest;
import com.b2c.flash_sale_b2c_UTC2.auth.dto.RefreshTokenRequest;
import com.b2c.flash_sale_b2c_UTC2.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    void logout();
}
