package com.ahmed.ai_business_platform.client.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ahmed.ai_business_platform.client.dto.AccountingClientResponse;
import com.ahmed.ai_business_platform.client.dto.AddOnboardingDocumentRequest;
import com.ahmed.ai_business_platform.client.dto.CreateAccountingClientRequest;
import com.ahmed.ai_business_platform.client.dto.OnboardingDocumentResponse;
import com.ahmed.ai_business_platform.client.dto.UpdateOnboardingDocumentStatusRequest;
import com.ahmed.ai_business_platform.client.service.AccountingClientService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/clients")
@Validated
public class AccountingClientController {

	private final AccountingClientService clientService;

	public AccountingClientController(AccountingClientService clientService) {
		this.clientService = clientService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AccountingClientResponse create(
			Authentication authentication,
			@Valid @RequestBody CreateAccountingClientRequest request) {
		return clientService.create(authentication.getName(), request);
	}

	@GetMapping
	public List<AccountingClientResponse> list(Authentication authentication) {
		return clientService.list(authentication.getName());
	}

	@GetMapping("/{clientId}")
	public AccountingClientResponse get(Authentication authentication, @PathVariable String clientId) {
		return clientService.get(authentication.getName(), clientId);
	}

	@PostMapping("/{clientId}/documents")
	@ResponseStatus(HttpStatus.CREATED)
	public OnboardingDocumentResponse addDocument(
			Authentication authentication,
			@PathVariable String clientId,
			@Valid @RequestBody AddOnboardingDocumentRequest request) {
		return clientService.addDocument(authentication.getName(), clientId, request);
	}

	@PatchMapping("/{clientId}/documents/{documentId}")
	public AccountingClientResponse updateDocumentStatus(
			Authentication authentication,
			@PathVariable String clientId,
			@PathVariable String documentId,
			@Valid @RequestBody UpdateOnboardingDocumentStatusRequest request) {
		return clientService.updateDocumentStatus(
				authentication.getName(), clientId, documentId, request);
	}
}
