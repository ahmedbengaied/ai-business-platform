package com.ahmed.ai_business_platform.task.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.ahmed.ai_business_platform.task.dto.GitWorkspaceSecurityReport;
import com.ahmed.ai_business_platform.task.dto.SecurityFinding;
import com.ahmed.ai_business_platform.project.entity.Project;
import com.ahmed.ai_business_platform.workflow.entity.Workflow;

@Service
public class WorkspaceSecretScanner {

	private static final int MAX_FILES = 5000;
	private static final long MAX_FILE_BYTES = 1024 * 1024;
	private static final Pattern PRIVATE_KEY = Pattern.compile(
			"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----");
	private static final Pattern CREDENTIAL_ASSIGNMENT = Pattern.compile(
			"(?i)(api[_-]?key|access[_-]?token|auth[_-]?token|client[_-]?secret|secret[_-]?key|password)"
					+ "\\s*[:=]\\s*[\"']?([A-Za-z0-9_./+=-]{12,})");
	private static final Pattern GITHUB_TOKEN = Pattern.compile(
			"\\b(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,})\\b");
	private static final Pattern AWS_ACCESS_KEY = Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b");
	private static final List<String> PLACEHOLDER_PREFIXES = List.of(
			"change-me", "changeme", "example", "your-", "replace-", "dummy", "placeholder");

	public GitWorkspaceSecurityReport scan(
			Path workspace, Project project, Workflow workflow, String taskId) {
		Path root;
		try {
			root = workspace.toRealPath();
		} catch (IOException exception) {
			throw new IllegalStateException("Could not resolve the task workspace for security scanning", exception);
		}
		ScanState state = new ScanState();
		try {
			Files.walkFileTree(root, new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
					if (!directory.equals(root) && directory.getFileName().toString().equals(".git")) {
						return FileVisitResult.SKIP_SUBTREE;
					}
					if (!directory.toAbsolutePath().normalize().startsWith(root)) {
						throw new IllegalStateException("Security scanner encountered a path outside the workspace");
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
					if (!attributes.isRegularFile()) {
						state.skippedFiles++;
						return FileVisitResult.CONTINUE;
					}
					if (state.scannedFiles + state.skippedFiles >= MAX_FILES) {
						state.complete = false;
						return FileVisitResult.TERMINATE;
					}
					if (attributes.size() > MAX_FILE_BYTES) {
						state.skippedFiles++;
						state.complete = false;
						return FileVisitResult.CONTINUE;
					}
					byte[] bytes = Files.readAllBytes(file);
					if (containsNullByte(bytes)) {
						state.skippedFiles++;
						return FileVisitResult.CONTINUE;
					}
					state.scannedFiles++;
					scanText(root, file, new String(bytes, StandardCharsets.UTF_8), state.findings);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exception) throws IOException {
					throw new IOException("Could not read a file during workspace security scanning", exception);
				}
			});
		} catch (IOException exception) {
			throw new IllegalStateException("Workspace security scan could not be completed", exception);
		}
		return new GitWorkspaceSecurityReport(
				project.getId(),
				workflow.getId(),
				taskId,
				List.copyOf(state.findings),
				state.scannedFiles,
				state.skippedFiles,
				state.complete,
				Instant.now());
	}

	private void scanText(Path root, Path file, String content, List<SecurityFinding> findings) {
		String[] lines = content.split("\\R", -1);
		String relativePath = root.relativize(file).toString();
		for (int index = 0; index < lines.length; index++) {
			String line = lines[index];
			if (PRIVATE_KEY.matcher(line).find()) {
				findings.add(new SecurityFinding("PRIVATE_KEY", "CRITICAL", relativePath, index + 1));
			}
			if (containsCredential(line)) {
				findings.add(new SecurityFinding("HARDCODED_CREDENTIAL", "HIGH", relativePath, index + 1));
			}
			if (GITHUB_TOKEN.matcher(line).find()) {
				findings.add(new SecurityFinding("GITHUB_TOKEN", "CRITICAL", relativePath, index + 1));
			}
			if (AWS_ACCESS_KEY.matcher(line).find()) {
				findings.add(new SecurityFinding("AWS_ACCESS_KEY", "HIGH", relativePath, index + 1));
			}
		}
	}

	private boolean containsCredential(String line) {
		Matcher matcher = CREDENTIAL_ASSIGNMENT.matcher(line);
		while (matcher.find()) {
			String candidate = matcher.group(2).toLowerCase(java.util.Locale.ROOT);
			if (candidate.startsWith("${") || PLACEHOLDER_PREFIXES.stream().anyMatch(candidate::startsWith)) {
				continue;
			}
			return true;
		}
		return false;
	}

	private boolean containsNullByte(byte[] bytes) {
		for (byte value : bytes) {
			if (value == 0) {
				return true;
			}
		}
		return false;
	}

	private static class ScanState {
		private final List<SecurityFinding> findings = new ArrayList<>();
		private int scannedFiles;
		private int skippedFiles;
		private boolean complete = true;
	}
}
