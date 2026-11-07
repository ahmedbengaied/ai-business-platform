package com.ahmed.ai_business_platform.task.service;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ahmed.ai_business_platform.project.service.ProjectAccessService;
import com.ahmed.ai_business_platform.task.dto.AddTaskDependencyRequest;
import com.ahmed.ai_business_platform.task.dto.CreateTaskRequest;
import com.ahmed.ai_business_platform.task.dto.GitWorkspaceResponse;
import com.ahmed.ai_business_platform.task.dto.GitWorkspaceQaReport;
import com.ahmed.ai_business_platform.task.dto.GitWorkspaceSecurityReport;
import com.ahmed.ai_business_platform.task.dto.GitWorkspaceDevOpsReport;
import com.ahmed.ai_business_platform.task.dto.TaskResponse;
import com.ahmed.ai_business_platform.task.dto.UpdateTaskApprovalRequest;
import com.ahmed.ai_business_platform.task.dto.UpdateTaskStatusRequest;
import com.ahmed.ai_business_platform.task.entity.TaskApprovalStatus;
import com.ahmed.ai_business_platform.task.entity.TaskStatus;
import com.ahmed.ai_business_platform.task.entity.WorkflowTask;
import com.ahmed.ai_business_platform.task.repository.WorkflowTaskRepository;
import com.ahmed.ai_business_platform.workflow.entity.Workflow;
import com.ahmed.ai_business_platform.workflow.entity.WorkflowStatus;
import com.ahmed.ai_business_platform.workflow.repository.WorkflowRepository;

@Service
@Transactional(readOnly = true)
public class WorkflowTaskService {

	private final WorkflowTaskRepository taskRepository;
	private final WorkflowRepository workflowRepository;
	private final ProjectAccessService projectAccessService;
	private final TaskExecutionService taskExecutionService;
	private final TaskArtifactService artifactService;
	private final GitWorkspaceService gitWorkspaceService;

	public WorkflowTaskService(
			WorkflowTaskRepository taskRepository,
			WorkflowRepository workflowRepository,
			ProjectAccessService projectAccessService,
			TaskExecutionService taskExecutionService,
			TaskArtifactService artifactService,
			GitWorkspaceService gitWorkspaceService) {
		this.taskRepository = taskRepository;
		this.workflowRepository = workflowRepository;
		this.projectAccessService = projectAccessService;
		this.taskExecutionService = taskExecutionService;
		this.artifactService = artifactService;
		this.gitWorkspaceService = gitWorkspaceService;
	}

	@Transactional
	public TaskResponse create(String email, String projectId, String workflowId, CreateTaskRequest request) {
		Workflow workflow = requireOwnedWorkflow(email, projectId, workflowId);
		if (workflow.getStatus() != WorkflowStatus.PLANNED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Tasks can only be added to PLANNED workflows");
		}
		WorkflowTask task = new WorkflowTask(
				request.title().trim(),
				trimToNull(request.description()),
				request.agentRole(),
				workflow);
		WorkflowTask savedTask = taskRepository.save(task);
		return TaskResponse.from(savedTask, artifactService.fromResult(savedTask.getResult()));
	}

	public List<TaskResponse> list(String email, String projectId, String workflowId) {
		Workflow workflow = requireOwnedWorkflow(email, projectId, workflowId);
		return taskRepository.findAllByWorkflow_IdOrderByCreatedAtAsc(workflow.getId()).stream()
				.map(task -> TaskResponse.from(task, artifactService.fromResult(task.getResult())))
				.toList();
	}

	@Transactional
	public TaskResponse updateStatus(
			String email,
			String projectId,
			String workflowId,
			String taskId,
			UpdateTaskStatusRequest request) {
		Workflow workflow = requireOwnedWorkflow(email, projectId, workflowId);
		WorkflowTask task = taskRepository.findByIdAndWorkflow_Id(taskId, workflow.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		task.setStatus(request.status());
		return TaskResponse.from(task, artifactService.fromResult(task.getResult()));
	}

	@Transactional
	public TaskResponse review(
			String email,
			String projectId,
			String workflowId,
			String taskId,
			UpdateTaskApprovalRequest request) {
		if (request.status() == TaskApprovalStatus.PENDING) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review decision must be APPROVED or REJECTED");
		}
		Workflow workflow = requireOwnedWorkflow(email, projectId, workflowId);
		if (workflow.getStatus() != WorkflowStatus.PLANNED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Tasks can only be reviewed in PLANNED workflows");
		}
		WorkflowTask task = taskRepository.findByIdAndWorkflow_Id(taskId, workflow.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (task.getStatus() != TaskStatus.TODO) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Only TODO tasks can be reviewed");
		}
		task.review(request.status(), email, trimToNull(request.note()));
		return TaskResponse.from(task, artifactService.fromResult(task.getResult()));
	}

