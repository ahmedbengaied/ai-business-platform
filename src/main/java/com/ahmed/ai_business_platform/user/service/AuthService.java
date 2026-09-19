package com.ahmed.ai_business_platform.user.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ahmed.ai_business_platform.security.JwtService;
import com.ahmed.ai_business_platform.user.dto.AuthResponse;
import com.ahmed.ai_business_platform.user.dto.LoginRequest;
import com.ahmed.ai_business_platform.user.dto.RegisterRequest;
import com.ahmed.ai_business_platform.user.dto.UserResponse;
import com.ahmed.ai_business_platform.user.entity.User;
import com.ahmed.ai_business_platform.user.repository.UserRepository;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new IllegalArgumentException("Email is already registered");
		}

		User user = new User(
				email,
				passwordEncoder.encode(request.password()),
				request.firstName().trim(),
				request.lastName().trim());
		userRepository.save(user);
		return createResponse(user);
	}

	public AuthResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(email, request.password()));
		UserDetails principal = (UserDetails) authentication.getPrincipal();
		User user = userRepository.findByEmailIgnoreCase(principal.getUsername())
				.orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
		return createResponse(user);
	}

	public UserResponse currentUser(String email) {
		return userRepository.findByEmailIgnoreCase(email)
				.map(UserResponse::from)
				.orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
	}

	private AuthResponse createResponse(User user) {
		return new AuthResponse(
				jwtService.generateToken(user),
				"Bearer",
				jwtService.getExpirationSeconds(),
				UserResponse.from(user));
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}
}
