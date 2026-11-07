package com.ahmed.ai_business_platform.workflow.entity;

import java.time.Instant;

import com.ahmed.ai_business_platform.project.entity.Project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "workflows")
public class Workflow {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(nullable = false, length = 150)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private WorkflowStatus status = WorkflowStatus.PLANNED;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Workflow() {
	}

	public Workflow(String name, Project project) {
		this.name = name;
		this.project = project;
	}

	@PrePersist
	void prePersist() {
		createdAt = Instant.now();
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public WorkflowStatus getStatus() {
		return status;
	}

	public Project getProject() {
		return project;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setStatus(WorkflowStatus status) {
		this.status = status;
	}
}
