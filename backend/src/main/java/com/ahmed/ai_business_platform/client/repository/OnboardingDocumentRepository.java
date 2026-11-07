package com.ahmed.ai_business_platform.client.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ahmed.ai_business_platform.client.entity.OnboardingDocument;

public interface OnboardingDocumentRepository extends JpaRepository<OnboardingDocument, String> {

	boolean existsByClient_IdAndNameIgnoreCase(String clientId, String name);

	Optional<OnboardingDocument> findByIdAndClient_Id(String id, String clientId);
}
