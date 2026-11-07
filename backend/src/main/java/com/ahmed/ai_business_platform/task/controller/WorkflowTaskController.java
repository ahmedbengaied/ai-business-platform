package com.ahmed.ai_business_platform.task.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ahmed.ai_business_platform.task.dto.AddTaskDependencyRequest;
import com.ahmed.ai_business_platform.task.dto.CreateTaskRequest;
import com.ahmed.ai_business_platform.task.dto.GitWorkspaceResponse;
import com.ahmed.ai_business_platform.task.dto.GitWorkspaceQaReport;
import com.ahmed.ai_business_platform.task.dto.GitWorkspaceSecurityReport;
import com.ahmed.ai_business_platform.task.dto.GitWorkspaceDevOpsReport;
import com.ahmed.ai_business_platform.task.dto.TaskResponse;
import com.ahmed.ai_business_platform.task.dto.UpdateTaskApprovalRequest;
import com.ahmed.ai_business_platform.task.dto.UpdateTaskStatusRequest;
import com.ahmed.ai_business_platform.task.service.WorkflowTaskService;
import com.ahmed.ai_business_platform.firm.service.FirmAccessService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/workflows/{workflowId}/tasks")
@Validated
public class WorkflowTaskController {

	private final WorkflowTaskService taskService;
	private final FirmAccessService firmAccess;

	public WorkflowTaskController(WorkflowTaskService taskService, FirmAccessService firmAccess) {
		this.taskService = taskService;
		this.firmAccess = firmAccess;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TaskResponse create(
			Authentication authentication,
			@PathVariable String projectId,
			@PathVariable String workflowId,
			@Valid @RequestBody CreateTaskRequest request) {
		firmAccess.requireCanWrite(authentication.getName());
		return taskService.create(authentication.getName(), projectId, workflowId, request);
	}

	@GetMapping
	public List<TaskResponse> list(
			Authentication authentication,
			@PathVariable String projectId,
			@PathVariable String workflowId) {
		return taskService.list(authentication.getName(), projectId, workflowId);
	}

	@PostMapping("/{taskId}/dependencies")
	public TaskResponse addDependency(
			Authentication authentication,
			@PathVariable String projectId,
			@PathVariable String workflowId,
			@PathVariable String taskId,
			@Valid @RequestBody AddTaskDependencyRequest request) {
		firmAccess.requireCanWrite(authentication.getName());
		return taskService.addDependency(
				authentication.getName(), projectId, workflowId, taskId, request);
	}

	@PatchMapping("/{taskId}/approval")
	public TaskResponse review(
			Authentication authentication,
			@PathVariable String projectId,
			@PathVariable String workflowId,
			@PathVariable String taskId,
			@Valid @RequestBody UpdateTaskApprovalRequest request) {
		firmAccess.requireCanWrite(authentication.getName());
		return taskService.review(authentication.getName(), projectId, workflowId, taskId, request);
	}

	@PostMapping("/{taskId}/execute")
	public TaskResponse execute(
			Authentication authentication,
			@PathVariable String projectId,
			@PathVariable String workflowId,
			@PathVariable String taskId) {
		firmAccess.requireCanWrite(authentication.getName());
		return taskService.execute(authentication.getName(), projectId, workflowId, taskId);
	}

	@PostMapping("/{taskId}/workspace")
	@ResponseStatus(HttpStatus.CREATED)
	public GitWorkspaceResponse createWorkspace(
			Authentication authentication,
			@PathVariable String projectId,
			@PathVariable String workflowId,
			@PathVariable String taskId) {
		firmAccess.requireCanWrite(authentication.getName());
		return taskService.createWorkspace(authentication.getName(), projectId, workflowId, taskId);
	}

	@GetMapping("/{taskId}/workspace")
	public GitWorkspaceResponse getWorkspace(
			Authentication authentication,
			@PathVariable String projectId,
			@PathVariable String workflowId,
			@PathVariable String taskId) {
		return taskService.getWorkspace(authentication.getName(), projectId, workflowId, taskId);
	}

	@GetMapping("/{taskId}/workspace/qa")
	public GitWorkspaceQaReport inspectWorkspace(
			Authentication authentication,
			@PathVariable String projectId,
			@PathVariable String workflowId,
			@PathVariable String taskId) {
		return taskService.inspectWorkspace(authentication.getName(), projectId, workflowId, taskId);
	}

	@GetMapping("/{taskId}/workspace/security")
	public GitWorkspaceSecurityReport scanWorkspace(
			Authentication authentication,
			@PathVariable String projectId,
			@PathVariable String workflowId,
			@PathVariable String taskId) {
		return taskService.scanWorkspace(authentication.getName(), projectId, workflowId, taskId);
	}

	@GetMapping("/{taskId}/workspace/devops")
	public GitWorkspaceDevOpsReport inspectDevOps(
			Authentication authentication,
			@PathVariable String projectId,
			@PathVariable String workflowId,
			@PathVariable String taskId) {
		return taskService.inspectDevOps(authentication.getName(), projectId, workflowId, taskId);
	}

	@DeleteMapping("/{taskId}/workspace")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeWorkspace(
			Authentication authentication,
			@PathVariable String projectId,
			@PathVariable String workflowId,
			@PathVariable String taskId) {
		firmAccess.requireCanWrite(authentication.getName());
		taskService.removeWorkspace(authentication.getName(), projectId, workflowId, taskId);
	}

	@PatchMapping("/{taskId}/status")
	public TaskResponse updateStatus(
			Authentication authentication,
			@PathVariable String projectId,
			@PathVariable String workflowId,
			@PathVariable String taskId,
			@Valid @RequestBody UpdateTaskStatusRequest request) {
		firmAccess.requireCanWrite(authentication.getName());
		return taskService.updateStatus(authentication.getName(), projectId, workflowId, taskId, request);
	}
}
