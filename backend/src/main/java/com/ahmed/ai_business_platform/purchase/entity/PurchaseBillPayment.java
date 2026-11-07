package com.ahmed.ai_business_platform.purchase.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "purchase_bill_payments")
public class PurchaseBillPayment {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "bill_id", nullable = false)
	private PurchaseBill bill;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(name = "payment_date", nullable = false)
	private LocalDate paymentDate;

	@Column(length = 300)
	private String note;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected PurchaseBillPayment() {
	}

	public PurchaseBillPayment(BigDecimal amount, LocalDate paymentDate, String note) {
		this.amount = amount;
		this.paymentDate = paymentDate;
		this.note = note;
	}

	void setBill(PurchaseBill bill) {
		this.bill = bill;
	}

	@PrePersist
	void prePersist() {
		createdAt = Instant.now();
	}

	public String getId() {
		return id;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public LocalDate getPaymentDate() {
		return paymentDate;
	}

	public String getNote() {
		return note;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
