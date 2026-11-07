package com.ahmed.ai_business_platform.firm.dto;

import java.util.List;

public record FirmTeamResponse(
		String firmName,
		String currentRole,
		List<FirmMemberResponse> members,
		List<FirmInvitationResponse> invitations) {
}
