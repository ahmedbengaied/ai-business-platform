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
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "onboarding_templates")
public class OnboardingTemplate {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(nullable = false, length = 150)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "firm_id", nullable = false)
	private AccountingFirm firm;

	@OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position ASC")
	private List<OnboardingTemplateItem> items = new ArrayList<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected OnboardingTemplate() {
	}

	public OnboardingTemplate(String name, AccountingFirm firm) {
		this.name = name;
		this.firm = firm;
	}

	public void replaceItems(List<String> names) {
		items.clear();
		for (int index = 0; index < names.size(); index++) {
			items.add(new OnboardingTemplateItem(names.get(index), index, this));
		}
	}

	public void setName(String name) {
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

	public AccountingFirm getFirm() {
		return firm;
	}

	public List<OnboardingTemplateItem> getItems() {
		return items;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
