package com.ahmed.ai_business_platform.workflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ahmed.ai_business_platform.workflow.entity.WorkflowRun;

public interface WorkflowRunRepository extends JpaRepository<WorkflowRun, String> {

	List<WorkflowRun> findAllByWorkflowIdOrderByStartedAtDesc(String workflowId);
}
