package com.ahmed.ai_business_platform.firm.dto;

import com.ahmed.ai_business_platform.firm.entity.FirmRole;

import jakarta.validation.constraints.NotNull;

public record UpdateFirmMemberRoleRequest(@NotNull FirmRole role) {
}
