package com.ahmed.ai_business_platform.firm.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ahmed.ai_business_platform.firm.entity.FirmInvitation;

public interface FirmInvitationRepository extends JpaRepository<FirmInvitation, String> {

	Optional<FirmInvitation> findByTokenHashAndAcceptedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(
			String tokenHash,
			Instant now);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select invitation from FirmInvitation invitation
			where invitation.tokenHash = :tokenHash
			  and invitation.acceptedAt is null
			  and invitation.revokedAt is null
			  and invitation.expiresAt > :now
			""")
	Optional<FirmInvitation> findAvailableForAcceptance(
			@Param("tokenHash") String tokenHash,
			@Param("now") Instant now);

	Optional<FirmInvitation> findByIdAndFirm_IdAndAcceptedAtIsNullAndRevokedAtIsNull(String id, String firmId);

	List<FirmInvitation> findAllByFirm_IdAndAcceptedAtIsNullAndRevokedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
			String firmId,
			Instant now);

	boolean existsByFirm_IdAndEmailIgnoreCaseAndAcceptedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(
			String firmId,
			String email,
			Instant now);
}
