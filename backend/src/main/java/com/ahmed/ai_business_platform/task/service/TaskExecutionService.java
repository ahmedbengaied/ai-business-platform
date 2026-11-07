package com.ahmed.ai_business_platform.task.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.ahmed.ai_business_platform.task.entity.WorkflowTask;

@Service
public class TaskExecutionService {

	private final TaskExecutor taskExecutor;
	private final int maxAttempts;

	public TaskExecutionService(
			TaskExecutor taskExecutor,
			@Value("${app.tasks.max-execution-attempts:3}") int maxAttempts) {
		if (maxAttempts < 1) {
			throw new IllegalArgumentException("Maximum task execution attempts must be at least 1");
		}
		this.taskExecutor = taskExecutor;
		this.maxAttempts = maxAttempts;
	}

	public boolean execute(WorkflowTask task, String userId) {
		while (task.getExecutionAttempts() < maxAttempts) {
			task.beginExecutionAttempt();
			try {
				AiExecutionResult result = taskExecutor.execute(task, userId);
				task.completeExecution(result.result());
				return true;
			} catch (RuntimeException exception) {
				task.recordExecutionFailure(exception.getClass().getSimpleName());
				if (!isRetryable(exception)) {
					break;
				}
			}
		}
		task.blockAfterExecutionFailure();
		return false;
	}

	private boolean isRetryable(RuntimeException exception) {
		if (exception instanceof ResponseStatusException) {
			return false;
		}
		if (exception instanceof RestClientResponseException responseException) {
			return responseException.getStatusCode().is5xxServerError()
					|| responseException.getStatusCode().value() == 429;
		}
		return exception instanceof ResourceAccessException;
	}
}
