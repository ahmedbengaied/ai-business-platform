package com.ahmed.ai_business_platform.ai.usage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.ahmed.ai_business_platform.firm.entity.AccountingFirm;
import com.ahmed.ai_business_platform.firm.entity.FirmRole;
import com.ahmed.ai_business_platform.task.service.AiExecutionResult;
import com.ahmed.ai_business_platform.user.entity.User;
import com.ahmed.ai_business_platform.user.repository.UserRepository;

class AiUsageServiceTest {

	@Test
	void rejectsRequestBeforeProviderCallWhenMonthlyBudgetWouldBeExceeded() {
		AiUsageRepository usageRepository = mock(AiUsageRepository.class);
		UserRepository userRepository = mock(UserRepository.class);
		when(userRepository.findByIdForUpdate("user-1"))
				.thenReturn(Optional.of(new User(
						"test@example.com", "hash", "Test", "User", new AccountingFirm("Test Firm"), FirmRole.OWNER)));
		when(usageRepository.sumCostForPeriod(anyString(), any(), any()))
				.thenReturn(new BigDecimal("0.0099"));
		AiUsageService service = new AiUsageService(
				usageRepository, userRepository, new BigDecimal("0.01"), BigDecimal.ONE, BigDecimal.ONE);
		AtomicBoolean providerCalled = new AtomicBoolean(false);

		assertThrows(
				ResponseStatusException.class,
				() -> service.executeWithBudget(
						"user-1",
						"task-1",
						"test-model",
						10,
						800,
						() -> {
							providerCalled.set(true);
							return new AiExecutionResult("{}", 10, 10);
						}));

		assertEquals(false, providerCalled.get());
		verify(usageRepository, never()).save(any(AiUsageRecord.class));
	}

	@Test
	void recordsProviderTokenUsageAndCalculatedCost() {
		AiUsageRepository usageRepository = mock(AiUsageRepository.class);
		UserRepository userRepository = mock(UserRepository.class);
		when(userRepository.findByIdForUpdate("user-1"))
				.thenReturn(Optional.of(new User(
						"test@example.com", "hash", "Test", "User", new AccountingFirm("Test Firm"), FirmRole.OWNER)));
		when(usageRepository.sumCostForPeriod(anyString(), any(), any())).thenReturn(BigDecimal.ZERO);
		AiUsageService service = new AiUsageService(
				usageRepository,
				userRepository,
				new BigDecimal("1.00"),
				new BigDecimal("1.00"),
				new BigDecimal("2.00"));

		AiExecutionResult result = service.executeWithBudget(
				"user-1",
				"task-1",
				"test-model",
				100,
				800,
				() -> new AiExecutionResult("{\"summary\":\"Done\"}", 100, 50));

		assertEquals("{\"summary\":\"Done\"}", result.result());
		org.mockito.ArgumentCaptor<AiUsageRecord> recordCaptor =
				org.mockito.ArgumentCaptor.forClass(AiUsageRecord.class);
		verify(usageRepository).save(recordCaptor.capture());
		AiUsageRecord record = recordCaptor.getValue();
		assertEquals("user-1", record.getUserId());
		assertEquals("task-1", record.getTaskId());
		assertEquals(100, record.getPromptTokens());
		assertEquals(50, record.getCompletionTokens());
		assertEquals(new BigDecimal("0.00020000"), record.getCostUsd());
	}
}
