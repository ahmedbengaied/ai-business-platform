package com.ahmed.ai_business_platform.purchase.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.ahmed.ai_business_platform.purchase.entity.PurchaseBillPayment;

public record PurchaseBillPaymentResponse(
		String id,
		BigDecimal amount,
		LocalDate paymentDate,
		String note,
		Instant createdAt) {

	public static PurchaseBillPaymentResponse from(PurchaseBillPayment payment) {
		return new PurchaseBillPaymentResponse(
				payment.getId(),
				payment.getAmount(),
				payment.getPaymentDate(),
				payment.getNote(),
				payment.getCreatedAt());
	}
}
