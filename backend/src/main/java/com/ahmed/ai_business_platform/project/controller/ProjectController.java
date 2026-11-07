package com.ahmed.ai_business_platform.project.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ahmed.ai_business_platform.project.dto.CreateProjectRequest;
import com.ahmed.ai_business_platform.project.dto.ProjectResponse;
import com.ahmed.ai_business_platform.project.service.ProjectService;
import com.ahmed.ai_business_platform.firm.service.FirmAccessService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/projects")
@Validated
public class ProjectController {

	private final ProjectService projectService;
	private final FirmAccessService firmAccess;

	public ProjectController(ProjectService projectService, FirmAccessService firmAccess) {
		this.projectService = projectService;
		this.firmAccess = firmAccess;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProjectResponse create(Authentication authentication, @Valid @RequestBody CreateProjectRequest request) {
		firmAccess.requireCanWrite(authentication.getName());
		return projectService.create(authentication.getName(), request);
	}

	@GetMapping
	public List<ProjectResponse> list(Authentication authentication) {
		return projectService.list(authentication.getName());
	}

	@GetMapping("/{projectId}")
	public ProjectResponse get(Authentication authentication, @PathVariable String projectId) {
		return projectService.get(authentication.getName(), projectId);
	}
}
