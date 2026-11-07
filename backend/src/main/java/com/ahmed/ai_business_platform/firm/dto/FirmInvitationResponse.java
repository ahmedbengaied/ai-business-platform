package com.ahmed.ai_business_platform.firm.dto;

import java.time.Instant;

import com.ahmed.ai_business_platform.firm.entity.FirmInvitation;

public record FirmInvitationResponse(
		String id,
		String email,
		String role,
		Instant createdAt,
		Instant expiresAt) {

	public static FirmInvitationResponse from(FirmInvitation invitation) {
		return new FirmInvitationResponse(
				invitation.getId(),
				invitation.getEmail(),
				invitation.getRole().name(),
				invitation.getCreatedAt(),
				invitation.getExpiresAt());
	}
}
