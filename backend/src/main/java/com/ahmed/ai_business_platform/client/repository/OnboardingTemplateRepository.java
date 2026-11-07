package com.ahmed.ai_business_platform.client.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ahmed.ai_business_platform.client.entity.OnboardingTemplate;

public interface OnboardingTemplateRepository extends JpaRepository<OnboardingTemplate, String> {

	@EntityGraph(attributePaths = "items")
	List<OnboardingTemplate> findAllByFirm_IdOrderByNameAsc(String firmId);

	@EntityGraph(attributePaths = "items")
	Optional<OnboardingTemplate> findByIdAndFirm_Id(String id, String firmId);

	boolean existsByFirm_IdAndNameIgnoreCase(String firmId, String name);

	boolean existsByFirm_IdAndNameIgnoreCaseAndIdNot(String firmId, String name, String id);
}
