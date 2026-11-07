package com.ahmed.ai_business_platform.task.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;

import com.ahmed.ai_business_platform.project.entity.Project;
import com.ahmed.ai_business_platform.firm.entity.AccountingFirm;
import com.ahmed.ai_business_platform.firm.entity.FirmRole;
import com.ahmed.ai_business_platform.task.entity.AgentRole;
import com.ahmed.ai_business_platform.user.entity.User;
import com.ahmed.ai_business_platform.workflow.entity.Workflow;

class GitWorkspaceServiceTest {

	@TempDir
	Path temporaryDirectory;

	@Test
	void createsGetsAndRemovesTaskWorktreeIdempotently() throws IOException {
		Path repositoriesRoot = Files.createDirectories(temporaryDirectory.resolve("repositories"));
		Path repository = initializeRepository(repositoriesRoot.resolve("sample"));
		Path worktreesRoot = temporaryDirectory.resolve("worktrees");
		GitWorkspaceService service = new GitWorkspaceService(
				repositoriesRoot.toString(),
				worktreesRoot.toString(),
				new WorkspaceSecretScanner(),
				new WorkspaceDevOpsInspector());
		String storedPath = service.validateRepositoryPath("sample");
		Project project = new Project(
				"Project", null, storedPath,
				new User("test@example.com", "hash", "Test", "User", new AccountingFirm("Test Firm"), FirmRole.OWNER));
		Workflow workflow = new Workflow("Workflow", project);
		setId(project, "project-id");
		setId(workflow, "workflow-id");

		var created = service.create(project, workflow, "task-id");
		var repeatedCreate = service.create(project, workflow, "task-id");
		assertEquals(created, repeatedCreate);
		assertEquals("agent/task-task-id", created.branchName());
		assertEquals("sample", created.repositoryPath());
		assertEquals(worktreesRoot.toRealPath()
						.resolve(project.getId()).resolve(workflow.getId()).resolve("task-id").toString(),
				created.workspacePath());
		assertEquals(created, service.get(project, workflow, "task-id"));
		var cleanReport = service.inspect(project, workflow, "task-id");
		assertEquals("agent/task-task-id", cleanReport.branchName());
		assertEquals(40, cleanReport.headCommit().length());
		org.junit.jupiter.api.Assertions.assertTrue(cleanReport.clean());
		org.junit.jupiter.api.Assertions.assertTrue(cleanReport.changes().isEmpty());
		assertEquals("task-id", cleanReport.taskId());

		Files.writeString(Path.of(created.workspacePath()).resolve("agent-notes.txt"), "QA can see this\n");
		var changedReport = service.inspect(project, workflow, "task-id");
		org.junit.jupiter.api.Assertions.assertFalse(changedReport.clean());
		assertEquals(1, changedReport.changes().size());
		assertEquals("agent-notes.txt", changedReport.changes().get(0).path());
		assertEquals("?", changedReport.changes().get(0).indexStatus());
		assertEquals("?", changedReport.changes().get(0).worktreeStatus());
		Path notesFile = Path.of(created.workspacePath()).resolve("agent-notes.txt");
		Files.writeString(notesFile,
				"api_key = \"test-secret-value-that-looks-real\"\n"
						+ "OPENAI_API_KEY=${OPENAI_API_KEY:}\n"
						+ "-----BEGIN PRIVATE KEY-----\n"
						+ "test-private-key-body\n"
						+ "-----END PRIVATE KEY-----\n");
		var securityReport = service.scanSecurity(project, workflow, "task-id");
		assertEquals(2, securityReport.findings().size(), securityReport.findings().toString());
		assertEquals("HARDCODED_CREDENTIAL", securityReport.findings().get(0).ruleId());
		assertEquals("agent-notes.txt", securityReport.findings().get(0).path());
		assertEquals(1, securityReport.findings().get(0).line());
		assertEquals("PRIVATE_KEY", securityReport.findings().get(1).ruleId());
		assertEquals(3, securityReport.findings().get(1).line());
		org.junit.jupiter.api.Assertions.assertTrue(securityReport.complete());

		Path workspacePath = Path.of(created.workspacePath());
		Files.createDirectories(workspacePath.resolve(".github/workflows"));
		Files.writeString(workspacePath.resolve("Dockerfile"), "FROM eclipse-temurin:21\n");
		Files.writeString(workspacePath.resolve("compose.yaml"), "services: {}\n");
		Files.writeString(workspacePath.resolve(".github/workflows/ci.yml"), "name: CI\n");
		Files.writeString(workspacePath.resolve("render.yaml"), "services: []\n");
		Files.writeString(workspacePath.resolve(".env.example"), "APP_ENV=production\n");
		var devOpsReport = service.inspectDevOps(project, workflow, "task-id");
		assertEquals(List.of("Dockerfile", "compose.yaml"), devOpsReport.containerFiles());
		assertEquals(List.of(".github\\workflows\\ci.yml"), devOpsReport.ciWorkflowFiles());
		assertEquals(List.of("render.yaml"), devOpsReport.deploymentFiles());
		assertEquals(List.of(".env.example"), devOpsReport.configurationExamples());
		assertEquals(List.of(), devOpsReport.recommendations());
		Files.delete(workspacePath.resolve("Dockerfile"));
		Files.delete(workspacePath.resolve("compose.yaml"));
		Files.delete(workspacePath.resolve(".github/workflows/ci.yml"));
		Files.delete(workspacePath.resolve("render.yaml"));
		Files.delete(workspacePath.resolve(".env.example"));
		Files.delete(workspacePath.resolve(".github/workflows"));
		Files.delete(workspacePath.resolve(".github"));
		Files.delete(notesFile);

		service.remove(project, workflow, "task-id");

		assertThrows(
				ResponseStatusException.class,
				() -> service.get(project, workflow, "task-id"));
	}

