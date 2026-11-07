package com.ahmed.ai_business_platform.task.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ahmed.ai_business_platform.project.entity.Project;
import com.ahmed.ai_business_platform.task.dto.GitWorkspaceResponse;
import com.ahmed.ai_business_platform.task.dto.GitWorkspaceChange;
import com.ahmed.ai_business_platform.task.dto.GitWorkspaceQaReport;
import com.ahmed.ai_business_platform.task.dto.GitWorkspaceSecurityReport;
import com.ahmed.ai_business_platform.task.dto.GitWorkspaceDevOpsReport;
import com.ahmed.ai_business_platform.workflow.entity.Workflow;

@Service
public class GitWorkspaceService {

	private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(20);

	private final Path repositoriesRoot;
	private final Path worktreesRoot;
	private final WorkspaceSecretScanner secretScanner;
	private final WorkspaceDevOpsInspector devOpsInspector;

	public GitWorkspaceService(
			@Value("${app.workspaces.repositories-root:./repositories}") String repositoriesRoot,
			@Value("${app.workspaces.worktrees-root:./worktrees}") String worktreesRoot,
			WorkspaceSecretScanner secretScanner,
			WorkspaceDevOpsInspector devOpsInspector) {
		this.repositoriesRoot = Path.of(repositoriesRoot).toAbsolutePath().normalize();
		this.worktreesRoot = Path.of(worktreesRoot).toAbsolutePath().normalize();
		this.secretScanner = secretScanner;
		this.devOpsInspector = devOpsInspector;
	}

	public String validateRepositoryPath(String repositoryPath) {
		Path repository = resolveRepository(repositoryPath);
		try {
			return repositoriesRoot.toRealPath().relativize(repository).toString();
		} catch (IOException exception) {
			throw new IllegalStateException("Could not resolve the configured repository root", exception);
		}
	}

