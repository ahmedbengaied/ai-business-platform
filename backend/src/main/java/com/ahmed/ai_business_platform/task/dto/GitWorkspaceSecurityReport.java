package com.ahmed.ai_business_platform.task.dto;

import java.time.Instant;
import java.util.List;

public record GitWorkspaceSecurityReport(
		String projectId,
		String workflowId,
		String taskId,
		List<SecurityFinding> findings,
		int scannedFiles,
		int skippedFiles,
		boolean complete,
		Instant inspectedAt) {
}