	@Test
	void rejectsRepositoryPathsOutsideConfiguredRoot() throws IOException {
		Path repositoriesRoot = Files.createDirectories(temporaryDirectory.resolve("repositories"));
		initializeRepository(temporaryDirectory.resolve("outside"));
		GitWorkspaceService service = new GitWorkspaceService(
				repositoriesRoot.toString(),
				temporaryDirectory.resolve("worktrees").toString(),
				new WorkspaceSecretScanner(),
				new WorkspaceDevOpsInspector());

		assertThrows(ResponseStatusException.class, () -> service.validateRepositoryPath("../outside"));
		assertThrows(ResponseStatusException.class, () -> service.validateRepositoryPath(
				temporaryDirectory.resolve("outside").toString()));
	}

	@Test
	void refusesToRemoveWorkspaceWithUncommittedFiles() throws IOException {
		Path repositoriesRoot = Files.createDirectories(temporaryDirectory.resolve("repositories"));
		initializeRepository(repositoriesRoot.resolve("sample"));
		GitWorkspaceService service = new GitWorkspaceService(
				repositoriesRoot.toString(),
				temporaryDirectory.resolve("worktrees").toString(),
				new WorkspaceSecretScanner(),
				new WorkspaceDevOpsInspector());
		String storedPath = service.validateRepositoryPath("sample");
		Project project = new Project(
				"Project", null, storedPath,
				new User("test@example.com", "hash", "Test", "User", new AccountingFirm("Test Firm"), FirmRole.OWNER));
		Workflow workflow = new Workflow("Workflow", project);
		setId(project, "project-id");
		setId(workflow, "workflow-id");
		var workspace = service.create(project, workflow, "task-id");
		Files.writeString(Path.of(workspace.workspacePath()).resolve("agent-notes.txt"), "keep me\n");

		assertThrows(ResponseStatusException.class, () -> service.remove(project, workflow, "task-id"));
		assertEquals(workspace, service.get(project, workflow, "task-id"));
	}

	@Test
	void reportsMissingDevOpsFilesAsRecommendations() throws IOException {
		Path repositoriesRoot = Files.createDirectories(temporaryDirectory.resolve("repositories"));
		initializeRepository(repositoriesRoot.resolve("sample"));
		GitWorkspaceService service = new GitWorkspaceService(
				repositoriesRoot.toString(),
				temporaryDirectory.resolve("worktrees").toString(),
				new WorkspaceSecretScanner(),
				new WorkspaceDevOpsInspector());
		String storedPath = service.validateRepositoryPath("sample");
		Project project = new Project(
				"Project", null, storedPath,
				new User("test@example.com", "hash", "Test", "User", new AccountingFirm("Test Firm"), FirmRole.OWNER));
		Workflow workflow = new Workflow("Workflow", project);
		setId(project, "project-id");
		setId(workflow, "workflow-id");
		service.create(project, workflow, "task-id");

		var report = service.inspectDevOps(project, workflow, "task-id");

		assertEquals(List.of(), report.containerFiles());
		assertEquals(List.of(), report.ciWorkflowFiles());
		assertEquals(List.of(), report.deploymentFiles());
		assertEquals(List.of(), report.configurationExamples());
		assertEquals(4, report.recommendations().size());
	}

	private Path initializeRepository(Path repository) throws IOException {
		Files.createDirectories(repository);
		runGit(repository, "init", "-b", "main");
		runGit(repository, "config", "user.email", "test@example.com");
		runGit(repository, "config", "user.name", "Test User");
		Files.writeString(repository.resolve("README.md"), "test repository\n");
		runGit(repository, "add", "README.md");
		runGit(repository, "commit", "-m", "Initial commit");
		return repository.toRealPath();
	}

	private void setId(Object entity, String id) {
		try {
			var idField = entity.getClass().getDeclaredField("id");
			idField.setAccessible(true);
			idField.set(entity, id);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Could not set entity ID for the workspace test", exception);
		}
	}

	private void runGit(Path directory, String... arguments) throws IOException {
		ArrayList<String> command = new ArrayList<>(List.of("git", "-C", directory.toString()));
		command.addAll(List.of(arguments));
		Process process = new ProcessBuilder(command)
				.redirectErrorStream(true)
				.start();
		try {
			if (!process.waitFor(10, TimeUnit.SECONDS) || process.exitValue() != 0) {
				process.destroyForcibly();
				throw new IllegalStateException("Git test repository command failed");
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Git test repository setup was interrupted", exception);
		}
	}

}
