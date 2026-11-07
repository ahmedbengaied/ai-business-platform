package com.ahmed.ai_business_platform.project.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ahmed.ai_business_platform.project.dto.CreateProjectRequest;
import com.ahmed.ai_business_platform.project.dto.ProjectResponse;
import com.ahmed.ai_business_platform.project.entity.Project;
import com.ahmed.ai_business_platform.project.repository.ProjectRepository;
import com.ahmed.ai_business_platform.task.service.GitWorkspaceService;

@Service
@Transactional(readOnly = true)
public class ProjectService {

	private final ProjectRepository projectRepository;
	private final ProjectAccessService projectAccessService;
	private final GitWorkspaceService gitWorkspaceService;

	public ProjectService(
			ProjectRepository projectRepository,
			ProjectAccessService projectAccessService,
			GitWorkspaceService gitWorkspaceService) {
		this.projectRepository = projectRepository;
		this.projectAccessService = projectAccessService;
		this.gitWorkspaceService = gitWorkspaceService;
	}

	@Transactional
	public ProjectResponse create(String email, CreateProjectRequest request) {
		String repositoryPath = trimToNull(request.repositoryPath());
		if (repositoryPath != null) {
			gitWorkspaceService.validateRepositoryPath(repositoryPath);
		}
		Project project = new Project(
				request.name().trim(),
				trimToNull(request.description()),
				repositoryPath,
				projectAccessService.requireUser(email));
		return ProjectResponse.from(projectRepository.save(project));
	}

	public List<ProjectResponse> list(String email) {
		String ownerId = projectAccessService.requireUser(email).getId();
		return projectRepository.findAllByOwner_IdOrderByCreatedAtDesc(ownerId).stream()
				.map(ProjectResponse::from)
				.toList();
	}

	public ProjectResponse get(String email, String projectId) {
		return ProjectResponse.from(projectAccessService.requireOwnedProject(projectId, email));
	}

	private String trimToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
