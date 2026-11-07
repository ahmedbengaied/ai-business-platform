package com.ahmed.ai_business_platform.firm.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
public class FirmInvitationTokens {

	private final SecureRandom secureRandom = new SecureRandom();

	public String create() {
		byte[] token = new byte[32];
		secureRandom.nextBytes(token);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
	}

	public String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}
}
