package com.ahmed.ai_business_platform.task.dto;

import java.time.Instant;
import java.util.List;

public record GitWorkspaceQaReport(
		String projectId,
		String workflowId,
		String taskId,
		String branchName,
		String headCommit,
		boolean clean,
		List<GitWorkspaceChange> changes,
		Instant inspectedAt) {
}
