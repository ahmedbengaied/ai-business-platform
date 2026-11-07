package com.ahmed.ai_business_platform.user.repository;

import java.util.Optional;
import java.util.List;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ahmed.ai_business_platform.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;

public interface UserRepository extends JpaRepository<User, String> {

	@EntityGraph(attributePaths = "firm")
	Optional<User> findByEmailIgnoreCase(String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select appUser from User appUser where appUser.id = :id")
	Optional<User> findByIdForUpdate(@Param("id") String id);

	boolean existsByEmailIgnoreCase(String email);

	@EntityGraph(attributePaths = "firm")
	Optional<User> findByEmailIgnoreCaseAndEnabledTrue(String email);

	List<User> findAllByFirm_IdAndEnabledTrueOrderByFirstNameAscLastNameAsc(String firmId);

	@EntityGraph(attributePaths = "firm")
	Optional<User> findByIdAndFirm_IdAndEnabledTrue(String id, String firmId);
}
