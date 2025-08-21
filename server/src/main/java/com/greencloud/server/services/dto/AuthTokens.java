package com.greencloud.server.services.dto;

public record AuthTokens(String accessToken, String refreshToken, String tokenType) {
    public static AuthTokens bearer(String at, String rt){ return new AuthTokens(at, rt, "Bearer"); }
}
