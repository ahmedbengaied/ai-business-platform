package com.ahmed.ai_business_platform.project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ahmed.ai_business_platform.project.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, String> {

	List<Project> findAllByOwner_IdOrderByCreatedAtDesc(String ownerId);

	Optional<Project> findByIdAndOwner_Id(String id, String ownerId);
}
