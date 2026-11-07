package com.ahmed.ai_business_platform.sales.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ahmed.ai_business_platform.sales.entity.SalesCustomer;

public interface SalesCustomerRepository extends JpaRepository<SalesCustomer, String> {

	List<SalesCustomer> findAllByFirm_IdOrderByNameAsc(String firmId);

	Optional<SalesCustomer> findByIdAndFirm_Id(String id, String firmId);
}
