package com.ahmed.ai_business_platform.client.dto;

import java.time.Instant;
import java.util.List;

import com.ahmed.ai_business_platform.client.entity.OnboardingTemplate;

public record OnboardingTemplateResponse(
		String id,
		String name,
		List<String> documentNames,
		Instant createdAt) {

	public static OnboardingTemplateResponse from(OnboardingTemplate template) {
		return new OnboardingTemplateResponse(
				template.getId(),
				template.getName(),
				template.getItems().stream().map(item -> item.getName()).toList(),
				template.getCreatedAt());
	}
}
