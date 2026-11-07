package com.ahmed.ai_business_platform.sales.entity;

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
@Table(name = "sales_customers")
public class SalesCustomer {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(length = 320)
	private String email;

	@Column(length = 40)
	private String phone;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "firm_id", nullable = false)
	private AccountingFirm firm;

	@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
	private List<SalesInvoice> invoices = new ArrayList<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected SalesCustomer() {
	}

	public SalesCustomer(String name, String email, String phone, AccountingFirm firm) {
		this.name = name;
		this.email = email;
		this.phone = phone;
		this.firm = firm;
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

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	public AccountingFirm getFirm() {
		return firm;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
