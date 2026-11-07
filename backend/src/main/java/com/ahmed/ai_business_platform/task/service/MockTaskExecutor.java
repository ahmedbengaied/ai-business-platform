package com.ahmed.ai_business_platform.task.service;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.ahmed.ai_business_platform.task.entity.WorkflowTask;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockTaskExecutor implements TaskExecutor {

	@Override
	public AiExecutionResult execute(WorkflowTask task, String userId) {
		return new AiExecutionResult(
				"Mock " + task.getAgentRole() + " agent completed task: " + task.getTitle()
						+ ". No AI model was called.",
				0,
				0);
	}
}
