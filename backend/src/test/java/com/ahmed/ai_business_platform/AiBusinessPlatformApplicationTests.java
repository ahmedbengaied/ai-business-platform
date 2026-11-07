package com.ahmed.ai_business_platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AiBusinessPlatformApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void registrationReturnsTokenAndProtectedMeEndpointAcceptsIt() throws Exception {
		String email = "user-" + System.nanoTime() + "@example.com";
		String registration = """
				{
				  "email": "%s",
				  "password": "secure-password",
				  "firstName": "Ada",
				  "lastName": "Lovelace"
				}
				""".formatted(email);

		String response = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registration))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.user.email").value(email))
				.andReturn()
				.getResponse()
				.getContentAsString();

		String token = response.replaceFirst(".*\"token\":\"([^\"]+)\".*", "$1");

		mockMvc.perform(get("/api/v1/auth/me")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email));
	}

	@Test
	void protectedEndpointRejectsAnonymousRequest() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void aiUsageEndpointReturnsOnlyAuthenticatedUsersCurrentMonthUsage() throws Exception {
		String token = registerAndGetToken("usage-" + System.nanoTime() + "@example.com");

		mockMvc.perform(get("/api/v1/ai/usage").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.monthlyLimitUsd").value(5.0))
				.andExpect(jsonPath("$.spentUsd").value(0.0))
				.andExpect(jsonPath("$.remainingUsd").value(5.0))
				.andExpect(jsonPath("$.promptTokens").value(0))
				.andExpect(jsonPath("$.completionTokens").value(0));

		mockMvc.perform(get("/api/v1/ai/usage"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void projectWorkflowAndAgentTaskCanBeCreatedAndTracked() throws Exception {
		String email = "workflow-" + System.nanoTime() + "@example.com";
		String token = registerAndGetToken(email);

		String projectResponse = mockMvc.perform(post("/api/v1/projects")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Agent platform","description":"Build the first workflow"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Agent platform"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String projectId = extractId(projectResponse);

		String workflowResponse = mockMvc.perform(post("/api/v1/projects/" + projectId + "/workflows")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Plan and build"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PLANNED"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String workflowId = extractId(workflowResponse);

		String tasksPath = "/api/v1/projects/" + projectId + "/workflows/" + workflowId + "/tasks";
		String taskResponse = mockMvc.perform(post(tasksPath)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Define the API","description":"Write the initial API contract","agentRole":"ARCHITECT"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.agentRole").value("ARCHITECT"))
				.andExpect(jsonPath("$.status").value("TODO"))
				.andExpect(jsonPath("$.approvalStatus").value("PENDING"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String taskId = extractId(taskResponse);

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
						tasksPath + "/" + taskId + "/status")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"IN_PROGRESS"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"));

		mockMvc.perform(get(tasksPath).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(taskId));

		String executableTaskResponse = mockMvc.perform(post(tasksPath)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Review the API design","agentRole":"ARCHITECT"}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String executableTaskId = extractId(executableTaskResponse);

		mockMvc.perform(post(tasksPath + "/" + executableTaskId + "/execute")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isConflict());

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
						tasksPath + "/" + executableTaskId + "/approval")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"APPROVED","note":"Reviewed and accepted"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.approvalStatus").value("APPROVED"))
				.andExpect(jsonPath("$.approvalNote").value("Reviewed and accepted"))
				.andExpect(jsonPath("$.reviewedBy").value(email))
				.andExpect(jsonPath("$.reviewedAt").isNotEmpty());

		mockMvc.perform(post(tasksPath + "/" + executableTaskId + "/execute")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DONE"))
				.andExpect(jsonPath("$.executionAttempts").value(1))
				.andExpect(jsonPath("$.lastExecutionFailure").doesNotExist())
				.andExpect(jsonPath("$.result").value(
						"Mock ARCHITECT agent completed task: Review the API design. No AI model was called."))
				.andExpect(jsonPath("$.artifact.summary").value(
						"Mock ARCHITECT agent completed task: Review the API design. No AI model was called."))
				.andExpect(jsonPath("$.artifact.nextSteps").isEmpty());

		mockMvc.perform(post(tasksPath + "/" + executableTaskId + "/execute")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isConflict());

		mockMvc.perform(get(tasksPath).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[1].result").isNotEmpty());
	}

	@Test
	void projectIsNotVisibleToAnotherUser() throws Exception {
		String ownerToken = registerAndGetToken("owner-" + System.nanoTime() + "@example.com");
		String otherToken = registerAndGetToken("other-" + System.nanoTime() + "@example.com");
		String projectResponse = mockMvc.perform(post("/api/v1/projects")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Private project"}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		mockMvc.perform(get("/api/v1/projects/" + extractId(projectResponse))
						.header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isNotFound());
	}

	@Test
	void workflowExecutionRunsTodoTasksAndCompletesWorkflow() throws Exception {
		String email = "workflow-run-" + System.nanoTime() + "@example.com";
		String token = registerAndGetToken(email);
		String projectResponse = mockMvc.perform(post("/api/v1/projects")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Workflow execution project"}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String projectId = extractId(projectResponse);

		String workflowResponse = mockMvc.perform(post("/api/v1/projects/" + projectId + "/workflows")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Run all tasks"}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String workflowId = extractId(workflowResponse);
		String workflowPath = "/api/v1/projects/" + projectId + "/workflows/" + workflowId;
		String tasksPath = workflowPath + "/tasks";

		String dependentTaskResponse = mockMvc.perform(post(tasksPath)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Review design","agentRole":"PRODUCT"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("TODO"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String dependentTaskId = extractId(dependentTaskResponse);

		String prerequisiteTaskResponse = mockMvc.perform(post(tasksPath)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Define requirements","agentRole":"PRODUCT"}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String prerequisiteTaskId = extractId(prerequisiteTaskResponse);

		mockMvc.perform(post(tasksPath + "/" + dependentTaskId + "/dependencies")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"prerequisiteTaskId":"%s"}
								""".formatted(prerequisiteTaskId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dependsOnTaskIds[0]").value(prerequisiteTaskId));

		mockMvc.perform(post(tasksPath + "/" + prerequisiteTaskId + "/dependencies")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"prerequisiteTaskId":"%s"}
								""".formatted(dependentTaskId)))
				.andExpect(status().isConflict());

		mockMvc.perform(post(tasksPath + "/" + dependentTaskId + "/execute")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isConflict());

		mockMvc.perform(post(workflowPath + "/execute")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isConflict());
		mockMvc.perform(get(workflowPath + "/runs").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		for (String taskId : new String[] {dependentTaskId, prerequisiteTaskId}) {
			mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
							tasksPath + "/" + taskId + "/approval")
							.header("Authorization", "Bearer " + token)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"status":"APPROVED","note":"Reviewed"}
									"""))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.approvalStatus").value("APPROVED"));
		}

		mockMvc.perform(post(workflowPath + "/execute")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"));

		mockMvc.perform(get(tasksPath).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].status").value("DONE"))
				.andExpect(jsonPath("$[0].executionAttempts").value(1))
				.andExpect(jsonPath("$[0].result").value(
						"Mock PRODUCT agent completed task: Review design. No AI model was called."))
				.andExpect(jsonPath("$[1].status").value("DONE"))
				.andExpect(jsonPath("$[1].executionAttempts").value(1))
				.andExpect(jsonPath("$[1].result").value(
						"Mock PRODUCT agent completed task: Define requirements. No AI model was called."));

		mockMvc.perform(get(workflowPath + "/runs").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].workflowId").value(workflowId))
				.andExpect(jsonPath("$[0].startedBy").value(email))
				.andExpect(jsonPath("$[0].status").value("COMPLETED"))
				.andExpect(jsonPath("$[0].startedAt").isNotEmpty())
				.andExpect(jsonPath("$[0].completedAt").isNotEmpty())
				.andExpect(jsonPath("$[0].completedTaskCount").value(2))
				.andExpect(jsonPath("$[0].failureReason").doesNotExist());

		mockMvc.perform(post(workflowPath + "/execute")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isConflict());
	}

	private String registerAndGetToken(String email) throws Exception {
		String registration = """
				{
				  "email": "%s",
				  "password": "secure-password",
				  "firstName": "Test",
				  "lastName": "User"
				}
				""".formatted(email);
		String response = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registration))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return response.replaceFirst(".*\"token\":\"([^\"]+)\".*", "$1");
	}

	private String extractId(String response) {
		return response.replaceFirst(".*\"id\":\"([^\"]+)\".*", "$1");
	}
}
