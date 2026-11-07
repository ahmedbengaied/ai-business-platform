package com.ahmed.ai_business_platform.client.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.ahmed.ai_business_platform.firm.entity.AccountingFirm;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounting_clients")
public class AccountingClient {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(name = "legal_name", nullable = false, length = 200)
	private String legalName;

	@Column(name = "contact_name", nullable = false, length = 200)
	private String contactName;

	@Column(name = "contact_email", length = 320)
	private String contactEmail;

	@Column(name = "contact_phone", length = 40)
	private String contactPhone;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "firm_id", nullable = false)
	private AccountingFirm firm;

	@OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OnboardingDocument> documents = new ArrayList<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected AccountingClient() {
	}

	public AccountingClient(
			String legalName,
			String contactName,
			String contactEmail,
			String contactPhone,
			AccountingFirm firm) {
		this.legalName = legalName;
		this.contactName = contactName;
		this.contactEmail = contactEmail;
		this.contactPhone = contactPhone;
		this.firm = firm;
	}

	public void addDocument(OnboardingDocument document) {
		documents.add(document);
		document.setClient(this);
	}

	@PrePersist
	void prePersist() {
		createdAt = Instant.now();
	}

	public String getId() {
		return id;
	}

	public String getLegalName() {
		return legalName;
	}

	public String getContactName() {
		return contactName;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public String getContactPhone() {
		return contactPhone;
	}

	public AccountingFirm getFirm() {
		return firm;
	}

	public List<OnboardingDocument> getDocuments() {
		return documents;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
