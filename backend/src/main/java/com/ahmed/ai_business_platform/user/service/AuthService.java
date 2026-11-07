package com.ahmed.ai_business_platform.user.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

import com.ahmed.ai_business_platform.firm.entity.FirmInvitation;
import com.ahmed.ai_business_platform.firm.entity.FirmRole;
import com.ahmed.ai_business_platform.firm.service.FirmService;
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
	private final FirmService firmService;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager,
			JwtService jwtService,
			FirmService firmService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.firmService = firmService;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		String firstName = request.firstName().trim();
		String lastName = request.lastName().trim();
		if (request.invitationToken() == null || request.invitationToken().isBlank()) {
			if (userRepository.existsByEmailIgnoreCase(email)) {
				throw new IllegalArgumentException("Email is already registered");
			}
			String passwordHash = passwordEncoder.encode(request.password());
			String firmName = request.firmName() == null || request.firmName().isBlank()
					? firstName + " " + lastName + " Accounting"
					: request.firmName().trim();
			var firm = firmService.createFirm(firmName);
			User user = new User(email, passwordHash, firstName, lastName, firm, FirmRole.OWNER);
			userRepository.save(user);
			return createResponse(user);
		}

		FirmInvitation invitation = firmService.acceptInvitation(request.invitationToken().trim(), email);
		var existingUser = userRepository.findByEmailIgnoreCase(email);
		if (existingUser.isPresent()) {
			User user = existingUser.get();
			if (user.isEnabled() || !user.getFirm().getId().equals(invitation.getFirm().getId())) {
				throw new ResponseStatusException(HttpStatus.CONFLICT, "This account cannot accept the invitation");
			}
			user.reactivate(
					passwordEncoder.encode(request.password()), firstName, lastName, invitation.getRole());
			return createResponse(user);
		}

		User user = new User(
				email,
				passwordEncoder.encode(request.password()),
				firstName,
				lastName,
				invitation.getFirm(),
				invitation.getRole());
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
		return userRepository.findByEmailIgnoreCaseAndEnabledTrue(email)
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
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
