package com.ahmed.ai_business_platform.workflow.dto;

import java.time.Instant;

import com.ahmed.ai_business_platform.workflow.entity.Workflow;
import com.ahmed.ai_business_platform.workflow.entity.WorkflowStatus;

public record WorkflowResponse(
		String id,
		String projectId,
		String name,
		WorkflowStatus status,
		Instant createdAt) {

	public static WorkflowResponse from(Workflow workflow) {
		return new WorkflowResponse(
				workflow.getId(),
				workflow.getProject().getId(),
				workflow.getName(),
				workflow.getStatus(),
				workflow.getCreatedAt());
	}
}
