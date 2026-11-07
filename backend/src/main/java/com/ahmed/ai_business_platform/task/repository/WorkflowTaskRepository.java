package com.ahmed.ai_business_platform.task.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ahmed.ai_business_platform.task.entity.WorkflowTask;

public interface WorkflowTaskRepository extends JpaRepository<WorkflowTask, String> {

	List<WorkflowTask> findAllByWorkflow_IdOrderByCreatedAtAsc(String workflowId);

	Optional<WorkflowTask> findByIdAndWorkflow_Id(String id, String workflowId);
}
