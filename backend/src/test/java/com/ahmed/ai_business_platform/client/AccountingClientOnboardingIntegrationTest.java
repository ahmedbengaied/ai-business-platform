package com.ahmed.ai_business_platform.client;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AccountingClientOnboardingIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void createsClientChecklistAndTracksDocumentReviewForOwnerOnly() throws Exception {
		String ownerToken = registerAndGetToken("accounting-" + System.nanoTime() + "@example.com");
		String otherToken = registerAndGetToken("other-" + System.nanoTime() + "@example.com");

		String clientResponse = mockMvc.perform(post("/api/v1/clients")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "legalName":"Northwind Studio Ltd",
								  "contactName":"Ada Lovelace",
								  "contactEmail":"ada@example.com",
								  "contactPhone":"+441234567890",
								  "templateId":null
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.legalName").value("Northwind Studio Ltd"))
				.andExpect(jsonPath("$.onboardingStatus").value("NOT_STARTED"))
				.andExpect(jsonPath("$.documentCount").value(5))
				.andExpect(jsonPath("$.approvedDocumentCount").value(0))
				.andExpect(jsonPath("$.documents[0].status").value("REQUESTED"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String clientId = extractId(clientResponse);
		String documentsPath = "/api/v1/clients/" + clientId + "/documents";
		String firstDocumentId = clientResponse.replaceFirst(
				".*\"documents\":\\[\\{\"id\":\"([^\"]+)\".*", "$1");

		String customDocumentResponse = mockMvc.perform(post(documentsPath)
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Client-specific engagement letter"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Client-specific engagement letter"))
				.andExpect(jsonPath("$.status").value("REQUESTED"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String customDocumentId = extractId(customDocumentResponse);

		mockMvc.perform(post(documentsPath)
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"client-specific engagement letter"}
								"""))
				.andExpect(status().isConflict());

		mockMvc.perform(patch(documentsPath + "/" + firstDocumentId)
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"RECEIVED"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.documents[0].status").value("RECEIVED"))
				.andExpect(jsonPath("$.onboardingStatus").value("IN_PROGRESS"));

		mockMvc.perform(patch(documentsPath + "/" + firstDocumentId)
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"APPROVED"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.approvedDocumentCount").value(1));

		mockMvc.perform(get("/api/v1/clients/" + clientId)
						.header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isNotFound());

		mockMvc.perform(patch(documentsPath + "/" + customDocumentId)
						.header("Authorization", "Bearer " + otherToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"APPROVED"}
								"""))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/v1/clients").header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		mockMvc.perform(get("/api/v1/clients"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void managesFirmOwnedTemplatesAndCopiesTemplateItemsIntoClientChecklist() throws Exception {
		String ownerToken = registerAndGetToken("template-owner-" + System.nanoTime() + "@example.com");
		String otherToken = registerAndGetToken("template-other-" + System.nanoTime() + "@example.com");
		String templatesPath = "/api/v1/onboarding-templates";

		String templateResponse = mockMvc.perform(post(templatesPath)
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name":"New company formation",
								  "documentNames":["Incorporation certificate","Director register"]
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("New company formation"))
				.andExpect(jsonPath("$.documentNames.length()").value(2))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String templateId = extractId(templateResponse);

		mockMvc.perform(post(templatesPath)
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name":"new COMPANY formation",
								  "documentNames":["Unique item"]
								}
								"""))
				.andExpect(status().isConflict());

		String clientResponse = mockMvc.perform(post("/api/v1/clients")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "legalName":"Template Client Ltd",
								  "contactName":"Grace Hopper",
								  "templateId":"%s"
								}
								""".formatted(templateId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.documentCount").value(2))
				.andExpect(jsonPath("$.documents[0].name").value("Incorporation certificate"))
				.andExpect(jsonPath("$.documents[1].name").value("Director register"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String clientId = extractId(clientResponse);

		mockMvc.perform(put(templatesPath + "/" + templateId)
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name":"Updated formation checklist",
								  "documentNames":["Updated certificate"]
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.documentNames.length()").value(1));

		mockMvc.perform(get("/api/v1/clients/" + clientId)
						.header("Authorization", "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.documents.length()").value(2))
				.andExpect(jsonPath("$.documents[0].name").value("Incorporation certificate"));

		mockMvc.perform(get(templatesPath + "/" + templateId)
						.header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isNotFound());

		mockMvc.perform(put(templatesPath + "/" + templateId)
						.header("Authorization", "Bearer " + otherToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name":"Unauthorized update",
								  "documentNames":["Unauthorized item"]
								}
								"""))
				.andExpect(status().isNotFound());

		mockMvc.perform(delete(templatesPath + "/" + templateId)
						.header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isNotFound());

		mockMvc.perform(get(templatesPath).header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void managesFirmInvitationsSharesOnboardingAndEnforcesViewerPermissions() throws Exception {
		String ownerToken = registerAndGetToken("firm-owner-" + System.nanoTime() + "@example.com");
		String invitationsPath = "/api/v1/firm/invitations";
		String membersPath = "/api/v1/firm/members";

		mockMvc.perform(get(invitationsPath + "/preview").param("token", "invalid"))
				.andExpect(status().isNotFound());

		String staffInvite = createInvitation(ownerToken, "firm-staff-" + System.nanoTime() + "@example.com", "STAFF");
		String staffInviteToken = extractToken(staffInvite);
		mockMvc.perform(get(invitationsPath + "/preview").param("token", staffInviteToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("STAFF"))
				.andExpect(jsonPath("$.firmName").value("Client Accountant Accounting"));
		String staffResponse = registerWithInvitation(staffInviteToken, extractEmail(staffInvite));
		String staffToken = extractToken(staffResponse);
		mockMvc.perform(get(invitationsPath + "/preview").param("token", staffInviteToken))
				.andExpect(status().isNotFound());

		String adminInvite = createInvitation(
				ownerToken, "firm-admin-" + System.nanoTime() + "@example.com", "ADMIN");
		String adminResponse = registerWithInvitation(extractToken(adminInvite), extractEmail(adminInvite));
		String adminToken = extractToken(adminResponse);
		String adminCreatedInvite = mockMvc.perform(post(invitationsPath)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"admin-created-staff@example.com","role":"STAFF"}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		mockMvc.perform(delete(invitationsPath + "/" + extractId(adminCreatedInvite))
						.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNoContent());
		mockMvc.perform(post(invitationsPath)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"admin-created-admin@example.com","role":"ADMIN"}
								"""))
				.andExpect(status().isForbidden());
		mockMvc.perform(patch(membersPath + "/" + extractUserId(staffResponse))
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"role":"ADMIN"}
								"""))
				.andExpect(status().isForbidden());

		String pendingInvite = createInvitation(
				ownerToken, "firm-pending-" + System.nanoTime() + "@example.com", "VIEWER");
		mockMvc.perform(get("/api/v1/firm/team").header("Authorization", "Bearer " + staffToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.invitations.length()").value(0));
		mockMvc.perform(get("/api/v1/firm/team").header("Authorization", "Bearer " + ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.invitations.length()").value(1));
		mockMvc.perform(delete(invitationsPath + "/" + extractId(pendingInvite))
						.header("Authorization", "Bearer " + ownerToken))
				.andExpect(status().isNoContent());

		String clientResponse = mockMvc.perform(post("/api/v1/clients")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"legalName":"Shared Firm Client Ltd","contactName":"Morgan Lee"}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String clientId = extractId(clientResponse);

		mockMvc.perform(get("/api/v1/clients").header("Authorization", "Bearer " + staffToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].legalName").value("Shared Firm Client Ltd"));
		mockMvc.perform(post("/api/v1/onboarding-templates")
						.header("Authorization", "Bearer " + staffToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Staff Checklist","documentNames":["Staff requirement"]}
								"""))
				.andExpect(status().isCreated());
		mockMvc.perform(post(invitationsPath)
						.header("Authorization", "Bearer " + staffToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"another-staff@example.com","role":"STAFF"}
								"""))
				.andExpect(status().isForbidden());

		String viewerInvite = createInvitation(
				ownerToken, "firm-viewer-" + System.nanoTime() + "@example.com", "VIEWER");
		String viewerToken = extractToken(registerWithInvitation(
				extractToken(viewerInvite), extractEmail(viewerInvite)));
		mockMvc.perform(get("/api/v1/clients/" + clientId).header("Authorization", "Bearer " + viewerToken))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/onboarding-templates").header("Authorization", "Bearer " + viewerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(post("/api/v1/clients")
						.header("Authorization", "Bearer " + viewerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"legalName":"Read Only Client Ltd","contactName":"Morgan Lee"}
								"""))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/onboarding-templates")
						.header("Authorization", "Bearer " + viewerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Viewer Checklist","documentNames":["Read only"]}
								"""))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/projects")
						.header("Authorization", "Bearer " + viewerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Read Only Project"}
								"""))
				.andExpect(status().isForbidden());

		mockMvc.perform(delete(membersPath + "/" + extractUserId(staffResponse))
						.header("Authorization", "Bearer " + ownerToken))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/clients").header("Authorization", "Bearer " + staffToken))
				.andExpect(status().isUnauthorized());

		String reactivationInvite = createInvitation(
				ownerToken, extractEmail(staffInvite), "VIEWER");
		String reactivated = registerWithInvitation(
				extractToken(reactivationInvite), extractEmail(reactivationInvite));
		mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + extractToken(reactivated)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firmRole").value("VIEWER"))
				.andExpect(jsonPath("$.firmName").value("Client Accountant Accounting"));
		mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + staffToken))
				.andExpect(status().isUnauthorized());
	}

	private String registerAndGetToken(String email) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email":"%s",
								  "password":"secure-password",
								  "firstName":"Client",
								  "lastName":"Accountant"
								}
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return response.replaceFirst(".*\"token\":\"([^\"]+)\".*", "$1");
	}

	private String createInvitation(String ownerToken, String email, String role) throws Exception {
		return mockMvc.perform(post("/api/v1/firm/invitations")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","role":"%s"}
								""".formatted(email, role)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
	}

	private String registerWithInvitation(String invitationToken, String email) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email":"%s",
								  "password":"secure-password",
								  "firstName":"Client",
								  "lastName":"Accountant",
								  "invitationToken":"%s"
								}
								""".formatted(email, invitationToken)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
	}

	private String extractEmail(String response) {
		return response.replaceFirst(".*\"email\":\"([^\"]+)\".*", "$1");
	}

	private String extractToken(String response) {
		return response.replaceFirst(".*\"token\":\"([^\"]+)\".*", "$1");
	}

	private String extractUserId(String response) {
		return response.replaceFirst(".*\"user\":\\{\"id\":\"([^\"]+)\".*", "$1");
	}

	private String extractId(String response) {
		return response.replaceFirst("^\\{\"id\":\"([^\"]+)\".*", "$1");
	}
}
