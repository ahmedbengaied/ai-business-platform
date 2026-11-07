package com.ahmed.ai_business_platform.project.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ahmed.ai_business_platform.project.entity.Project;
import com.ahmed.ai_business_platform.project.repository.ProjectRepository;
import com.ahmed.ai_business_platform.user.entity.User;
import com.ahmed.ai_business_platform.user.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class ProjectAccessService {

	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;

	public ProjectAccessService(UserRepository userRepository, ProjectRepository projectRepository) {
		this.userRepository = userRepository;
		this.projectRepository = projectRepository;
	}

	public User requireUser(String email) {
		return userRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}

	public Project requireOwnedProject(String projectId, String email) {
		User owner = requireUser(email);
		return projectRepository.findByIdAndOwner_Id(projectId, owner.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}
}
