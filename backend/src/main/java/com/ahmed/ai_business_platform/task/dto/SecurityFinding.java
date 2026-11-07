package com.ahmed.ai_business_platform.task.dto;

public record SecurityFinding(String ruleId, String severity, String path, int line) {
}
