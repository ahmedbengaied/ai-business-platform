package com.ahmed.ai_business_platform.task.service;

public record AiExecutionResult(
		String result,
		long promptTokens,
		long completionTokens) {
}
