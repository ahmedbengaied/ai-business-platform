package com.ahmed.ai_business_platform.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAccountingClientRequest(
		@NotBlank @Size(max = 200) String legalName,
		@NotBlank @Size(max = 200) String contactName,
		@Email @Size(max = 320) String contactEmail,
		@Size(max = 40) String contactPhone,
		String templateId) {
}
