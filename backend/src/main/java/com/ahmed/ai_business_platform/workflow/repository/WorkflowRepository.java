package com.ahmed.ai_business_platform.workflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ahmed.ai_business_platform.workflow.entity.Workflow;

public interface WorkflowRepository extends JpaRepository<Workflow, String> {

	List<Workflow> findAllByProject_IdOrderByCreatedAtAsc(String projectId);

	Optional<Workflow> findByIdAndProject_Id(String id, String projectId);
}