	@Transactional
	public TaskResponse addDependency(
			String email,
			String projectId,
			String workflowId,
			String taskId,
			AddTaskDependencyRequest request) {
		Workflow workflow = requireOwnedWorkflow(email, projectId, workflowId);
		if (workflow.getStatus() != WorkflowStatus.PLANNED) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT, "Dependencies can only be changed on PLANNED workflows");
		}
		WorkflowTask task = taskRepository.findByIdAndWorkflow_Id(taskId, workflow.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		WorkflowTask prerequisite = taskRepository.findByIdAndWorkflow_Id(
						request.prerequisiteTaskId(), workflow.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		if (task.getId().equals(prerequisite.getId())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A task cannot depend on itself");
		}
		if (task.getStatus() != TaskStatus.TODO || prerequisite.getStatus() != TaskStatus.TODO) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Dependencies can only be changed for TODO tasks");
		}
		if (wouldCreateCycle(task, prerequisite)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Task dependency would create a cycle");
		}

		task.addPrerequisite(prerequisite);
		return TaskResponse.from(task, artifactService.fromResult(task.getResult()));
	}

	@Transactional
	public TaskResponse execute(String email, String projectId, String workflowId, String taskId) {
		Workflow workflow = requireOwnedWorkflow(email, projectId, workflowId);
		if (workflow.getStatus() != WorkflowStatus.PLANNED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Tasks can only be executed in PLANNED workflows");
		}
		WorkflowTask task = taskRepository.findByIdAndWorkflow_Id(taskId, workflow.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (task.getStatus() != TaskStatus.TODO) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Only TODO tasks can be executed");
		}
		if (task.getApprovalStatus() != TaskApprovalStatus.APPROVED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Task must be approved before execution");
		}
		if (task.getPrerequisites().stream().anyMatch(prerequisite -> prerequisite.getStatus() != TaskStatus.DONE)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Task prerequisites are not complete");
		}

		taskExecutionService.execute(task, workflow.getProject().getOwner().getId());
		return TaskResponse.from(task, artifactService.fromResult(task.getResult()));
	}

	public GitWorkspaceResponse createWorkspace(
			String email, String projectId, String workflowId, String taskId) {
		Workflow workflow = requireOwnedWorkflow(email, projectId, workflowId);
		WorkflowTask task = requireWorkspaceEligibleTask(workflow, taskId);
		return gitWorkspaceService.create(workflow.getProject(), workflow, task.getId());
	}

	public GitWorkspaceResponse getWorkspace(
			String email, String projectId, String workflowId, String taskId) {
		Workflow workflow = requireOwnedWorkflow(email, projectId, workflowId);
		WorkflowTask task = taskRepository.findByIdAndWorkflow_Id(taskId, workflow.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		return gitWorkspaceService.get(workflow.getProject(), workflow, task.getId());
	}

	public GitWorkspaceQaReport inspectWorkspace(
			String email, String projectId, String workflowId, String taskId) {
		Workflow workflow = requireOwnedWorkflow(email, projectId, workflowId);
		WorkflowTask task = taskRepository.findByIdAndWorkflow_Id(taskId, workflow.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		return gitWorkspaceService.inspect(workflow.getProject(), workflow, task.getId());
	}

	public GitWorkspaceSecurityReport scanWorkspace(
			String email, String projectId, String workflowId, String taskId) {
		Workflow workflow = requireOwnedWorkflow(email, projectId, workflowId);
		WorkflowTask task = taskRepository.findByIdAndWorkflow_Id(taskId, workflow.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		return gitWorkspaceService.scanSecurity(workflow.getProject(), workflow, task.getId());
	}

	public GitWorkspaceDevOpsReport inspectDevOps(
			String email, String projectId, String workflowId, String taskId) {
		Workflow workflow = requireOwnedWorkflow(email, projectId, workflowId);
		WorkflowTask task = taskRepository.findByIdAndWorkflow_Id(taskId, workflow.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		return gitWorkspaceService.inspectDevOps(workflow.getProject(), workflow, task.getId());
	}

	@Transactional
	public void removeWorkspace(String email, String projectId, String workflowId, String taskId) {
		Workflow workflow = requireOwnedWorkflow(email, projectId, workflowId);
		WorkflowTask task = taskRepository.findByIdAndWorkflow_Id(taskId, workflow.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		gitWorkspaceService.remove(workflow.getProject(), workflow, task.getId());
	}

	private WorkflowTask requireWorkspaceEligibleTask(Workflow workflow, String taskId) {
		if (workflow.getStatus() != WorkflowStatus.PLANNED) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Workspaces can only be created for PLANNED workflows");
		}
		WorkflowTask task = taskRepository.findByIdAndWorkflow_Id(taskId, workflow.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (task.getStatus() != TaskStatus.TODO || task.getApprovalStatus() != TaskApprovalStatus.APPROVED) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT, "Task must be approved and TODO before creating a workspace");
		}
		return task;
	}

	private boolean wouldCreateCycle(WorkflowTask task, WorkflowTask prerequisite) {
		Set<String> visited = new HashSet<>();
		ArrayDeque<WorkflowTask> pending = new ArrayDeque<>();
		pending.add(prerequisite);
		while (!pending.isEmpty()) {
			WorkflowTask current = pending.removeFirst();
			if (current.getId().equals(task.getId())) {
				return true;
			}
			if (visited.add(current.getId())) {
				pending.addAll(current.getPrerequisites());
			}
		}
		return false;
	}

	private Workflow requireOwnedWorkflow(String email, String projectId, String workflowId) {
		var project = projectAccessService.requireOwnedProject(projectId, email);
		return workflowRepository.findByIdAndProject_Id(workflowId, project.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	private String trimToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
