package com.ahmed.ai_business_platform.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecordSalesInvoicePaymentRequest(
		@NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
		@NotNull LocalDate paymentDate,
		@Size(max = 300) String note) {
}
