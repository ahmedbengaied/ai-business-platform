package com.ahmed.ai_business_platform.firm.entity;

import java.time.Instant;

import com.ahmed.ai_business_platform.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "firm_invitations")
public class FirmInvitation {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "firm_id", nullable = false)
	private AccountingFirm firm;

	@Column(nullable = false, length = 320)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private FirmRole role;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by_id", nullable = false)
	private User createdBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "accepted_at")
	private Instant acceptedAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	protected FirmInvitation() {
	}

	public FirmInvitation(
			AccountingFirm firm,
			String email,
			FirmRole role,
			String tokenHash,
			User createdBy,
			Instant expiresAt) {
		this.firm = firm;
		this.email = email;
		this.role = role;
		this.tokenHash = tokenHash;
		this.createdBy = createdBy;
		this.expiresAt = expiresAt;
	}

	public void accept(Instant acceptedAt) {
		this.acceptedAt = acceptedAt;
	}

	public void revoke(Instant revokedAt) {
		this.revokedAt = revokedAt;
	}

	@PrePersist
	void prePersist() {
		createdAt = Instant.now();
	}

	public String getId() {
		return id;
	}

	public AccountingFirm getFirm() {
		return firm;
	}

	public String getEmail() {
		return email;
	}

	public FirmRole getRole() {
		return role;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getAcceptedAt() {
		return acceptedAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}
}
