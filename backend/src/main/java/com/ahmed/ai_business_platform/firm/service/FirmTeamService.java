package com.ahmed.ai_business_platform.firm.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ahmed.ai_business_platform.firm.dto.CreateFirmInvitationRequest;
import com.ahmed.ai_business_platform.firm.dto.CreatedFirmInvitationResponse;
import com.ahmed.ai_business_platform.firm.dto.FirmInvitationPreviewResponse;
import com.ahmed.ai_business_platform.firm.dto.FirmInvitationResponse;
import com.ahmed.ai_business_platform.firm.dto.FirmMemberResponse;
import com.ahmed.ai_business_platform.firm.dto.FirmTeamResponse;
import com.ahmed.ai_business_platform.firm.dto.UpdateFirmMemberRoleRequest;
import com.ahmed.ai_business_platform.firm.entity.FirmInvitation;
import com.ahmed.ai_business_platform.firm.entity.FirmRole;
import com.ahmed.ai_business_platform.firm.repository.FirmInvitationRepository;
import com.ahmed.ai_business_platform.user.entity.User;
import com.ahmed.ai_business_platform.user.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class FirmTeamService {

	private static final Duration INVITATION_LIFETIME = Duration.ofDays(7);

	private final FirmAccessService firmAccess;
	private final FirmInvitationRepository invitationRepository;
	private final UserRepository userRepository;
	private final FirmInvitationTokens invitationTokens;

	public FirmTeamService(
			FirmAccessService firmAccess,
			FirmInvitationRepository invitationRepository,
			UserRepository userRepository,
			FirmInvitationTokens invitationTokens) {
		this.firmAccess = firmAccess;
		this.invitationRepository = invitationRepository;
		this.userRepository = userRepository;
		this.invitationTokens = invitationTokens;
	}

	public FirmTeamResponse getTeam(String email) {
		User currentUser = firmAccess.requireUser(email);
		String firmId = currentUser.getFirm().getId();
		List<FirmMemberResponse> members = userRepository
				.findAllByFirm_IdAndEnabledTrueOrderByFirstNameAscLastNameAsc(firmId).stream()
				.map(FirmMemberResponse::from)
				.toList();
		List<FirmInvitationResponse> invitations = List.of();
		if (currentUser.getFirmRole() == FirmRole.OWNER || currentUser.getFirmRole() == FirmRole.ADMIN) {
			invitations = invitationRepository
					.findAllByFirm_IdAndAcceptedAtIsNullAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
							firmId, Instant.now()).stream()
					.map(FirmInvitationResponse::from)
					.toList();
		}
		return new FirmTeamResponse(currentUser.getFirm().getName(), currentUser.getFirmRole().name(),
				members, invitations);
	}

	@Transactional
	public CreatedFirmInvitationResponse createInvitation(String email, CreateFirmInvitationRequest request) {
		User actor = firmAccess.requireManager(email);
		FirmRole requestedRole = request.role();
		if (requestedRole == FirmRole.OWNER) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The owner role cannot be assigned by invitation");
		}
		if (actor.getFirmRole() != FirmRole.OWNER && requestedRole == FirmRole.ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the firm owner can invite admins");
		}

		String inviteEmail = request.email().trim().toLowerCase(Locale.ROOT);
		userRepository.findByEmailIgnoreCase(inviteEmail).ifPresent(existing -> {
			if (existing.isEnabled() || !existing.getFirm().getId().equals(actor.getFirm().getId())) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"This email already has an account; firm invitations are for new or removed firm members");
			}
		});

		Instant now = Instant.now();
		if (invitationRepository
				.existsByFirm_IdAndEmailIgnoreCaseAndAcceptedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(
						actor.getFirm().getId(), inviteEmail, now)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "An active invitation already exists for this email");
		}

		String token = invitationTokens.create();
		Instant expiresAt = now.plus(INVITATION_LIFETIME);
		FirmInvitation invitation = invitationRepository.saveAndFlush(new FirmInvitation(
				actor.getFirm(),
				inviteEmail,
				requestedRole,
				invitationTokens.hash(token),
				actor,
				expiresAt));
		return new CreatedFirmInvitationResponse(
				invitation.getId(), inviteEmail, requestedRole.name(), token, expiresAt);
	}

	public FirmInvitationPreviewResponse previewInvitation(String token) {
		FirmInvitation invitation = requireValidInvitation(token);
		return new FirmInvitationPreviewResponse(
				invitation.getFirm().getName(),
				invitation.getEmail(),
				invitation.getRole().name(),
				invitation.getExpiresAt());
	}

	@Transactional
	public void revokeInvitation(String email, String invitationId) {
		User actor = firmAccess.requireManager(email);
		FirmInvitation invitation = invitationRepository
				.findByIdAndFirm_IdAndAcceptedAtIsNullAndRevokedAtIsNull(invitationId, actor.getFirm().getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		invitation.revoke(Instant.now());
	}

	@Transactional
	public FirmMemberResponse updateMemberRole(
			String email,
			String memberId,
			UpdateFirmMemberRoleRequest request) {
		User actor = firmAccess.requireManager(email);
		User member = requireManageableMember(actor, memberId);
		if (request.role() == FirmRole.OWNER) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ownership cannot be changed here");
		}
		if (actor.getFirmRole() != FirmRole.OWNER && request.role() == FirmRole.ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the firm owner can assign admins");
		}
		if (actor.getId().equals(member.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot change your own firm role");
		}
		member.setFirmRole(request.role());
		return FirmMemberResponse.from(member);
	}

	@Transactional
	public void removeMember(String email, String memberId) {
		User actor = firmAccess.requireManager(email);
		User member = requireManageableMember(actor, memberId);
		if (actor.getId().equals(member.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot remove your own account");
		}
		member.disable();
	}

	private User requireManageableMember(User actor, String memberId) {
		User member = userRepository.findByIdAndFirm_IdAndEnabledTrue(memberId, actor.getFirm().getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (member.getFirmRole() == FirmRole.OWNER
				|| (actor.getFirmRole() == FirmRole.ADMIN && member.getFirmRole() == FirmRole.ADMIN)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot manage this firm member");
		}
		return member;
	}

	private FirmInvitation requireValidInvitation(String token) {
		if (token == null || token.isBlank()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation is invalid or expired");
		}
		return invitationRepository
				.findByTokenHashAndAcceptedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(
						invitationTokens.hash(token), Instant.now())
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND, "Invitation is invalid or expired"));
	}
}
