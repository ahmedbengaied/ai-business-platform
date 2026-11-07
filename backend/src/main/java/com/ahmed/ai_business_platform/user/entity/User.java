package com.ahmed.ai_business_platform.user.entity;

import java.time.Instant;

import com.ahmed.ai_business_platform.firm.entity.AccountingFirm;
import com.ahmed.ai_business_platform.firm.entity.FirmRole;

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
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(nullable = false, unique = true, length = 320)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role = Role.USER;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "firm_id", nullable = false)
	private AccountingFirm firm;

	@Enumerated(EnumType.STRING)
	@Column(name = "firm_role", nullable = false, length = 20)
	private FirmRole firmRole = FirmRole.OWNER;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(name = "auth_version", nullable = false)
	private long authVersion;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected User() {
	}

	public User(
			String email,
			String passwordHash,
			String firstName,
			String lastName,
			AccountingFirm firm,
			FirmRole firmRole) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.firstName = firstName;
		this.lastName = lastName;
		this.firm = firm;
		this.firmRole = firmRole;
	}

	@PrePersist
	void prePersist() {
		createdAt = Instant.now();
	}

	public String getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public Role getRole() {
		return role;
	}

	public AccountingFirm getFirm() {
		return firm;
	}

	public FirmRole getFirmRole() {
		return firmRole;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public long getAuthVersion() {
		return authVersion;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setFirmRole(FirmRole firmRole) {
		this.firmRole = firmRole;
	}

	public void disable() {
		enabled = false;
		authVersion++;
	}

	public void reactivate(String passwordHash, String firstName, String lastName, FirmRole firmRole) {
		this.passwordHash = passwordHash;
		this.firstName = firstName;
		this.lastName = lastName;
		this.firmRole = firmRole;
		enabled = true;
		authVersion++;
	}
}
