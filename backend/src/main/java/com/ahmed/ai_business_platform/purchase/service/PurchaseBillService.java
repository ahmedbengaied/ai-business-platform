package com.ahmed.ai_business_platform.purchase.service;

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
import com.ahmed.ai_business_platform.purchase.dto.CreatePurchaseBillRequest;
import com.ahmed.ai_business_platform.purchase.dto.PurchaseBillResponse;
import com.ahmed.ai_business_platform.purchase.dto.RecordPurchaseBillPaymentRequest;
import com.ahmed.ai_business_platform.purchase.entity.PurchaseBill;
import com.ahmed.ai_business_platform.purchase.entity.PurchaseBillPayment;
import com.ahmed.ai_business_platform.purchase.repository.PurchaseBillRepository;

@Service
@Transactional(readOnly = true)
public class PurchaseBillService {

	private final FirmAccessService firmAccess;
	private final PurchaseBillRepository billRepository;

	public PurchaseBillService(FirmAccessService firmAccess, PurchaseBillRepository billRepository) {
		this.firmAccess = firmAccess;
		this.billRepository = billRepository;
	}

	public List<PurchaseBillResponse> list(String email) {
		String firmId = firmAccess.requireUser(email).getFirm().getId();
		LocalDate today = LocalDate.now();
		return billRepository.findAllByFirm_IdOrderByIssueDateDescCreatedAtDesc(firmId).stream()
				.map(bill -> PurchaseBillResponse.from(bill, today))
				.toList();
	}

	@Transactional
	public PurchaseBillResponse create(String email, CreatePurchaseBillRequest request) {
		var actor = firmAccess.requireCanWrite(email);
		String firmId = actor.getFirm().getId();
		String billNumber = request.billNumber().trim().toUpperCase(Locale.ROOT);
		if (billRepository.existsByFirm_IdAndBillNumberIgnoreCase(firmId, billNumber)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A bill with this number already exists");
		}
		PurchaseBill bill = new PurchaseBill(
				actor.getFirm(),
				request.supplierName().trim(),
				billNumber,
				request.category().trim(),
				request.description().trim(),
				request.issueDate(),
				request.dueDate(),
				request.currency().trim().toUpperCase(Locale.ROOT),
				request.subtotal().setScale(2, RoundingMode.UNNECESSARY),
				request.taxRate().setScale(2, RoundingMode.UNNECESSARY));
		return PurchaseBillResponse.from(billRepository.saveAndFlush(bill), LocalDate.now());
	}

	@Transactional
	public PurchaseBillResponse recordPayment(
			String email,
			String billId,
			RecordPurchaseBillPaymentRequest request) {
		var actor = firmAccess.requireCanWrite(email);
		PurchaseBill bill = billRepository.findForUpdateByIdAndFirmId(billId, actor.getFirm().getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found"));
		BigDecimal taxAmount = bill.getSubtotal()
				.multiply(bill.getTaxRate())
				.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
		BigDecimal totalAmount = bill.getSubtotal().add(taxAmount);
		BigDecimal amountPaid = bill.getPayments().stream()
				.map(PurchaseBillPayment::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal balance = totalAmount.subtract(amountPaid);
		BigDecimal amount = request.amount().setScale(2, RoundingMode.UNNECESSARY);
		if (amount.compareTo(balance) > 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment cannot exceed the bill balance");
		}
		bill.addPayment(new PurchaseBillPayment(
				amount,
				request.paymentDate(),
				trimToNull(request.note())));
		return PurchaseBillResponse.from(bill, LocalDate.now());
	}

	private String trimToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
