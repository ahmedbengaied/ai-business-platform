package com.ahmed.ai_business_platform.ai.usage;

import java.math.BigDecimal;
import java.time.YearMonth;

public record AiUsageResponse(
		YearMonth period,
		BigDecimal monthlyLimitUsd,
		BigDecimal spentUsd,
		BigDecimal remainingUsd,
		long promptTokens,
		long completionTokens) {
}
