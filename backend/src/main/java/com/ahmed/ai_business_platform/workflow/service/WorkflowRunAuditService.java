package com.ahmed.ai_business_platform.workflow.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ahmed.ai_business_platform.workflow.entity.WorkflowRun;
import com.ahmed.ai_business_platform.workflow.repository.WorkflowRunRepository;

@Service
public class WorkflowRunAuditService {

	private final WorkflowRunRepository runRepository;

	public WorkflowRunAuditService(WorkflowRunRepository runRepository) {
		this.runRepository = runRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public WorkflowRun start(String workflowId, String startedBy) {
		return runRepository.saveAndFlush(new WorkflowRun(workflowId, startedBy));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void complete(String runId, int completedTaskCount) {
		WorkflowRun run = requireRun(runId);
		run.complete(completedTaskCount);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void fail(String runId, int completedTaskCount, String failureReason) {
		WorkflowRun run = requireRun(runId);
		run.fail(completedTaskCount, failureReason);
	}

	private WorkflowRun requireRun(String runId) {
		return runRepository.findById(runId)
				.orElseThrow(() -> new IllegalStateException("Workflow run audit record was not found"));
	}
}
