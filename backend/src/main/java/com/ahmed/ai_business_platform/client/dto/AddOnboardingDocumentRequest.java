package com.ahmed.ai_business_platform.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddOnboardingDocumentRequest(
		@NotBlank @Size(max = 200) String name) {
}
