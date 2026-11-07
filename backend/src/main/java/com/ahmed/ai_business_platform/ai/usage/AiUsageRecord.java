package com.ahmed.ai_business_platform.ai.usage;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_usage_records")
public class AiUsageRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(name = "user_id", nullable = false, length = 255, updatable = false)
	private String userId;

	@Column(name = "task_id", nullable = false, length = 255, updatable = false)
	private String taskId;

	@Column(nullable = false, length = 100, updatable = false)
	private String model;

	@Column(name = "prompt_tokens", nullable = false, updatable = false)
	private long promptTokens;

	@Column(name = "completion_tokens", nullable = false, updatable = false)
	private long completionTokens;

	@Column(name = "cost_usd", nullable = false, precision = 12, scale = 8, updatable = false)
	private BigDecimal costUsd;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected AiUsageRecord() {
	}

	public AiUsageRecord(
			String userId,
			String taskId,
			String model,
			long promptTokens,
			long completionTokens,
			BigDecimal costUsd) {
		this.userId = userId;
		this.taskId = taskId;
		this.model = model;
		this.promptTokens = promptTokens;
		this.completionTokens = completionTokens;
		this.costUsd = costUsd;
	}

	@PrePersist
	void prePersist() {
		createdAt = Instant.now();
	}

	public String getId() {
		return id;
	}

	public String getUserId() {
		return userId;
	}

	public String getTaskId() {
		return taskId;
	}

	public String getModel() {
		return model;
	}

	public long getPromptTokens() {
		return promptTokens;
	}

	public long getCompletionTokens() {
		return completionTokens;
	}

	public BigDecimal getCostUsd() {
		return costUsd;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
