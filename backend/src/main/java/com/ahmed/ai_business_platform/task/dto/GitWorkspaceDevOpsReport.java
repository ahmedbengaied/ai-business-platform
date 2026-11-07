package com.ahmed.ai_business_platform.task.dto;

import java.time.Instant;
import java.util.List;

public record GitWorkspaceDevOpsReport(
		String projectId,
		String workflowId,
		String taskId,
		List<String> containerFiles,
		List<String> ciWorkflowFiles,
		List<String> deploymentFiles,
		List<String> configurationExamples,
		List<String> recommendations,
		Instant inspectedAt) {
}
