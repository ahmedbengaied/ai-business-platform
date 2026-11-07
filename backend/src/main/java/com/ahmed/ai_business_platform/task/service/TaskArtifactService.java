package com.ahmed.ai_business_platform.task.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ahmed.ai_business_platform.task.dto.TaskArtifact;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class TaskArtifactService {

	private final ObjectMapper objectMapper;

	public TaskArtifactService(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public TaskArtifact fromResult(String result) {
		if (result == null || result.isBlank()) {
			return null;
		}
		if (!result.stripLeading().startsWith("{")) {
			return new TaskArtifact(result, List.of());
		}

		TaskArtifact artifact;
		try {
			artifact = objectMapper.readValue(result, TaskArtifact.class);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Stored task result is not valid artifact JSON", exception);
		}
		if (artifact.summary() == null || artifact.summary().isBlank()
				|| artifact.nextSteps() == null
				|| artifact.nextSteps().stream().anyMatch(step -> step == null || step.isBlank())) {
			throw new IllegalStateException("Stored task result does not match the artifact schema");
		}
		return artifact;
	}
}
