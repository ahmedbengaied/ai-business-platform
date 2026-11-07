package com.ahmed.ai_business_platform.client.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record SaveOnboardingTemplateRequest(
		@NotBlank @Size(max = 150) String name,
		@NotEmpty @Size(max = 50) List<@NotBlank @Size(max = 200) String> documentNames) {
}
