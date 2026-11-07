package com.ahmed.ai_business_platform.ai.usage;

import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ahmed.ai_business_platform.user.repository.UserRepository;

@RestController
@RequestMapping("/api/v1/ai/usage")
public class AiUsageController {

	private final AiUsageService usageService;
	private final UserRepository userRepository;

	public AiUsageController(AiUsageService usageService, UserRepository userRepository) {
		this.usageService = usageService;
		this.userRepository = userRepository;
	}

	@GetMapping
	public AiUsageResponse getCurrentUsage(Authentication authentication) {
		String userId = userRepository.findByEmailIgnoreCase(authentication.getName())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED))
				.getId();
		return usageService.getCurrentUsage(userId);
	}
}
