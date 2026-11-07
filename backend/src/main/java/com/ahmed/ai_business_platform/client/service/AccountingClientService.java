package com.ahmed.ai_business_platform.client.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ahmed.ai_business_platform.client.dto.AccountingClientResponse;
import com.ahmed.ai_business_platform.client.dto.AddOnboardingDocumentRequest;
import com.ahmed.ai_business_platform.client.dto.CreateAccountingClientRequest;
import com.ahmed.ai_business_platform.client.dto.OnboardingDocumentResponse;
import com.ahmed.ai_business_platform.client.dto.UpdateOnboardingDocumentStatusRequest;
import com.ahmed.ai_business_platform.client.entity.AccountingClient;
import com.ahmed.ai_business_platform.client.entity.OnboardingDocument;
import com.ahmed.ai_business_platform.client.entity.OnboardingDocumentStatus;
import com.ahmed.ai_business_platform.client.entity.OnboardingTemplate;
import com.ahmed.ai_business_platform.client.repository.AccountingClientRepository;
import com.ahmed.ai_business_platform.client.repository.OnboardingDocumentRepository;
import com.ahmed.ai_business_platform.client.repository.OnboardingTemplateRepository;
import com.ahmed.ai_business_platform.firm.service.FirmAccessService;

@Service
@Transactional(readOnly = true)
public class AccountingClientService {

	private static final List<String> STANDARD_DOCUMENTS = List.of(
			"Business registration certificate",
			"Tax identification document",
			"Proof of business address",
			"Director identification",
			"Previous financial statements");

	private final AccountingClientRepository clientRepository;
	private final OnboardingDocumentRepository documentRepository;
	private final OnboardingTemplateRepository templateRepository;
	private final FirmAccessService firmAccess;

	public AccountingClientService(
			AccountingClientRepository clientRepository,
			OnboardingDocumentRepository documentRepository,
			OnboardingTemplateRepository templateRepository,
			FirmAccessService firmAccess) {
		this.clientRepository = clientRepository;
		this.documentRepository = documentRepository;
		this.templateRepository = templateRepository;
		this.firmAccess = firmAccess;
	}

	@Transactional
	public AccountingClientResponse create(String email, CreateAccountingClientRequest request) {
		var firmUser = firmAccess.requireCanWrite(email);
		AccountingClient client = new AccountingClient(
				request.legalName().trim(),
				request.contactName().trim(),
				trimToNull(request.contactEmail()),
				trimToNull(request.contactPhone()),
				firmUser.getFirm());
		List<String> documentNames = request.templateId() == null || request.templateId().isBlank()
				? STANDARD_DOCUMENTS
				: templateRepository.findByIdAndFirm_Id(request.templateId(), firmUser.getFirm().getId())
						.map(OnboardingTemplate::getItems)
						.map(items -> items.stream().map(item -> item.getName()).toList())
						.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		documentNames.forEach(name -> client.addDocument(new OnboardingDocument(name)));
		return AccountingClientResponse.from(clientRepository.save(client));
	}

	public List<AccountingClientResponse> list(String email) {
		String firmId = firmAccess.requireUser(email).getFirm().getId();
		return clientRepository.findAllByFirm_IdOrderByCreatedAtDesc(firmId).stream()
				.map(AccountingClientResponse::from)
				.toList();
	}

	public AccountingClientResponse get(String email, String clientId) {
		return AccountingClientResponse.from(requireOwnedClient(clientId, email));
	}

	@Transactional
	public OnboardingDocumentResponse addDocument(
			String email,
			String clientId,
			AddOnboardingDocumentRequest request) {
		firmAccess.requireCanWrite(email);
		AccountingClient client = requireOwnedClient(clientId, email);
		String name = request.name().trim();
		if (documentRepository.existsByClient_IdAndNameIgnoreCase(clientId, name)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This document is already on the checklist");
		}
		OnboardingDocument document = new OnboardingDocument(name);
		client.addDocument(document);
		documentRepository.saveAndFlush(document);
		return OnboardingDocumentResponse.from(document);
	}

	@Transactional
	public AccountingClientResponse updateDocumentStatus(
			String email,
			String clientId,
			String documentId,
			UpdateOnboardingDocumentStatusRequest request) {
		firmAccess.requireCanWrite(email);
		AccountingClient client = requireOwnedClient(clientId, email);
		OnboardingDocument document = documentRepository.findByIdAndClient_Id(documentId, clientId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		document.setStatus(request.status());
		return AccountingClientResponse.from(client);
	}

	private AccountingClient requireOwnedClient(String clientId, String email) {
		String firmId = firmAccess.requireUser(email).getFirm().getId();
		return clientRepository.findByIdAndFirm_Id(clientId, firmId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	private String trimToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
