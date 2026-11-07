package com.ahmed.ai_business_platform.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSalesInvoiceRequest(
		@NotBlank @Size(max = 50) String invoiceNumber,
		@NotBlank String customerId,
		@NotBlank @Size(max = 500) String description,
		@NotNull LocalDate issueDate,
		@NotNull LocalDate dueDate,
		@NotBlank @Pattern(regexp = "(?i)^[A-Z]{3}$") String currency,
		@NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal subtotal,
		@NotNull @DecimalMin(value = "0.00") @DecimalMax(value = "100.00")
		@Digits(integer = 3, fraction = 2) BigDecimal taxRate) {

		@AssertTrue(message = "Due date must be on or after the issue date")
		public boolean isDueDateOnOrAfterIssueDate() {
			return issueDate == null || dueDate == null || !dueDate.isBefore(issueDate);
		}
}
