package com.ahmed.ai_business_platform.task.dto;

import com.ahmed.ai_business_platform.task.entity.TaskStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(@NotNull TaskStatus status) {
}
