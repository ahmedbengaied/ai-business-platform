package com.ahmed.ai_business_platform.task.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.ahmed.ai_business_platform.project.entity.Project;
import com.ahmed.ai_business_platform.task.dto.GitWorkspaceDevOpsReport;
import com.ahmed.ai_business_platform.workflow.entity.Workflow;

@Service
public class WorkspaceDevOpsInspector {

	private static final List<String> CONTAINER_FILES = List.of(
			"Dockerfile", "docker-compose.yml", "docker-compose.yaml", "compose.yml", "compose.yaml");
	private static final List<String> DEPLOYMENT_FILES = List.of(
			"fly.toml",
			"render.yaml",
			"railway.toml",
			"railway.json",
			"vercel.json",
			"Procfile",
			"k8s/deployment.yaml",
			"k8s/deployment.yml",
			"kubernetes/deployment.yaml",
			"kubernetes/deployment.yml",
			"helm/Chart.yaml");
	private static final List<String> CONFIGURATION_EXAMPLES = List.of(
			".env.example", ".env.sample", "application-prod.properties", "application-production.properties");

	public GitWorkspaceDevOpsReport inspect(
			Path workspace, Project project, Workflow workflow, String taskId) {
		Path root;
		try {
			root = workspace.toRealPath();
		} catch (IOException exception) {
			throw new IllegalStateException("Could not resolve the task workspace for DevOps inspection", exception);
		}

		List<String> containerFiles = existingFiles(root, CONTAINER_FILES);
		List<String> ciWorkflowFiles = listCiWorkflows(root);
		List<String> deploymentFiles = existingFiles(root, DEPLOYMENT_FILES);
		List<String> configurationExamples = existingFiles(root, CONFIGURATION_EXAMPLES);
		List<String> recommendations = new ArrayList<>();
		if (containerFiles.isEmpty()) {
			recommendations.add("No Dockerfile or Compose configuration was found.");
		}
		if (ciWorkflowFiles.isEmpty()) {
			recommendations.add("No GitHub Actions workflow file was found under .github/workflows.");
		}
		if (deploymentFiles.isEmpty()) {
			recommendations.add("No common deployment manifest was found.");
		}
		if (configurationExamples.isEmpty()) {
			recommendations.add("No example production configuration file was found.");
		}

		return new GitWorkspaceDevOpsReport(
				project.getId(),
				workflow.getId(),
				taskId,
				containerFiles,
				ciWorkflowFiles,
				deploymentFiles,
				configurationExamples,
				recommendations,
				Instant.now());
	}

	private List<String> existingFiles(Path root, List<String> candidates) {
		return candidates.stream()
				.filter(candidate -> isRegularFile(root.resolve(candidate)))
				.toList();
	}

	private List<String> listCiWorkflows(Path root) {
		Path workflowDirectory = root.resolve(".github").resolve("workflows");
		if (!Files.exists(workflowDirectory, LinkOption.NOFOLLOW_LINKS)) {
			return List.of();
		}
		if (Files.isSymbolicLink(workflowDirectory) || !Files.isDirectory(workflowDirectory, LinkOption.NOFOLLOW_LINKS)) {
			throw new IllegalStateException("GitHub Actions workflow path is not a regular directory");
		}
		try (Stream<Path> entries = Files.list(workflowDirectory)) {
			return entries
					.filter(path -> !Files.isSymbolicLink(path))
					.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
					.filter(path -> path.getFileName().toString().endsWith(".yml")
							|| path.getFileName().toString().endsWith(".yaml"))
					.map(path -> root.relativize(path).toString())
					.sorted()
					.toList();
		} catch (IOException exception) {
			throw new IllegalStateException("Could not inspect GitHub Actions workflow files", exception);
		}
	}

	private boolean isRegularFile(Path file) {
		return !Files.isSymbolicLink(file) && Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS);
	}
}
