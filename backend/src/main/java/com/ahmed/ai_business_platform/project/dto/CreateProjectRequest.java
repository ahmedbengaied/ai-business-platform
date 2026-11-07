package com.ahmed.ai_business_platform.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
		@NotBlank @Size(max = 150) String name,
		@Size(max = 2000) String description,
		@Size(max = 500) String repositoryPath) {
}
