package com.ahmed.ai_business_platform.project.dto;

import java.time.Instant;

import com.ahmed.ai_business_platform.project.entity.Project;

public record ProjectResponse(
		String id,
		String name,
		String description,
		String repositoryPath,
		Instant createdAt) {

	public static ProjectResponse from(Project project) {
		return new ProjectResponse(
				project.getId(),
				project.getName(),
				project.getDescription(),
				project.getRepositoryPath(),
				project.getCreatedAt());
	}
}
