package com.ahmed.ai_business_platform.client.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ahmed.ai_business_platform.client.dto.OnboardingTemplateResponse;
import com.ahmed.ai_business_platform.client.dto.SaveOnboardingTemplateRequest;
import com.ahmed.ai_business_platform.client.service.OnboardingTemplateService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/onboarding-templates")
@Validated
public class OnboardingTemplateController {

	private final OnboardingTemplateService templateService;

	public OnboardingTemplateController(OnboardingTemplateService templateService) {
		this.templateService = templateService;
	}

	@GetMapping
	public List<OnboardingTemplateResponse> list(Authentication authentication) {
		return templateService.list(authentication.getName());
	}

	@GetMapping("/{templateId}")
	public OnboardingTemplateResponse get(
			Authentication authentication,
			@PathVariable String templateId) {
		return templateService.get(authentication.getName(), templateId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OnboardingTemplateResponse create(
			Authentication authentication,
			@Valid @RequestBody SaveOnboardingTemplateRequest request) {
		return templateService.create(authentication.getName(), request);
	}

	@PutMapping("/{templateId}")
	public OnboardingTemplateResponse update(
			Authentication authentication,
			@PathVariable String templateId,
			@Valid @RequestBody SaveOnboardingTemplateRequest request) {
		return templateService.update(authentication.getName(), templateId, request);
	}

	@DeleteMapping("/{templateId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(Authentication authentication, @PathVariable String templateId) {
		templateService.delete(authentication.getName(), templateId);
	}
}
