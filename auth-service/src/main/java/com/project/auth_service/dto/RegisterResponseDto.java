package com.project.auth_service.dto;

public class RegisterResponseDto {
    private String message;
    private String accessToken;
    private String refreshToken;
    private long expiresIn;

    public RegisterResponseDto(String message, String accessToken, String refreshToken, long expiresIn) {
        this.message = message;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
}
