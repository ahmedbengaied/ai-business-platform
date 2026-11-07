package com.ahmed.ai_business_platform.firm.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ahmed.ai_business_platform.firm.dto.CreateFirmInvitationRequest;
import com.ahmed.ai_business_platform.firm.dto.CreatedFirmInvitationResponse;
import com.ahmed.ai_business_platform.firm.dto.FirmInvitationPreviewResponse;
import com.ahmed.ai_business_platform.firm.dto.FirmMemberResponse;
import com.ahmed.ai_business_platform.firm.dto.FirmTeamResponse;
import com.ahmed.ai_business_platform.firm.dto.UpdateFirmMemberRoleRequest;
import com.ahmed.ai_business_platform.firm.service.FirmTeamService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/firm")
@Validated
public class FirmTeamController {

	private final FirmTeamService teamService;

	public FirmTeamController(FirmTeamService teamService) {
		this.teamService = teamService;
	}

	@GetMapping("/team")
	public FirmTeamResponse team(Authentication authentication) {
		return teamService.getTeam(authentication.getName());
	}

	@PostMapping("/invitations")
	@ResponseStatus(HttpStatus.CREATED)
	public CreatedFirmInvitationResponse createInvitation(
			Authentication authentication,
			@Valid @RequestBody CreateFirmInvitationRequest request) {
		return teamService.createInvitation(authentication.getName(), request);
	}

	@GetMapping("/invitations/preview")
	public FirmInvitationPreviewResponse previewInvitation(@RequestParam String token) {
		return teamService.previewInvitation(token);
	}

	@DeleteMapping("/invitations/{invitationId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void revokeInvitation(Authentication authentication, @PathVariable String invitationId) {
		teamService.revokeInvitation(authentication.getName(), invitationId);
	}

	@PatchMapping("/members/{memberId}")
	public FirmMemberResponse updateMemberRole(
			Authentication authentication,
			@PathVariable String memberId,
			@Valid @RequestBody UpdateFirmMemberRoleRequest request) {
		return teamService.updateMemberRole(authentication.getName(), memberId, request);
	}

	@DeleteMapping("/members/{memberId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeMember(Authentication authentication, @PathVariable String memberId) {
		teamService.removeMember(authentication.getName(), memberId);
	}
}
