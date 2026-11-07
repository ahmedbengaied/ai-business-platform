package com.ahmed.ai_business_platform.sales.dto;

import java.time.Instant;

import com.ahmed.ai_business_platform.sales.entity.SalesCustomer;

public record SalesCustomerResponse(String id, String name, String email, String phone, Instant createdAt) {

	public static SalesCustomerResponse from(SalesCustomer customer) {
		return new SalesCustomerResponse(
				customer.getId(),
				customer.getName(),
				customer.getEmail(),
				customer.getPhone(),
				customer.getCreatedAt());
	}
}
