package com.ahmed.ai_business_platform.task.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

public record TaskArtifact(
		String summary,
		@JsonAlias("next_steps") List<String> nextSteps) {
}
