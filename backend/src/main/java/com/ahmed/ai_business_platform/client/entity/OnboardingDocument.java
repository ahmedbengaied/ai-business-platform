package com.ahmed.ai_business_platform.client.entity;

import java.time.Instant;

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
@Table(name = "onboarding_documents")
public class OnboardingDocument {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(nullable = false, length = 200)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OnboardingDocumentStatus status = OnboardingDocumentStatus.REQUESTED;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "client_id", nullable = false)
	private AccountingClient client;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected OnboardingDocument() {
	}

	public OnboardingDocument(String name) {
		this.name = name;
	}

	void setClient(AccountingClient client) {
		this.client = client;
	}

	public void setStatus(OnboardingDocumentStatus status) {
		this.status = status;
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

	public OnboardingDocumentStatus getStatus() {
		return status;
	}

	public AccountingClient getClient() {
		return client;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
