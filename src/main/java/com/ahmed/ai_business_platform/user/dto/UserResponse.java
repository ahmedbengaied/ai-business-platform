package com.ahmed.ai_business_platform.user.dto;

import com.ahmed.ai_business_platform.user.entity.User;

public record UserResponse(
		String id,
		String email,
		String firstName,
		String lastName,
		String role) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getFirstName(),
				user.getLastName(),
				user.getRole().name());
	}
}
