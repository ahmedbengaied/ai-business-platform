package com.ahmed.ai_business_platform.workflow.dto;

import java.time.Instant;

import com.ahmed.ai_business_platform.workflow.entity.WorkflowRun;
import com.ahmed.ai_business_platform.workflow.entity.WorkflowRunStatus;

public record WorkflowRunResponse(
		String id,
		String workflowId,
		String startedBy,
		WorkflowRunStatus status,
		Instant startedAt,
		Instant completedAt,
		int completedTaskCount,
		String failureReason) {

	public static WorkflowRunResponse from(WorkflowRun run) {
		return new WorkflowRunResponse(
				run.getId(),
				run.getWorkflowId(),
				run.getStartedBy(),
				run.getStatus(),
				run.getStartedAt(),
				run.getCompletedAt(),
				run.getCompletedTaskCount(),
				run.getFailureReason());
	}
}
