package com.ahmed.ai_business_platform.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.ahmed.ai_business_platform.sales.entity.SalesInvoicePayment;

public record SalesInvoicePaymentResponse(
		String id,
		BigDecimal amount,
		LocalDate paymentDate,
		String note,
		Instant createdAt) {

	public static SalesInvoicePaymentResponse from(SalesInvoicePayment payment) {
		return new SalesInvoicePaymentResponse(
				payment.getId(),
				payment.getAmount(),
				payment.getPaymentDate(),
				payment.getNote(),
				payment.getCreatedAt());
	}
}
