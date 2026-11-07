package com.ahmed.ai_business_platform.firm.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ahmed.ai_business_platform.firm.entity.FirmRole;
import com.ahmed.ai_business_platform.user.entity.User;
import com.ahmed.ai_business_platform.user.repository.UserRepository;

@Service
public class FirmAccessService {

	private final UserRepository userRepository;

	public FirmAccessService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public User requireUser(String email) {
		return userRepository.findByEmailIgnoreCaseAndEnabledTrue(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}

	public User requireCanWrite(String email) {
		User user = requireUser(email);
		if (user.getFirmRole() == FirmRole.VIEWER) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Viewer access is read-only");
		}
		return user;
	}

	public User requireManager(String email) {
		User user = requireUser(email);
		if (user.getFirmRole() != FirmRole.OWNER && user.getFirmRole() != FirmRole.ADMIN) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Firm owner or admin access is required");
		}
		return user;
	}
}
