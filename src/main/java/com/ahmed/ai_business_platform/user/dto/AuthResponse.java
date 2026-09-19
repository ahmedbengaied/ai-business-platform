package com.ahmed.ai_business_platform.user.dto;

public record AuthResponse(
		String token,
		String tokenType,
		long expiresIn,
		UserResponse user) {
}
