package com.ahmed.ai_business_platform.client.dto;

import com.ahmed.ai_business_platform.client.entity.OnboardingDocumentStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateOnboardingDocumentStatusRequest(
		@NotNull OnboardingDocumentStatus status) {
}
