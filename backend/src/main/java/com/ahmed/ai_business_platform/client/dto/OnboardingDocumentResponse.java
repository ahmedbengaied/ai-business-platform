package com.ahmed.ai_business_platform.client.dto;

import java.time.Instant;

import com.ahmed.ai_business_platform.client.entity.OnboardingDocument;
import com.ahmed.ai_business_platform.client.entity.OnboardingDocumentStatus;

public record OnboardingDocumentResponse(
		String id,
		String name,
		OnboardingDocumentStatus status,
		Instant createdAt) {

	public static OnboardingDocumentResponse from(OnboardingDocument document) {
		return new OnboardingDocumentResponse(
				document.getId(),
				document.getName(),
				document.getStatus(),
				document.getCreatedAt());
	}
}
