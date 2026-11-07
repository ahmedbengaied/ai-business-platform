package com.ahmed.ai_business_platform.task.dto;

import com.ahmed.ai_business_platform.task.entity.AgentRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
		@NotBlank @Size(max = 200) String title,
		@Size(max = 4000) String description,
		@NotNull AgentRole agentRole) {
}
