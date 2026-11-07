package com.ahmed.ai_business_platform.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddTaskDependencyRequest(
		@NotBlank @Size(max = 255) String prerequisiteTaskId) {
}
