package com.ahmed.ai_business_platform.firm.service;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ahmed.ai_business_platform.firm.entity.AccountingFirm;
import com.ahmed.ai_business_platform.firm.entity.FirmInvitation;
import com.ahmed.ai_business_platform.firm.repository.AccountingFirmRepository;
import com.ahmed.ai_business_platform.firm.repository.FirmInvitationRepository;

@Service
public class FirmService {

	private final AccountingFirmRepository firmRepository;
	private final FirmInvitationRepository invitationRepository;
	private final FirmInvitationTokens invitationTokens;

	public FirmService(
			AccountingFirmRepository firmRepository,
			FirmInvitationRepository invitationRepository,
			FirmInvitationTokens invitationTokens) {
		this.firmRepository = firmRepository;
		this.invitationRepository = invitationRepository;
		this.invitationTokens = invitationTokens;
	}

	@Transactional
	public AccountingFirm createFirm(String name) {
		return firmRepository.save(new AccountingFirm(name));
	}

	@Transactional
	public FirmInvitation acceptInvitation(String token, String email) {
		if (token == null || token.isBlank()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation is invalid or expired");
		}
		Instant now = Instant.now();
		FirmInvitation invitation = invitationRepository
				.findAvailableForAcceptance(
						invitationTokens.hash(token), now)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Invitation is invalid or expired"));
		if (!invitation.getEmail().equalsIgnoreCase(email)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invitation email does not match");
		}
		invitation.accept(now);
		return invitation;
	}
}
