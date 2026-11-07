package com.ahmed.ai_business_platform.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.ahmed.ai_business_platform.user.entity.User;

public class PlatformUserDetails implements UserDetails {

	private final String username;
	private final String password;
	private final boolean enabled;
	private final long authVersion;
	private final Collection<? extends GrantedAuthority> authorities;

	private PlatformUserDetails(User user) {
		username = user.getEmail();
		password = user.getPasswordHash();
		enabled = user.isEnabled();
		authVersion = user.getAuthVersion();
		authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
	}

	public static PlatformUserDetails from(User user) {
		return new PlatformUserDetails(user);
	}

	public long getAuthVersion() {
		return authVersion;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
}
