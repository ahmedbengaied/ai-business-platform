package com.ahmed.ai_business_platform.purchase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

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
class PurchaseBillIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void managesFirmBillsPaymentsAndRoleRestrictions() throws Exception {
		String ownerToken = registerAndGetToken("purchase-owner-" + System.nanoTime() + "@example.com");
		String outsiderToken = registerAndGetToken("purchase-other-" + System.nanoTime() + "@example.com");
		String billsPath = "/api/v1/purchases/bills";

		String billJson = mockMvc.perform(post(billsPath)
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(billRequest("SUP-100", LocalDate.now(), LocalDate.now().plusDays(20))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.billNumber").value("SUP-100"))
				.andExpect(jsonPath("$.category").value("Software"))
				.andExpect(jsonPath("$.taxAmount").value(12.00))
				.andExpect(jsonPath("$.totalAmount").value(212.00))
				.andExpect(jsonPath("$.balance").value(212.00))
				.andExpect(jsonPath("$.status").value("ISSUED"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String billId = extractValue(billJson, "id");

		mockMvc.perform(get(billsPath).header("Authorization", "Bearer " + outsiderToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
		mockMvc.perform(post(billsPath)
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(billRequest("sup-100", LocalDate.now(), LocalDate.now().plusDays(20))))
				.andExpect(status().isConflict());
		mockMvc.perform(post(billsPath)
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(billRequest("SUP-INVALID", LocalDate.now(), LocalDate.now().minusDays(1))))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post(billsPath + "/" + billId + "/payments")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"amount":100.00,"paymentDate":"%s","note":"Bank transfer"}
								""".formatted(LocalDate.now())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.amountPaid").value(100.00))
				.andExpect(jsonPath("$.balance").value(112.00))
				.andExpect(jsonPath("$.status").value("PARTIALLY_PAID"));

		mockMvc.perform(post(billsPath + "/" + billId + "/payments")
						.header("Authorization", "Bearer " + outsiderToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"amount":1.00,"paymentDate":"%s"}
								""".formatted(LocalDate.now())))
				.andExpect(status().isNotFound());

		String staffInvite = createInvitation(ownerToken, "purchase-staff-" + System.nanoTime() + "@example.com", "STAFF");
		String staffToken = registerWithInvitation(extractValue(staffInvite, "token"), extractValue(staffInvite, "email"));
		mockMvc.perform(get(billsPath).header("Authorization", "Bearer " + staffToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(post(billsPath + "/" + billId + "/payments")
						.header("Authorization", "Bearer " + staffToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"amount":112.00,"paymentDate":"%s"}
								""".formatted(LocalDate.now())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PAID"))
				.andExpect(jsonPath("$.balance").value(0.00));

		String viewerInvite = createInvitation(ownerToken, "purchase-viewer-" + System.nanoTime() + "@example.com", "VIEWER");
		String viewerToken = registerWithInvitation(
				extractValue(viewerInvite, "token"), extractValue(viewerInvite, "email"));
		mockMvc.perform(get(billsPath).header("Authorization", "Bearer " + viewerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(post(billsPath)
						.header("Authorization", "Bearer " + viewerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(billRequest("SUP-VIEWER", LocalDate.now(), LocalDate.now().plusDays(10))))
				.andExpect(status().isForbidden());
		mockMvc.perform(post(billsPath + "/" + billId + "/payments")
						.header("Authorization", "Bearer " + viewerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"amount":1.00,"paymentDate":"%s"}
								""".formatted(LocalDate.now())))
				.andExpect(status().isForbidden());
	}

	@Test
	void marksPastDueBillsOverdue() throws Exception {
		String token = registerAndGetToken("purchase-overdue-" + System.nanoTime() + "@example.com");
		mockMvc.perform(post("/api/v1/purchases/bills")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(billRequest(
								"SUP-OVERDUE",
								LocalDate.now().minusDays(10),
								LocalDate.now().minusDays(1))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("OVERDUE"));
	}

	private String billRequest(String number, LocalDate issueDate, LocalDate dueDate) {
		return """
				{
				  "supplierName":"Cloud Software Inc",
				  "billNumber":"%s",
				  "category":"Software",
				  "description":"Annual accounting software subscription",
				  "issueDate":"%s",
				  "dueDate":"%s",
				  "currency":"eur",
				  "subtotal":200.00,
				  "taxRate":6.00
				}
				""".formatted(number, issueDate, dueDate);
	}

	private String registerAndGetToken(String email) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"secure-password","firstName":"Purchase","lastName":"Accountant"}
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return extractValue(response, "token");
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
		String response = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email":"%s",
								  "password":"secure-password",
								  "firstName":"Purchase",
								  "lastName":"Teammate",
								  "invitationToken":"%s"
								}
								""".formatted(email, invitationToken)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return extractValue(response, "token");
	}

	private String extractValue(String response, String property) {
		return response.replaceFirst(".*\"" + property + "\":\"([^\"]+)\".*", "$1");
	}
}
