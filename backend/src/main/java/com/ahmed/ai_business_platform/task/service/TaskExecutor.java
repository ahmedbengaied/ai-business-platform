package com.ahmed.ai_business_platform.task.service;

import com.ahmed.ai_business_platform.task.entity.WorkflowTask;

public interface TaskExecutor {

	AiExecutionResult execute(WorkflowTask task, String userId);
}
