package com.ahmed.ai_business_platform.sales.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.ahmed.ai_business_platform.sales.entity.SalesInvoice;
import com.ahmed.ai_business_platform.sales.entity.SalesInvoicePayment;
import com.ahmed.ai_business_platform.sales.entity.SalesInvoiceStatus;

public record SalesInvoiceResponse(
		String id,
		String invoiceNumber,
		String customerId,
		String customerName,
		String description,
		LocalDate issueDate,
		LocalDate dueDate,
		String currency,
		BigDecimal subtotal,
		BigDecimal taxRate,
		BigDecimal taxAmount,
		BigDecimal totalAmount,
		BigDecimal amountPaid,
		BigDecimal balance,
		SalesInvoiceStatus status,
		List<SalesInvoicePaymentResponse> payments,
		Instant createdAt) {

	public static SalesInvoiceResponse from(SalesInvoice invoice, LocalDate today) {
		BigDecimal taxAmount = invoice.getSubtotal()
				.multiply(invoice.getTaxRate())
				.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
		BigDecimal totalAmount = invoice.getSubtotal().add(taxAmount);
		BigDecimal amountPaid = invoice.getPayments().stream()
				.map(SalesInvoicePayment::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.UNNECESSARY);
		BigDecimal balance = totalAmount.subtract(amountPaid).setScale(2, RoundingMode.UNNECESSARY);

		SalesInvoiceStatus status;
		if (balance.signum() == 0) {
			status = SalesInvoiceStatus.PAID;
		} else if (invoice.getDueDate().isBefore(today)) {
			status = SalesInvoiceStatus.OVERDUE;
		} else if (amountPaid.signum() > 0) {
			status = SalesInvoiceStatus.PARTIALLY_PAID;
		} else {
			status = SalesInvoiceStatus.ISSUED;
		}

		return new SalesInvoiceResponse(
				invoice.getId(),
				invoice.getInvoiceNumber(),
				invoice.getCustomer().getId(),
				invoice.getCustomer().getName(),
				invoice.getDescription(),
				invoice.getIssueDate(),
				invoice.getDueDate(),
				invoice.getCurrency(),
				invoice.getSubtotal(),
				invoice.getTaxRate(),
				taxAmount,
				totalAmount,
				amountPaid,
				balance,
				status,
				invoice.getPayments().stream().map(SalesInvoicePaymentResponse::from).toList(),
				invoice.getCreatedAt());
	}
}
