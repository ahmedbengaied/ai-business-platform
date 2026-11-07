package com.ahmed.ai_business_platform.client.dto;

import java.time.Instant;
import java.util.List;

import com.ahmed.ai_business_platform.client.entity.AccountingClient;
import com.ahmed.ai_business_platform.client.entity.OnboardingDocumentStatus;

public record AccountingClientResponse(
		String id,
		String legalName,
		String contactName,
		String contactEmail,
		String contactPhone,
		String onboardingStatus,
		int documentCount,
		int approvedDocumentCount,
		List<OnboardingDocumentResponse> documents,
		Instant createdAt) {

	public static AccountingClientResponse from(AccountingClient client) {
		List<OnboardingDocumentResponse> documents = client.getDocuments().stream()
				.sorted((left, right) -> left.getCreatedAt().compareTo(right.getCreatedAt()))
				.map(OnboardingDocumentResponse::from)
				.toList();
		long approvedCount = documents.stream()
				.filter(document -> document.status() == OnboardingDocumentStatus.APPROVED)
				.count();
		String onboardingStatus;
		if (!documents.isEmpty() && approvedCount == documents.size()) {
			onboardingStatus = "COMPLETE";
		} else if (documents.stream().allMatch(document ->
				document.status() == OnboardingDocumentStatus.REQUESTED)) {
			onboardingStatus = "NOT_STARTED";
		} else {
			onboardingStatus = "IN_PROGRESS";
		}
		return new AccountingClientResponse(
				client.getId(),
				client.getLegalName(),
				client.getContactName(),
				client.getContactEmail(),
				client.getContactPhone(),
				onboardingStatus,
				documents.size(),
				(int) approvedCount,
				documents,
				client.getCreatedAt());
	}
}
