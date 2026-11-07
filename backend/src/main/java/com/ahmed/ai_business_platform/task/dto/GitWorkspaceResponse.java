package com.ahmed.ai_business_platform.task.dto;

public record GitWorkspaceResponse(
		String projectId,
		String workflowId,
		String taskId,
		String branchName,
		String repositoryPath,
		String workspacePath) {
}
