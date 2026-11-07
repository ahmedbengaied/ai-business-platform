package com.ahmed.ai_business_platform.sales.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ahmed.ai_business_platform.sales.entity.SalesInvoice;

public interface SalesInvoiceRepository extends JpaRepository<SalesInvoice, String> {

	@EntityGraph(attributePaths = { "customer", "payments" })
	List<SalesInvoice> findAllByFirm_IdOrderByIssueDateDescCreatedAtDesc(String firmId);

	boolean existsByFirm_IdAndInvoiceNumberIgnoreCase(String firmId, String invoiceNumber);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select invoice from SalesInvoice invoice where invoice.id = :invoiceId and invoice.firm.id = :firmId")
	Optional<SalesInvoice> findForUpdateByIdAndFirmId(
			@Param("invoiceId") String invoiceId,
			@Param("firmId") String firmId);
}
