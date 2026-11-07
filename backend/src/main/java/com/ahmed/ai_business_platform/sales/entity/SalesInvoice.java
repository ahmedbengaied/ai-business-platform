package com.ahmed.ai_business_platform.sales.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
@Table(name = "sales_invoices")
public class SalesInvoice {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "firm_id", nullable = false)
	private AccountingFirm firm;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private SalesCustomer customer;

	@Column(name = "invoice_number", nullable = false, length = 50)
	private String invoiceNumber;

	@Column(nullable = false, length = 500)
	private String description;

	@Column(name = "issue_date", nullable = false)
	private LocalDate issueDate;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal subtotal;

	@Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
	private BigDecimal taxRate;

	@OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("paymentDate ASC, createdAt ASC")
	private List<SalesInvoicePayment> payments = new ArrayList<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected SalesInvoice() {
	}

	public SalesInvoice(
			AccountingFirm firm,
			SalesCustomer customer,
			String invoiceNumber,
			String description,
			LocalDate issueDate,
			LocalDate dueDate,
			String currency,
			BigDecimal subtotal,
			BigDecimal taxRate) {
		this.firm = firm;
		this.customer = customer;
		this.invoiceNumber = invoiceNumber;
		this.description = description;
		this.issueDate = issueDate;
		this.dueDate = dueDate;
		this.currency = currency;
		this.subtotal = subtotal;
		this.taxRate = taxRate;
	}

	public void addPayment(SalesInvoicePayment payment) {
		payments.add(payment);
		payment.setInvoice(this);
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

	public SalesCustomer getCustomer() {
		return customer;
	}

	public String getInvoiceNumber() {
		return invoiceNumber;
	}

	public String getDescription() {
		return description;
	}

	public LocalDate getIssueDate() {
		return issueDate;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public String getCurrency() {
		return currency;
	}

	public BigDecimal getSubtotal() {
		return subtotal;
	}

	public BigDecimal getTaxRate() {
		return taxRate;
	}

	public List<SalesInvoicePayment> getPayments() {
		return payments;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
