package com.ahmed.ai_business_platform.workflow.dto;

import com.ahmed.ai_business_platform.workflow.entity.WorkflowStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateWorkflowStatusRequest(@NotNull WorkflowStatus status) {
}
