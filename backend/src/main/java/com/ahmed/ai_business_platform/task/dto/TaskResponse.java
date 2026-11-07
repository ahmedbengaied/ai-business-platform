package com.ahmed.ai_business_platform.task.dto;

import java.time.Instant;
import java.util.List;

import com.ahmed.ai_business_platform.task.entity.AgentRole;
import com.ahmed.ai_business_platform.task.entity.TaskApprovalStatus;
import com.ahmed.ai_business_platform.task.entity.TaskStatus;
import com.ahmed.ai_business_platform.task.entity.WorkflowTask;

public record TaskResponse(
		String id,
		String workflowId,
		String title,
		String description,
		String result,
		TaskArtifact artifact,
		List<String> dependsOnTaskIds,
		AgentRole agentRole,
		TaskStatus status,
		int executionAttempts,
		String lastExecutionFailure,
		TaskApprovalStatus approvalStatus,
		String approvalNote,
		String reviewedBy,
		Instant reviewedAt,
		Instant createdAt) {

	public static TaskResponse from(WorkflowTask task, TaskArtifact artifact) {
		return new TaskResponse(
				task.getId(),
				task.getWorkflow().getId(),
				task.getTitle(),
				task.getDescription(),
				task.getResult(),
				artifact,
				task.getPrerequisites().stream()
						.map(WorkflowTask::getId)
						.sorted()
						.toList(),
				task.getAgentRole(),
				task.getStatus(),
				task.getExecutionAttempts(),
				task.getLastExecutionFailure(),
				task.getApprovalStatus(),
				task.getApprovalNote(),
				task.getReviewedBy(),
				task.getReviewedAt(),
				task.getCreatedAt());
	}
}
