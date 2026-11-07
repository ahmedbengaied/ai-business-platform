package com.ahmed.ai_business_platform.client.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ahmed.ai_business_platform.client.entity.AccountingClient;

public interface AccountingClientRepository extends JpaRepository<AccountingClient, String> {

	@EntityGraph(attributePaths = "documents")
	List<AccountingClient> findAllByFirm_IdOrderByCreatedAtDesc(String firmId);

	@EntityGraph(attributePaths = "documents")
	Optional<AccountingClient> findByIdAndFirm_Id(String id, String firmId);
}
