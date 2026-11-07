package com.ahmed.ai_business_platform.purchase.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.ahmed.ai_business_platform.purchase.entity.PurchaseBill;
import com.ahmed.ai_business_platform.purchase.entity.PurchaseBillPayment;
import com.ahmed.ai_business_platform.purchase.entity.PurchaseBillStatus;

public record PurchaseBillResponse(
		String id,
		String supplierName,
		String billNumber,
		String category,
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
		PurchaseBillStatus status,
		List<PurchaseBillPaymentResponse> payments,
		Instant createdAt) {

	public static PurchaseBillResponse from(PurchaseBill bill, LocalDate today) {
		BigDecimal taxAmount = bill.getSubtotal()
				.multiply(bill.getTaxRate())
				.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
		BigDecimal totalAmount = bill.getSubtotal().add(taxAmount);
		BigDecimal amountPaid = bill.getPayments().stream()
				.map(PurchaseBillPayment::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.UNNECESSARY);
		BigDecimal balance = totalAmount.subtract(amountPaid).setScale(2, RoundingMode.UNNECESSARY);

		PurchaseBillStatus status;
		if (balance.signum() == 0) {
			status = PurchaseBillStatus.PAID;
		} else if (bill.getDueDate().isBefore(today)) {
			status = PurchaseBillStatus.OVERDUE;
		} else if (amountPaid.signum() > 0) {
			status = PurchaseBillStatus.PARTIALLY_PAID;
		} else {
			status = PurchaseBillStatus.ISSUED;
		}

		return new PurchaseBillResponse(
				bill.getId(),
				bill.getSupplierName(),
				bill.getBillNumber(),
				bill.getCategory(),
				bill.getDescription(),
				bill.getIssueDate(),
				bill.getDueDate(),
				bill.getCurrency(),
				bill.getSubtotal(),
				bill.getTaxRate(),
				taxAmount,
				totalAmount,
				amountPaid,
				balance,
				status,
				bill.getPayments().stream().map(PurchaseBillPaymentResponse::from).toList(),
				bill.getCreatedAt());
	}
}
