package com.ahmed.ai_business_platform.sales.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSalesCustomerRequest(
		@NotBlank @Size(max = 200) String name,
		@Email @Size(max = 320) String email,
		@Size(max = 40) String phone) {
}
