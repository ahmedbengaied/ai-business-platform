package com.ahmed.ai_business_platform.firm.dto;

import java.time.Instant;

import com.ahmed.ai_business_platform.user.entity.User;

public record FirmMemberResponse(
		String id,
		String email,
		String firstName,
		String lastName,
		String role,
		Instant joinedAt) {

	public static FirmMemberResponse from(User user) {
		return new FirmMemberResponse(
				user.getId(),
				user.getEmail(),
				user.getFirstName(),
				user.getLastName(),
				user.getFirmRole().name(),
				user.getCreatedAt());
	}
}
