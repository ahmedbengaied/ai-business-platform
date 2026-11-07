package com.ahmed.ai_business_platform.ai.usage;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiUsageRepository extends JpaRepository<AiUsageRecord, String> {

	@Query("""
			select coalesce(sum(record.costUsd), 0)
			from AiUsageRecord record
			where record.userId = :userId and record.createdAt >= :periodStart and record.createdAt < :periodEnd
			""")
	BigDecimal sumCostForPeriod(
			@Param("userId") String userId,
			@Param("periodStart") Instant periodStart,
			@Param("periodEnd") Instant periodEnd);

	@Query("""
			select coalesce(sum(record.promptTokens), 0)
			from AiUsageRecord record
			where record.userId = :userId and record.createdAt >= :periodStart and record.createdAt < :periodEnd
			""")
	Long sumPromptTokensForPeriod(
			@Param("userId") String userId,
			@Param("periodStart") Instant periodStart,
			@Param("periodEnd") Instant periodEnd);

	@Query("""
			select coalesce(sum(record.completionTokens), 0)
			from AiUsageRecord record
			where record.userId = :userId and record.createdAt >= :periodStart and record.createdAt < :periodEnd
			""")
	Long sumCompletionTokensForPeriod(
			@Param("userId") String userId,
			@Param("periodStart") Instant periodStart,
			@Param("periodEnd") Instant periodEnd);
}
