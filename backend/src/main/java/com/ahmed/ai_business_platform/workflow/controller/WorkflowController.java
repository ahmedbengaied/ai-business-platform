package com.ahmed.ai_business_platform.workflow.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ahmed.ai_business_platform.workflow.dto.CreateWorkflowRequest;
import com.ahmed.ai_business_platform.workflow.dto.UpdateWorkflowStatusRequest;
import com.ahmed.ai_business_platform.workflow.dto.WorkflowRunResponse;
import com.ahmed.ai_business_platform.workflow.dto.WorkflowResponse;
import com.ahmed.ai_business_platform.workflow.service.WorkflowService;
import com.ahmed.ai_business_platform.firm.service.FirmAccessService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/workflows")
@Validated
public class WorkflowController {

	private final WorkflowService workflowService;
	private final FirmAccessService firmAccess;

	public WorkflowController(WorkflowService workflowService, FirmAccessService firmAccess) {
		this.workflowService = workflowService;
		this.firmAccess = firmAccess;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public WorkflowResponse create(
			Authentication authentication,
			@PathVariable String projectId,
			@Valid @RequestBody CreateWorkflowRequest request) {
		firmAccess.requireCanWrite(authentication.getName());
		return workflowService.create(authentication.getName(), projectId, request);
	}

	@GetMapping
	public List<WorkflowResponse> list(Authentication authentication, @PathVariable String projectId) {
		return workflowService.list(authentication.getName(), projectId);
	}

	@PostMapping("/{workflowId}/execute")
	public WorkflowResponse execute(
			Authentication authentication,
			@PathVariable String projectId,
			@PathVariable String workflowId) {
		firmAccess.requireCanWrite(authentication.getName());
		return workflowService.execute(authentication.getName(), projectId, workflowId);
	}

	@GetMapping("/{workflowId}/runs")
	public List<WorkflowRunResponse> listRuns(
			Authentication authentication,
			@PathVariable String projectId,
			@PathVariable String workflowId) {
		return workflowService.getRunHistory(authentication.getName(), projectId, workflowId);
	}

	@PatchMapping("/{workflowId}/status")
	public WorkflowResponse updateStatus(
			Authentication authentication,
			@PathVariable String projectId,
			@PathVariable String workflowId,
			@Valid @RequestBody UpdateWorkflowStatusRequest request) {
		firmAccess.requireCanWrite(authentication.getName());
		return workflowService.updateStatus(authentication.getName(), projectId, workflowId, request);
	}
}
