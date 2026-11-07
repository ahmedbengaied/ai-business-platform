package com.ahmed.ai_business_platform.ai.usage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ahmed.ai_business_platform.task.service.AiExecutionResult;
import com.ahmed.ai_business_platform.user.repository.UserRepository;

@Service
public class AiUsageService {

	private static final BigDecimal TOKENS_PER_MILLION = new BigDecimal("1000000");
	private static final long TOKENS_PER_INPUT_CHARACTER_UPPER_BOUND = 4;
	private static final long REQUEST_OVERHEAD_TOKENS = 1000;
	private final AiUsageRepository usageRepository;
	private final UserRepository userRepository;
	private final BigDecimal monthlyLimitUsd;
	private final BigDecimal inputPricePerMillionTokensUsd;
	private final BigDecimal outputPricePerMillionTokensUsd;

	public AiUsageService(
			AiUsageRepository usageRepository,
			UserRepository userRepository,
			@Value("${app.ai.monthly-budget-usd:5.00}") BigDecimal monthlyLimitUsd,
			@Value("${app.ai.openai.input-usd-per-million-tokens:0.15}") BigDecimal inputPricePerMillionTokensUsd,
			@Value("${app.ai.openai.output-usd-per-million-tokens:0.60}") BigDecimal outputPricePerMillionTokensUsd) {
		if (monthlyLimitUsd.signum() <= 0
				|| inputPricePerMillionTokensUsd.signum() < 0
				|| outputPricePerMillionTokensUsd.signum() < 0) {
			throw new IllegalArgumentException("AI budget must be positive and token prices cannot be negative");
		}
		this.usageRepository = usageRepository;
		this.userRepository = userRepository;
		this.monthlyLimitUsd = monthlyLimitUsd;
		this.inputPricePerMillionTokensUsd = inputPricePerMillionTokensUsd;
		this.outputPricePerMillionTokensUsd = outputPricePerMillionTokensUsd;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public AiExecutionResult executeWithBudget(
			String userId,
			String taskId,
			String model,
			int promptCharacterCount,
			int maxOutputTokens,
			Supplier<AiExecutionResult> providerCall) {
		userRepository.findByIdForUpdate(userId)
				.orElseThrow(() -> new IllegalStateException("AI usage owner does not exist"));
		Period period = currentPeriod();
		BigDecimal spent = usageRepository.sumCostForPeriod(userId, period.start(), period.end());
		long estimatedPromptTokens = (long) promptCharacterCount * TOKENS_PER_INPUT_CHARACTER_UPPER_BOUND
				+ REQUEST_OVERHEAD_TOKENS;
		BigDecimal maximumCharge = calculateCost(estimatedPromptTokens, maxOutputTokens);
		if (spent.add(maximumCharge).compareTo(monthlyLimitUsd) > 0) {
			throw new ResponseStatusException(
					HttpStatus.TOO_MANY_REQUESTS,
					"Monthly AI usage limit reached; request was not sent to the provider");
		}

		AiExecutionResult result = providerCall.get();
		if (result.promptTokens() < 0 || result.completionTokens() < 0
				|| result.promptTokens() > estimatedPromptTokens
				|| result.completionTokens() > maxOutputTokens) {
			throw new IllegalStateException("AI provider returned invalid token usage");
		}
		BigDecimal actualCost = calculateCost(result.promptTokens(), result.completionTokens());
		if (spent.add(actualCost).compareTo(monthlyLimitUsd) > 0) {
			throw new IllegalStateException("AI provider usage exceeded the reserved monthly budget");
		}
		usageRepository.save(new AiUsageRecord(
				userId,
				taskId,
				model,
				result.promptTokens(),
				result.completionTokens(),
				actualCost));
		return result;
	}

	@Transactional(readOnly = true)
	public AiUsageResponse getCurrentUsage(String userId) {
		Period period = currentPeriod();
		BigDecimal spent = usageRepository.sumCostForPeriod(userId, period.start(), period.end());
		long promptTokens = usageRepository.sumPromptTokensForPeriod(userId, period.start(), period.end());
		long completionTokens = usageRepository.sumCompletionTokensForPeriod(userId, period.start(), period.end());
		return new AiUsageResponse(
				period.month(),
				monthlyLimitUsd,
				spent,
				monthlyLimitUsd.subtract(spent).max(BigDecimal.ZERO),
				promptTokens,
				completionTokens);
	}

	private BigDecimal calculateCost(long promptTokens, long completionTokens) {
		return BigDecimal.valueOf(promptTokens)
				.multiply(inputPricePerMillionTokensUsd)
				.add(BigDecimal.valueOf(completionTokens).multiply(outputPricePerMillionTokensUsd))
				.divide(TOKENS_PER_MILLION, 8, RoundingMode.HALF_UP);
	}

	private Period currentPeriod() {
		YearMonth month = YearMonth.now(ZoneOffset.UTC);
		return new Period(
				month,
				month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC),
				month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC));
	}

	private record Period(YearMonth month, Instant start, Instant end) {
	}
}
