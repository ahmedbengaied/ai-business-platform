package com.ahmed.ai_business_platform.workflow.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ahmed.ai_business_platform.project.service.ProjectAccessService;
import com.ahmed.ai_business_platform.task.entity.TaskApprovalStatus;
import com.ahmed.ai_business_platform.task.entity.TaskStatus;
import com.ahmed.ai_business_platform.task.entity.WorkflowTask;
import com.ahmed.ai_business_platform.task.repository.WorkflowTaskRepository;
import com.ahmed.ai_business_platform.task.service.TaskExecutionService;
import com.ahmed.ai_business_platform.workflow.dto.CreateWorkflowRequest;
import com.ahmed.ai_business_platform.workflow.dto.UpdateWorkflowStatusRequest;
import com.ahmed.ai_business_platform.workflow.dto.WorkflowRunResponse;
import com.ahmed.ai_business_platform.workflow.dto.WorkflowResponse;
import com.ahmed.ai_business_platform.workflow.entity.Workflow;
import com.ahmed.ai_business_platform.workflow.entity.WorkflowRun;
import com.ahmed.ai_business_platform.workflow.entity.WorkflowStatus;
import com.ahmed.ai_business_platform.workflow.repository.WorkflowRunRepository;
import com.ahmed.ai_business_platform.workflow.repository.WorkflowRepository;

@Service
@Transactional(readOnly = true)
public class WorkflowService {

	private final WorkflowRepository workflowRepository;
	private final ProjectAccessService projectAccessService;
	private final WorkflowTaskRepository taskRepository;
	private final TaskExecutionService taskExecutionService;
	private final WorkflowRunAuditService runAuditService;
	private final WorkflowRunRepository runRepository;

	public WorkflowService(
			WorkflowRepository workflowRepository,
			ProjectAccessService projectAccessService,
			WorkflowTaskRepository taskRepository,
			TaskExecutionService taskExecutionService,
			WorkflowRunAuditService runAuditService,
			WorkflowRunRepository runRepository) {
		this.workflowRepository = workflowRepository;
		this.projectAccessService = projectAccessService;
		this.taskRepository = taskRepository;
		this.taskExecutionService = taskExecutionService;
		this.runAuditService = runAuditService;
		this.runRepository = runRepository;
	}

	@Transactional
	public WorkflowResponse create(String email, String projectId, CreateWorkflowRequest request) {
		var project = projectAccessService.requireOwnedProject(projectId, email);
		return WorkflowResponse.from(workflowRepository.save(new Workflow(request.name().trim(), project)));
	}

	public List<WorkflowResponse> list(String email, String projectId) {
		var project = projectAccessService.requireOwnedProject(projectId, email);
		return workflowRepository.findAllByProject_IdOrderByCreatedAtAsc(project.getId()).stream()
				.map(WorkflowResponse::from)
				.toList();
	}

	@Transactional
	public WorkflowResponse updateStatus(
			String email,
			String projectId,
			String workflowId,
			UpdateWorkflowStatusRequest request) {
		var project = projectAccessService.requireOwnedProject(projectId, email);
		Workflow workflow = workflowRepository.findByIdAndProject_Id(workflowId, project.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		workflow.setStatus(request.status());
		return WorkflowResponse.from(workflow);
	}

	@Transactional
	public WorkflowResponse execute(String email, String projectId, String workflowId) {
		var project = projectAccessService.requireOwnedProject(projectId, email);
		Workflow workflow = workflowRepository.findByIdAndProject_Id(workflowId, project.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (workflow.getStatus() != WorkflowStatus.PLANNED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Only PLANNED workflows can be executed");
		}

		List<WorkflowTask> tasks = taskRepository.findAllByWorkflow_IdOrderByCreatedAtAsc(workflow.getId());
		if (tasks.stream().anyMatch(task -> task.getStatus() != TaskStatus.TODO
				&& task.getStatus() != TaskStatus.DONE)) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"Workflow contains tasks that are not ready to execute");
		}
		if (tasks.stream().anyMatch(task -> task.getApprovalStatus() != TaskApprovalStatus.APPROVED)) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT, "All workflow tasks must be approved before execution");
		}

		WorkflowRun run = runAuditService.start(workflow.getId(), email);
		workflow.setStatus(WorkflowStatus.RUNNING);
		int completedTasks = 0;
		for (WorkflowTask task : orderTasksByDependencies(tasks)) {
			if (task.getStatus() == TaskStatus.TODO) {
				if (taskExecutionService.execute(task, workflow.getProject().getOwner().getId())) {
					completedTasks++;
				} else {
					String failureReason = task.getLastExecutionFailure();
					workflow.setStatus(WorkflowStatus.FAILED);
					runAuditService.fail(run.getId(), completedTasks, failureReason);
					return WorkflowResponse.from(workflow);
				}
			}
		}
		workflow.setStatus(WorkflowStatus.COMPLETED);
		runAuditService.complete(run.getId(), completedTasks);
		return WorkflowResponse.from(workflow);
	}

	private List<WorkflowTask> orderTasksByDependencies(List<WorkflowTask> tasks) {
		List<WorkflowTask> pending = new ArrayList<>(tasks);
		List<WorkflowTask> ordered = new ArrayList<>(tasks.size());
		Set<String> resolvedTaskIds = new HashSet<>();

		while (!pending.isEmpty()) {
			WorkflowTask readyTask = pending.stream()
					.filter(task -> task.getPrerequisites().stream()
							.allMatch(prerequisite -> resolvedTaskIds.contains(prerequisite.getId())))
					.findFirst()
					.orElseThrow(() -> new ResponseStatusException(
							HttpStatus.CONFLICT, "Workflow task dependencies contain a cycle"));
			pending.remove(readyTask);
			ordered.add(readyTask);
			resolvedTaskIds.add(readyTask.getId());
		}
		return ordered;
	}

	public List<WorkflowRun> listRuns(String email, String projectId, String workflowId) {
		Workflow workflow = requireOwnedWorkflow(email, projectId, workflowId);
		return runRepository.findAllByWorkflowIdOrderByStartedAtDesc(workflow.getId());
	}

	public List<WorkflowRunResponse> getRunHistory(String email, String projectId, String workflowId) {
		return listRuns(email, projectId, workflowId).stream()
				.map(WorkflowRunResponse::from)
				.toList();
	}

	private Workflow requireOwnedWorkflow(String email, String projectId, String workflowId) {
		var project = projectAccessService.requireOwnedProject(projectId, email);
		return workflowRepository.findByIdAndProject_Id(workflowId, project.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}
}
