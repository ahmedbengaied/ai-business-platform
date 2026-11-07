package com.ahmed.ai_business_platform.firm.dto;

import com.ahmed.ai_business_platform.firm.entity.FirmRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFirmInvitationRequest(
		@NotBlank @Email @Size(max = 320) String email,
		@NotNull FirmRole role) {
}
