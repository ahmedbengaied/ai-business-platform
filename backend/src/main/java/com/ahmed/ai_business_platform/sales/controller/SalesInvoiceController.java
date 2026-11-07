package com.ahmed.ai_business_platform.sales.controller;

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

import com.ahmed.ai_business_platform.sales.dto.CreateSalesCustomerRequest;
import com.ahmed.ai_business_platform.sales.dto.CreateSalesInvoiceRequest;
import com.ahmed.ai_business_platform.sales.dto.RecordSalesInvoicePaymentRequest;
import com.ahmed.ai_business_platform.sales.dto.SalesCustomerResponse;
import com.ahmed.ai_business_platform.sales.dto.SalesInvoiceResponse;
import com.ahmed.ai_business_platform.sales.service.SalesInvoiceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/sales")
@Validated
public class SalesInvoiceController {

	private final SalesInvoiceService salesInvoiceService;

	public SalesInvoiceController(SalesInvoiceService salesInvoiceService) {
		this.salesInvoiceService = salesInvoiceService;
	}

	@GetMapping("/customers")
	public List<SalesCustomerResponse> listCustomers(Authentication authentication) {
		return salesInvoiceService.listCustomers(authentication.getName());
	}

	@PostMapping("/customers")
	@ResponseStatus(HttpStatus.CREATED)
	public SalesCustomerResponse createCustomer(
			Authentication authentication,
			@Valid @RequestBody CreateSalesCustomerRequest request) {
		return salesInvoiceService.createCustomer(authentication.getName(), request);
	}

	@GetMapping("/invoices")
	public List<SalesInvoiceResponse> listInvoices(Authentication authentication) {
		return salesInvoiceService.listInvoices(authentication.getName());
	}

	@PostMapping("/invoices")
	@ResponseStatus(HttpStatus.CREATED)
	public SalesInvoiceResponse createInvoice(
			Authentication authentication,
			@Valid @RequestBody CreateSalesInvoiceRequest request) {
		return salesInvoiceService.createInvoice(authentication.getName(), request);
	}

	@PostMapping("/invoices/{invoiceId}/payments")
	public SalesInvoiceResponse recordPayment(
			Authentication authentication,
			@PathVariable String invoiceId,
			@Valid @RequestBody RecordSalesInvoicePaymentRequest request) {
		return salesInvoiceService.recordPayment(authentication.getName(), invoiceId, request);
	}
}
