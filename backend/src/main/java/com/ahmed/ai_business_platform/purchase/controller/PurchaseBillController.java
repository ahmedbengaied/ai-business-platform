package com.ahmed.ai_business_platform.purchase.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ahmed.ai_business_platform.purchase.dto.CreatePurchaseBillRequest;
import com.ahmed.ai_business_platform.purchase.dto.PurchaseBillResponse;
import com.ahmed.ai_business_platform.purchase.dto.RecordPurchaseBillPaymentRequest;
import com.ahmed.ai_business_platform.purchase.service.PurchaseBillService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/purchases/bills")
@Validated
public class PurchaseBillController {

	private final PurchaseBillService billService;

	public PurchaseBillController(PurchaseBillService billService) {
		this.billService = billService;
	}

	@GetMapping
	public List<PurchaseBillResponse> list(Authentication authentication) {
		return billService.list(authentication.getName());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PurchaseBillResponse create(
			Authentication authentication,
			@Valid @RequestBody CreatePurchaseBillRequest request) {
		return billService.create(authentication.getName(), request);
	}

	@PostMapping("/{billId}/payments")
	public PurchaseBillResponse recordPayment(
			Authentication authentication,
			@PathVariable String billId,
			@Valid @RequestBody RecordPurchaseBillPaymentRequest request) {
		return billService.recordPayment(authentication.getName(), billId, request);
	}
}
