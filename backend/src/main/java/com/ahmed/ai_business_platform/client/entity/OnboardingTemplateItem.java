package com.ahmed.ai_business_platform.client.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "onboarding_template_items")
public class OnboardingTemplateItem {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false)
	private int position;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "template_id", nullable = false)
	private OnboardingTemplate template;

	protected OnboardingTemplateItem() {
	}

	public OnboardingTemplateItem(String name, int position, OnboardingTemplate template) {
		this.name = name;
		this.position = position;
		this.template = template;
	}

	public String getName() {
		return name;
	}
}