	public GitWorkspaceResponse create(Project project, Workflow workflow, String taskId) {
		Path repository = requireProjectRepository(project);
		Path workspaceRoot = resolveWorktreesRoot();
		Path workspace = workspacePath(workspaceRoot, project, workflow, taskId);
		String branch = branchName(taskId);

		if (isRegisteredWorktree(repository, workspace)) {
			return response(project, workflow, taskId, branch, workspace);
		}
		if (Files.exists(workspace)) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT, "Workspace path exists but is not a registered Git worktree");
		}

		try {
			Files.createDirectories(workspace.getParent());
			if (!workspace.getParent().toRealPath().startsWith(workspaceRoot)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace path is outside the configured root");
			}
		} catch (IOException exception) {
			throw new IllegalStateException("Could not prepare the configured workspace directory", exception);
		}

		List<String> branchLookup = runGit(repository, List.of("branch", "--list", branch));
		List<String> command = new ArrayList<>(List.of("worktree", "add"));
		if (branchLookup.isEmpty()) {
			command.addAll(List.of("-b", branch));
		}
		command.add(workspace.toString());
		if (!branchLookup.isEmpty()) {
			command.add(branch);
		} else {
			command.add("HEAD");
		}
		runGit(repository, command);
		return response(project, workflow, taskId, branch, workspace);
	}

	public GitWorkspaceResponse get(Project project, Workflow workflow, String taskId) {
		Path repository = requireProjectRepository(project);
		Path workspace = workspacePath(resolveWorktreesRoot(), project, workflow, taskId);
		if (!isRegisteredWorktree(repository, workspace)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Git workspace was not found");
		}
		return response(project, workflow, taskId, branchName(taskId), workspace);
	}

	public GitWorkspaceQaReport inspect(Project project, Workflow workflow, String taskId) {
		Path repository = requireProjectRepository(project);
		Path workspace = workspacePath(resolveWorktreesRoot(), project, workflow, taskId);
		if (!isRegisteredWorktree(repository, workspace)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Git workspace was not found");
		}

		String branch = singleGitValue(workspace, "branch", "--show-current");
		String headCommit = singleGitValue(workspace, "rev-parse", "HEAD");
		List<String> statusLines = runGit(workspace, List.of("status", "--porcelain=v1", "--untracked-files=normal"));
		List<GitWorkspaceChange> changes = statusLines.stream()
				.map(GitWorkspaceService::parseChange)
				.toList();
		return new GitWorkspaceQaReport(
				project.getId(),
				workflow.getId(),
				taskId,
				branch,
				headCommit,
				changes.isEmpty(),
				changes,
				Instant.now());
	}

	public GitWorkspaceSecurityReport scanSecurity(Project project, Workflow workflow, String taskId) {
		Path repository = requireProjectRepository(project);
		Path workspace = workspacePath(resolveWorktreesRoot(), project, workflow, taskId);
		if (!isRegisteredWorktree(repository, workspace)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Git workspace was not found");
		}
		return secretScanner.scan(workspace, project, workflow, taskId);
	}

	public GitWorkspaceDevOpsReport inspectDevOps(Project project, Workflow workflow, String taskId) {
		Path repository = requireProjectRepository(project);
		Path workspace = workspacePath(resolveWorktreesRoot(), project, workflow, taskId);
		if (!isRegisteredWorktree(repository, workspace)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Git workspace was not found");
		}
		return devOpsInspector.inspect(workspace, project, workflow, taskId);
	}

	public void remove(Project project, Workflow workflow, String taskId) {
		Path repository = requireProjectRepository(project);
		Path workspace = workspacePath(resolveWorktreesRoot(), project, workflow, taskId);
		if (!isRegisteredWorktree(repository, workspace)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Git workspace was not found");
		}
		if (!runGit(workspace, List.of("status", "--porcelain", "--untracked-files=all", "--ignored=matching"))
				.isEmpty()) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"Workspace has uncommitted changes; commit or remove them before deleting the workspace");
		}
		runGit(repository, List.of("worktree", "remove", workspace.toString()));
	}

	private Path resolveRepository(String repositoryPath) {
		if (repositoryPath == null || repositoryPath.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Repository path is required");
		}
		Path relativePath;
		try {
			relativePath = Path.of(repositoryPath).normalize();
		} catch (RuntimeException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Repository path is invalid", exception);
		}
		if (relativePath.isAbsolute() || relativePath.startsWith("..")) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST, "Repository path must be inside the configured repository root");
		}

		try {
			Path root = repositoriesRoot.toRealPath();
			Path repository = root.resolve(relativePath).normalize().toRealPath();
			if (!repository.startsWith(root) || !Files.isDirectory(repository)) {
				throw new ResponseStatusException(
						HttpStatus.BAD_REQUEST, "Repository path must identify a directory inside the configured root");
			}
			List<String> gitRoot = runGit(repository, List.of("rev-parse", "--show-toplevel"));
			if (gitRoot.size() != 1 || !Path.of(gitRoot.get(0)).toRealPath().equals(repository)) {
				throw new ResponseStatusException(
						HttpStatus.BAD_REQUEST, "Repository path must identify the root of a Git repository");
			}
			return repository;
		} catch (IOException exception) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST, "Repository path does not resolve inside the configured repository root");
		}
	}

	private Path requireProjectRepository(Project project) {
		if (project.getRepositoryPath() == null) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT, "Configure a local Git repository when creating the project");
		}
		return resolveRepository(project.getRepositoryPath());
	}

	private Path resolveWorktreesRoot() {
		try {
			Files.createDirectories(worktreesRoot);
			return worktreesRoot.toRealPath();
		} catch (IOException exception) {
			throw new IllegalStateException("Could not access the configured worktrees root", exception);
		}
	}

	private Path workspacePath(Path root, Project project, Workflow workflow, String taskId) {
		Path workspace = root.resolve(project.getId()).resolve(workflow.getId()).resolve(taskId).normalize();
		if (!workspace.startsWith(root)) {
			throw new IllegalStateException("Generated workspace path escaped the configured root");
		}
		return workspace;
	}

	private boolean isRegisteredWorktree(Path repository, Path workspace) {
		return runGit(repository, List.of("worktree", "list", "--porcelain")).stream()
				.filter(line -> line.startsWith("worktree "))
				.map(line -> Path.of(line.substring("worktree ".length())).toAbsolutePath().normalize())
				.anyMatch(workspace.toAbsolutePath().normalize()::equals);
	}

	private GitWorkspaceResponse response(
			Project project,
			Workflow workflow,
			String taskId,
			String branch,
			Path workspace) {
		return new GitWorkspaceResponse(
				project.getId(),
				workflow.getId(),
				taskId,
				branch,
				project.getRepositoryPath(),
				workspace.toString());
	}

	private String branchName(String taskId) {
		return "agent/task-" + taskId;
	}

	private String singleGitValue(Path directory, String... arguments) {
		List<String> output = runGit(directory, List.of(arguments));
		if (output.size() != 1) {
			throw new IllegalStateException("Git returned an unexpected value for a workspace inspection");
		}
		return output.get(0);
	}

	private static GitWorkspaceChange parseChange(String line) {
		if (line.length() < 4) {
			throw new IllegalStateException("Git returned an invalid porcelain status line");
		}
		return new GitWorkspaceChange(line.substring(3), line.substring(0, 1), line.substring(1, 2));
	}

	private List<String> runGit(Path directory, List<String> arguments) {
		Path outputFile;
		try {
			outputFile = Files.createTempFile("ai-business-git-", ".log");
		} catch (IOException exception) {
			throw new IllegalStateException("Could not create temporary output for Git", exception);
		}

		List<String> command = new ArrayList<>(arguments.size() + 2);
		command.add("git");
		command.add("-C");
		command.add(directory.toString());
		command.addAll(arguments);
		Process process = null;
		try {
			process = new ProcessBuilder(command)
					.redirectErrorStream(true)
					.redirectOutput(outputFile.toFile())
					.start();
			if (!process.waitFor(COMMAND_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
				process.destroyForcibly();
				throw new IllegalStateException("Git operation exceeded the 20-second timeout");
			}
			List<String> output = Files.readAllLines(outputFile, StandardCharsets.UTF_8).stream()
					.map(String::stripTrailing)
					.filter(line -> !line.isBlank())
					.toList();
			if (process.exitValue() != 0) {
				throw new ResponseStatusException(
						HttpStatus.CONFLICT,
						"Git operation failed. Check the repository state and workspace branch.");
			}
			return output;
		} catch (IOException exception) {
			throw new IllegalStateException("Could not run Git; ensure Git is installed and accessible", exception);
		} catch (InterruptedException exception) {
			if (process != null) {
				process.destroyForcibly();
			}
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Git operation was interrupted", exception);
		} finally {
			try {
				Files.deleteIfExists(outputFile);
			} catch (IOException exception) {
				throw new IllegalStateException("Could not remove temporary Git output file", exception);
			}
		}
	}
}
