package com.ahmed.ai_business_platform.project.entity;

import java.time.Instant;

import com.ahmed.ai_business_platform.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class Project {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(length = 2000)
	private String description;

	@Column(name = "repository_path", length = 500)
	private String repositoryPath;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private User owner;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected Project() {
	}

	public Project(String name, String description, String repositoryPath, User owner) {
		this.name = name;
		this.description = description;
		this.repositoryPath = repositoryPath;
		this.owner = owner;
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

	public String getDescription() {
		return description;
	}

	public String getRepositoryPath() {
		return repositoryPath;
	}

	public User getOwner() {
		return owner;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
