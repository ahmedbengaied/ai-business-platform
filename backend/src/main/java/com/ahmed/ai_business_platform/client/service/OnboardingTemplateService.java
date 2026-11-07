package com.ahmed.ai_business_platform.client.service;

import java.util.HashSet;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ahmed.ai_business_platform.client.dto.OnboardingTemplateResponse;
import com.ahmed.ai_business_platform.client.dto.SaveOnboardingTemplateRequest;
import com.ahmed.ai_business_platform.client.entity.OnboardingTemplate;
import com.ahmed.ai_business_platform.client.repository.OnboardingTemplateRepository;
import com.ahmed.ai_business_platform.firm.service.FirmAccessService;

@Service
@Transactional(readOnly = true)
public class OnboardingTemplateService {

	private final OnboardingTemplateRepository templateRepository;
	private final FirmAccessService firmAccess;

	public OnboardingTemplateService(
			OnboardingTemplateRepository templateRepository,
			FirmAccessService firmAccess) {
		this.templateRepository = templateRepository;
		this.firmAccess = firmAccess;
	}

	public List<OnboardingTemplateResponse> list(String email) {
		String firmId = firmAccess.requireUser(email).getFirm().getId();
		return templateRepository.findAllByFirm_IdOrderByNameAsc(firmId).stream()
				.map(OnboardingTemplateResponse::from)
				.toList();
	}

	public OnboardingTemplateResponse get(String email, String templateId) {
		String firmId = firmAccess.requireUser(email).getFirm().getId();
		return OnboardingTemplateResponse.from(requireFirmTemplate(templateId, firmId));
	}

	@Transactional
	public OnboardingTemplateResponse create(String email, SaveOnboardingTemplateRequest request) {
		var user = firmAccess.requireCanWrite(email);
		String firmId = user.getFirm().getId();
		String name = request.name().trim();
		validateItems(request.documentNames());
		if (templateRepository.existsByFirm_IdAndNameIgnoreCase(firmId, name)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A template with this name already exists");
		}
		OnboardingTemplate template = new OnboardingTemplate(name, user.getFirm());
		template.replaceItems(normalizeItems(request.documentNames()));
		return OnboardingTemplateResponse.from(templateRepository.save(template));
	}

	@Transactional
	public OnboardingTemplateResponse update(
			String email,
			String templateId,
			SaveOnboardingTemplateRequest request) {
		var user = firmAccess.requireCanWrite(email);
		String firmId = user.getFirm().getId();
		OnboardingTemplate template = requireFirmTemplate(templateId, firmId);
		String name = request.name().trim();
		validateItems(request.documentNames());
		if (templateRepository.existsByFirm_IdAndNameIgnoreCaseAndIdNot(firmId, name, templateId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A template with this name already exists");
		}
		template.setName(name);
		template.replaceItems(normalizeItems(request.documentNames()));
		return OnboardingTemplateResponse.from(template);
	}

	@Transactional
	public void delete(String email, String templateId) {
		var user = firmAccess.requireCanWrite(email);
		templateRepository.delete(requireFirmTemplate(templateId, user.getFirm().getId()));
	}

	private OnboardingTemplate requireFirmTemplate(String templateId, String firmId) {
		return templateRepository.findByIdAndFirm_Id(templateId, firmId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	private void validateItems(List<String> names) {
		List<String> normalizedNames = normalizeItems(names);
		if (normalizedNames.stream().anyMatch(String::isBlank)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document names cannot be blank");
		}
		if (new HashSet<>(normalizedNames.stream().map(String::toLowerCase).toList()).size()
				!= normalizedNames.size()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document names must be unique");
		}
	}

	private List<String> normalizeItems(List<String> names) {
		return names.stream().map(String::trim).toList();
	}
}
