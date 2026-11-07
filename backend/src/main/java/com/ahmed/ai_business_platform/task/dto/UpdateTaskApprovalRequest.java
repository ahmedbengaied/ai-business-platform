package com.ahmed.ai_business_platform.task.dto;

import com.ahmed.ai_business_platform.task.entity.TaskApprovalStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTaskApprovalRequest(
		@NotNull TaskApprovalStatus status,
		@Size(max = 1000) String note) {
}
