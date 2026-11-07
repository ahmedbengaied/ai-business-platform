package com.ahmed.ai_business_platform.firm.dto;

import java.time.Instant;

public record CreatedFirmInvitationResponse(
		String id,
		String email,
		String role,
		String token,
		Instant expiresAt) {
}
