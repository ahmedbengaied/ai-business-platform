package com.ahmed.ai_business_platform.purchase.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ahmed.ai_business_platform.purchase.entity.PurchaseBill;

public interface PurchaseBillRepository extends JpaRepository<PurchaseBill, String> {

	@EntityGraph(attributePaths = "payments")
	List<PurchaseBill> findAllByFirm_IdOrderByIssueDateDescCreatedAtDesc(String firmId);

	boolean existsByFirm_IdAndBillNumberIgnoreCase(String firmId, String billNumber);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select bill from PurchaseBill bill where bill.id = :billId and bill.firm.id = :firmId")
	Optional<PurchaseBill> findForUpdateByIdAndFirmId(
			@Param("billId") String billId,
			@Param("firmId") String firmId);
}
