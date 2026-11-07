package com.ahmed.ai_business_platform.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkflowRequest(@NotBlank @Size(max = 150) String name) {
}
