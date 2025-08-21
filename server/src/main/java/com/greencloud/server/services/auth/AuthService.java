package com.greencloud.server.services.auth;

import com.greencloud.server.dto.requests.SignInRequest;
import com.greencloud.server.dto.requests.SignUpRequest;
import com.greencloud.server.services.dto.AuthTokens;

public interface AuthService {
    void signUp(SignUpRequest req);
    AuthTokens signIn(SignInRequest req);
    AuthTokens refresh(String refreshToken);
    void logout(String accessToken, String refreshToken);
    boolean isEmailAvailable(String email);
}