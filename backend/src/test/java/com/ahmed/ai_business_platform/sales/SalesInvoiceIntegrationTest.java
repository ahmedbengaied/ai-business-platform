package com.ahmed.ai_business_platform.sales;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SalesInvoiceIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void managesFirmScopedCustomersInvoicesPaymentsAndReadOnlyAccess() throws Exception {
		String ownerToken = registerAndGetToken("sales-owner-" + System.nanoTime() + "@example.com");
		String otherFirmToken = registerAndGetToken("sales-other-" + System.nanoTime() + "@example.com");

		String customerJson = mockMvc.perform(post("/api/v1/sales/customers")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Northwind Supplies","email":"billing@northwind.example","phone":"+15550100"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Northwind Supplies"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String customerId = extractId(customerJson);

		mockMvc.perform(get("/api/v1/sales/customers").header("Authorization", "Bearer " + otherFirmToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
		mockMvc.perform(post("/api/v1/sales/invoices")
						.header("Authorization", "Bearer " + otherFirmToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(invoiceRequest("INV-OTHER", customerId, LocalDate.now(), LocalDate.now().plusDays(5))))
				.andExpect(status().isNotFound());

		String invoiceJson = mockMvc.perform(post("/api/v1/sales/invoices")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(invoiceRequest("INV-1001", customerId, LocalDate.now(), LocalDate.now().plusDays(30))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.invoiceNumber").value("INV-1001"))
				.andExpect(jsonPath("$.currency").value("USD"))
				.andExpect(jsonPath("$.taxAmount").value(100.00))
				.andExpect(jsonPath("$.totalAmount").value(1100.00))
				.andExpect(jsonPath("$.balance").value(1100.00))
				.andExpect(jsonPath("$.status").value("ISSUED"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String invoiceId = extractId(invoiceJson);

		mockMvc.perform(post("/api/v1/sales/invoices")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(invoiceRequest("inv-1001", customerId, LocalDate.now(), LocalDate.now().plusDays(30))))
				.andExpect(status().isConflict());
		mockMvc.perform(post("/api/v1/sales/invoices")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(invoiceRequest("INV-INVALID", customerId, LocalDate.now(), LocalDate.now().minusDays(1))))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/v1/sales/invoices/" + invoiceId + "/payments")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"amount":300.00,"paymentDate":"%s","note":"Bank transfer"}
								""".formatted(LocalDate.now())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.amountPaid").value(300.00))
				.andExpect(jsonPath("$.balance").value(800.00))
				.andExpect(jsonPath("$.status").value("PARTIALLY_PAID"))
				.andExpect(jsonPath("$.payments.length()").value(1));

		mockMvc.perform(post("/api/v1/sales/invoices/" + invoiceId + "/payments")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"amount":800.01,"paymentDate":"%s"}
								""".formatted(LocalDate.now())))
				.andExpect(status().isBadRequest());

		String invitationJson = mockMvc.perform(post("/api/v1/firm/invitations")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"sales-staff-%s@example.com","role":"STAFF"}
								""".formatted(System.nanoTime())))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String staffToken = registerWithInvitation(
				extractValue(invitationJson, "token"),
				extractValue(invitationJson, "email"));
		mockMvc.perform(get("/api/v1/sales/invoices").header("Authorization", "Bearer " + staffToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].customerName").value("Northwind Supplies"));
		mockMvc.perform(post("/api/v1/sales/invoices/" + invoiceId + "/payments")
						.header("Authorization", "Bearer " + staffToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"amount":800.00,"paymentDate":"%s"}
								""".formatted(LocalDate.now())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PAID"))
				.andExpect(jsonPath("$.balance").value(0.00))
				.andExpect(jsonPath("$.payments.length()").value(2));

		String viewerInvitationJson = mockMvc.perform(post("/api/v1/firm/invitations")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"sales-viewer-%s@example.com","role":"VIEWER"}
								""".formatted(System.nanoTime())))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String viewerToken = registerWithInvitation(
				extractValue(viewerInvitationJson, "token"),
				extractValue(viewerInvitationJson, "email"));
		mockMvc.perform(get("/api/v1/sales/invoices").header("Authorization", "Bearer " + viewerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(post("/api/v1/sales/customers")
						.header("Authorization", "Bearer " + viewerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Viewer cannot add"}
								"""))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/sales/invoices/" + invoiceId + "/payments")
						.header("Authorization", "Bearer " + viewerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"amount":1.00,"paymentDate":"%s"}
								""".formatted(LocalDate.now())))
				.andExpect(status().isForbidden());
	}

	@Test
	void reportsOverdueAndRejectsPaymentsForInvoicesInAnotherFirm() throws Exception {
		String ownerToken = registerAndGetToken("sales-overdue-" + System.nanoTime() + "@example.com");
		String otherToken = registerAndGetToken("sales-outsider-" + System.nanoTime() + "@example.com");
		String customerJson = mockMvc.perform(post("/api/v1/sales/customers")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Past-due customer"}
								"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String invoiceJson = mockMvc.perform(post("/api/v1/sales/invoices")
						.header("Authorization", "Bearer " + ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(invoiceRequest(
								"INV-OVERDUE",
								extractId(customerJson),
								LocalDate.now().minusDays(10),
								LocalDate.now().minusDays(1))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("OVERDUE"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		mockMvc.perform(post("/api/v1/sales/invoices/" + extractId(invoiceJson) + "/payments")
						.header("Authorization", "Bearer " + otherToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"amount":1.00,"paymentDate":"%s"}
								""".formatted(LocalDate.now())))
				.andExpect(status().isNotFound());
	}

	private String invoiceRequest(String number, String customerId, LocalDate issueDate, LocalDate dueDate) {
		return """
				{
				  "invoiceNumber":"%s",
				  "customerId":"%s",
				  "description":"Monthly bookkeeping services",
				  "issueDate":"%s",
				  "dueDate":"%s",
				  "currency":"usd",
				  "subtotal":1000.00,
				  "taxRate":10.00
				}
				""".formatted(number, customerId, issueDate, dueDate);
	}

	private String registerAndGetToken(String email) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email":"%s",
								  "password":"secure-password",
								  "firstName":"Sales",
								  "lastName":"Accountant"
								}
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return extractValue(response, "token");
	}

	private String registerWithInvitation(String invitationToken, String email) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email":"%s",
								  "password":"secure-password",
								  "firstName":"Sales",
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

	private String extractId(String response) {
		return extractValue(response, "id");
	}

	private String extractValue(String response, String property) {
		return response.replaceFirst(".*\"" + property + "\":\"([^\"]+)\".*", "$1");
	}
}
