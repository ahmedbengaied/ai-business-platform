package com.ahmed.ai_business_platform.workflow.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "workflow_runs")
public class WorkflowRun {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(name = "workflow_id", nullable = false, updatable = false)
	private String workflowId;

	@Column(name = "started_by", nullable = false, length = 320, updatable = false)
	private String startedBy;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private WorkflowRunStatus status = WorkflowRunStatus.RUNNING;

	@Column(name = "started_at", nullable = false, updatable = false)
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "completed_task_count", nullable = false)
	private int completedTaskCount;

	@Column(name = "failure_reason", length = 200)
	private String failureReason;

	protected WorkflowRun() {
	}

	public WorkflowRun(String workflowId, String startedBy) {
		this.workflowId = workflowId;
		this.startedBy = startedBy;
	}

	@PrePersist
	void prePersist() {
		startedAt = Instant.now();
	}

	public String getId() {
		return id;
	}

	public String getWorkflowId() {
		return workflowId;
	}

	public String getStartedBy() {
		return startedBy;
	}

	public WorkflowRunStatus getStatus() {
		return status;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public int getCompletedTaskCount() {
		return completedTaskCount;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public void complete(int completedTaskCount) {
		this.status = WorkflowRunStatus.COMPLETED;
		this.completedTaskCount = completedTaskCount;
		this.completedAt = Instant.now();
	}

	public void fail(int completedTaskCount, String failureReason) {
		this.status = WorkflowRunStatus.FAILED;
		this.completedTaskCount = completedTaskCount;
		this.failureReason = failureReason;
		this.completedAt = Instant.now();
	}
}
