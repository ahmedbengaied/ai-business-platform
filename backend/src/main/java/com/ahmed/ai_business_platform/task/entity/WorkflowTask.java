package com.ahmed.ai_business_platform.task.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.ahmed.ai_business_platform.workflow.entity.Workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "workflow_tasks")
public class WorkflowTask {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(length = 4000)
	private String description;

	@Column(length = 4000)
	private String result;

	@Enumerated(EnumType.STRING)
	@Column(name = "agent_role", nullable = false, length = 30)
	private AgentRole agentRole;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TaskStatus status = TaskStatus.TODO;

	@Enumerated(EnumType.STRING)
	@Column(name = "approval_status", nullable = false, length = 20)
	private TaskApprovalStatus approvalStatus = TaskApprovalStatus.PENDING;

	@Column(name = "approval_note", length = 1000)
	private String approvalNote;

	@Column(name = "reviewed_by", length = 320)
	private String reviewedBy;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	@Column(name = "execution_attempts", nullable = false)
	private int executionAttempts;

	@Column(name = "last_execution_failure", length = 200)
	private String lastExecutionFailure;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "workflow_id", nullable = false)
	private Workflow workflow;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "workflow_task_dependencies",
			joinColumns = @JoinColumn(name = "task_id"),
			inverseJoinColumns = @JoinColumn(name = "prerequisite_task_id"))
	private Set<WorkflowTask> prerequisites = new HashSet<>();

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected WorkflowTask() {
	}

	public WorkflowTask(String title, String description, AgentRole agentRole, Workflow workflow) {
		this.title = title;
		this.description = description;
		this.agentRole = agentRole;
		this.workflow = workflow;
	}

	@PrePersist
	void prePersist() {
		createdAt = Instant.now();
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getResult() {
		return result;
	}

	public AgentRole getAgentRole() {
		return agentRole;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public TaskApprovalStatus getApprovalStatus() {
		return approvalStatus;
	}

	public String getApprovalNote() {
		return approvalNote;
	}

	public String getReviewedBy() {
		return reviewedBy;
	}

	public Instant getReviewedAt() {
		return reviewedAt;
	}

	public int getExecutionAttempts() {
		return executionAttempts;
	}

	public String getLastExecutionFailure() {
		return lastExecutionFailure;
	}

	public Workflow getWorkflow() {
		return workflow;
	}

	public Set<WorkflowTask> getPrerequisites() {
		return prerequisites;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setStatus(TaskStatus status) {
		this.status = status;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public void beginExecutionAttempt() {
		executionAttempts++;
		status = TaskStatus.IN_PROGRESS;
	}

	public void recordExecutionFailure(String failureType) {
		lastExecutionFailure = failureType;
	}

	public void completeExecution(String result) {
		this.result = result;
		this.lastExecutionFailure = null;
		this.status = TaskStatus.DONE;
	}

	public void blockAfterExecutionFailure() {
		status = TaskStatus.BLOCKED;
	}

	public void addPrerequisite(WorkflowTask prerequisite) {
		prerequisites.add(prerequisite);
	}

	public void review(TaskApprovalStatus approvalStatus, String reviewedBy, String approvalNote) {
		this.approvalStatus = approvalStatus;
		this.reviewedBy = reviewedBy;
		this.approvalNote = approvalNote;
		this.reviewedAt = Instant.now();
	}
}
