package com.mnco.security.service;

import com.mnco.domain.entities.User;
import com.mnco.domain.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Bridges the domain UserRepository with Spring Security's authentication mechanism.
 * Returns MncoUserDetails which carries the platform UUID so controllers never
 * need to hit UserRepository just to resolve the current user's ID.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found for authentication: '{}'", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        return new MncoUserDetails(user);
    }

    // ── MncoUserDetails ───────────────────────────────────────────────────────

    /**
     * Custom UserDetails implementation that carries the platform UUID.
     * Inject with @AuthenticationPrincipal MncoUserDetails in controllers.
     */
    @Getter
    public static class MncoUserDetails implements UserDetails {

        private final UUID userId;
        private final String username;
        private final String password;
        private final boolean enabled;
        private final Collection<? extends GrantedAuthority> authorities;
        private final boolean isAdmin;

        public MncoUserDetails(User user) {
            this.userId    = user.getId();
            this.username  = user.getUsername();
            this.password  = user.getPassword();
            this.enabled   = user.isEnabled();
            this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
            this.isAdmin   = user.isAdmin();
        }

        @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
        @Override public String getPassword()                                      { return password; }
        @Override public String getUsername()                                      { return username; }
        @Override public boolean isAccountNonExpired()                             { return true; }
        @Override public boolean isAccountNonLocked()                              { return true; }
        @Override public boolean isCredentialsNonExpired()                         { return true; }
        @Override public boolean isEnabled()                                       { return enabled; }
    }
}