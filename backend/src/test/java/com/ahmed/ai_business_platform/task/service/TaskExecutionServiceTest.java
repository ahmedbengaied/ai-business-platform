package com.ahmed.ai_business_platform.task.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import com.ahmed.ai_business_platform.task.entity.AgentRole;
import com.ahmed.ai_business_platform.task.entity.TaskStatus;
import com.ahmed.ai_business_platform.task.entity.WorkflowTask;
import com.ahmed.ai_business_platform.workflow.entity.Workflow;

class TaskExecutionServiceTest {

	@Test
	void retriesTransientFailuresAndCompletesWithinConfiguredAttemptLimit() {
		TaskExecutor executor = mock(TaskExecutor.class);
		when(executor.execute(any(), eq("user-1")))
				.thenThrow(new ResourceAccessException("temporary failure"))
				.thenThrow(new ResourceAccessException("temporary failure"))
				.thenReturn(new AiExecutionResult("completed result", 0, 0));
		TaskExecutionService service = new TaskExecutionService(executor, 3);
		WorkflowTask task = newTask();

		assertTrue(service.execute(task, "user-1"));

		assertEquals(TaskStatus.DONE, task.getStatus());
		assertEquals(3, task.getExecutionAttempts());
		assertEquals("completed result", task.getResult());
		assertNull(task.getLastExecutionFailure());
		verify(executor, times(3)).execute(task, "user-1");
	}

	@Test
	void blocksTaskAfterMaximumAttemptsAndStoresFailureType() {
		TaskExecutor executor = mock(TaskExecutor.class);
		when(executor.execute(any(), eq("user-1")))
				.thenThrow(new ResourceAccessException("temporary failure"));
		TaskExecutionService service = new TaskExecutionService(executor, 3);
		WorkflowTask task = newTask();

		assertFalse(service.execute(task, "user-1"));

		assertEquals(TaskStatus.BLOCKED, task.getStatus());
		assertEquals(3, task.getExecutionAttempts());
		assertEquals("ResourceAccessException", task.getLastExecutionFailure());
		verify(executor, times(3)).execute(task, "user-1");
	}

	@Test
	void doesNotRetryNonRetryableBudgetRejection() {
		TaskExecutor executor = mock(TaskExecutor.class);
		when(executor.execute(any(), eq("user-1")))
				.thenThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Budget reached"));
		TaskExecutionService service = new TaskExecutionService(executor, 3);
		WorkflowTask task = newTask();

		assertFalse(service.execute(task, "user-1"));

		assertEquals(TaskStatus.BLOCKED, task.getStatus());
		assertEquals(1, task.getExecutionAttempts());
		assertEquals("ResponseStatusException", task.getLastExecutionFailure());
		verify(executor, times(1)).execute(task, "user-1");
	}

	private WorkflowTask newTask() {
		return new WorkflowTask("Test task", null, AgentRole.DEVELOPER, new Workflow("Test workflow", null));
	}
}
