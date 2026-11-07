package com.ahmed.ai_business_platform.firm.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounting_firms")
public class AccountingFirm {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected AccountingFirm() {
	}

	public AccountingFirm(String name) {
		this.name = name;
	}

	@PrePersist
	void prePersist() {
		createdAt = Instant.now();
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
