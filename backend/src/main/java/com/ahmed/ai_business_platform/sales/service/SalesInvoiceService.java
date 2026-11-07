package com.ahmed.ai_business_platform.sales.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ahmed.ai_business_platform.firm.service.FirmAccessService;
import com.ahmed.ai_business_platform.sales.dto.CreateSalesCustomerRequest;
import com.ahmed.ai_business_platform.sales.dto.CreateSalesInvoiceRequest;
import com.ahmed.ai_business_platform.sales.dto.RecordSalesInvoicePaymentRequest;
import com.ahmed.ai_business_platform.sales.dto.SalesCustomerResponse;
import com.ahmed.ai_business_platform.sales.dto.SalesInvoiceResponse;
import com.ahmed.ai_business_platform.sales.entity.SalesCustomer;
import com.ahmed.ai_business_platform.sales.entity.SalesInvoice;
import com.ahmed.ai_business_platform.sales.entity.SalesInvoicePayment;
import com.ahmed.ai_business_platform.sales.repository.SalesCustomerRepository;
import com.ahmed.ai_business_platform.sales.repository.SalesInvoiceRepository;

@Service
@Transactional(readOnly = true)
public class SalesInvoiceService {

	private final FirmAccessService firmAccess;
	private final SalesCustomerRepository customerRepository;
	private final SalesInvoiceRepository invoiceRepository;

	public SalesInvoiceService(
			FirmAccessService firmAccess,
			SalesCustomerRepository customerRepository,
			SalesInvoiceRepository invoiceRepository) {
		this.firmAccess = firmAccess;
		this.customerRepository = customerRepository;
		this.invoiceRepository = invoiceRepository;
	}

	public List<SalesCustomerResponse> listCustomers(String email) {
		String firmId = firmAccess.requireUser(email).getFirm().getId();
		return customerRepository.findAllByFirm_IdOrderByNameAsc(firmId).stream()
				.map(SalesCustomerResponse::from)
				.toList();
	}

	@Transactional
	public SalesCustomerResponse createCustomer(String email, CreateSalesCustomerRequest request) {
		var actor = firmAccess.requireCanWrite(email);
		SalesCustomer customer = new SalesCustomer(
				request.name().trim(),
				trimToNull(request.email()),
				trimToNull(request.phone()),
				actor.getFirm());
		return SalesCustomerResponse.from(customerRepository.save(customer));
	}

	public List<SalesInvoiceResponse> listInvoices(String email) {
		String firmId = firmAccess.requireUser(email).getFirm().getId();
		LocalDate today = LocalDate.now();
		return invoiceRepository.findAllByFirm_IdOrderByIssueDateDescCreatedAtDesc(firmId).stream()
				.map(invoice -> SalesInvoiceResponse.from(invoice, today))
				.toList();
	}

	@Transactional
	public SalesInvoiceResponse createInvoice(String email, CreateSalesInvoiceRequest request) {
		var actor = firmAccess.requireCanWrite(email);
		String firmId = actor.getFirm().getId();
		String invoiceNumber = request.invoiceNumber().trim().toUpperCase(Locale.ROOT);
		if (invoiceRepository.existsByFirm_IdAndInvoiceNumberIgnoreCase(firmId, invoiceNumber)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "An invoice with this number already exists");
		}
		SalesCustomer customer = customerRepository.findByIdAndFirm_Id(request.customerId(), firmId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
		SalesInvoice invoice = new SalesInvoice(
				actor.getFirm(),
				customer,
				invoiceNumber,
				request.description().trim(),
				request.issueDate(),
				request.dueDate(),
				request.currency().trim().toUpperCase(Locale.ROOT),
				request.subtotal().setScale(2, RoundingMode.UNNECESSARY),
				request.taxRate().setScale(2, RoundingMode.UNNECESSARY));
		return SalesInvoiceResponse.from(invoiceRepository.saveAndFlush(invoice), LocalDate.now());
	}

	@Transactional
	public SalesInvoiceResponse recordPayment(
			String email,
			String invoiceId,
			RecordSalesInvoicePaymentRequest request) {
		var actor = firmAccess.requireCanWrite(email);
		SalesInvoice invoice = invoiceRepository.findForUpdateByIdAndFirmId(
						invoiceId, actor.getFirm().getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

		BigDecimal taxAmount = invoice.getSubtotal()
				.multiply(invoice.getTaxRate())
				.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
		BigDecimal totalAmount = invoice.getSubtotal().add(taxAmount);
		BigDecimal amountPaid = invoice.getPayments().stream()
				.map(SalesInvoicePayment::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal balance = totalAmount.subtract(amountPaid);
		BigDecimal amount = request.amount().setScale(2, RoundingMode.UNNECESSARY);
		if (amount.compareTo(balance) > 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment cannot exceed the invoice balance");
		}

		invoice.addPayment(new SalesInvoicePayment(
				amount,
				request.paymentDate(),
				trimToNull(request.note())));
		return SalesInvoiceResponse.from(invoice, LocalDate.now());
	}

	private String trimToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
