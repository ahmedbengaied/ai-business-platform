package com.ahmed.ai_business_platform.firm.dto;

import java.time.Instant;

public record FirmInvitationPreviewResponse(
		String firmName,
		String email,
		String role,
		Instant expiresAt) {
}
