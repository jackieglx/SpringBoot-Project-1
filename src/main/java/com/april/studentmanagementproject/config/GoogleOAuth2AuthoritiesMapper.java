package com.april.studentmanagementproject.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class GoogleOAuth2AuthoritiesMapper {

    private final AppSecurityProperties securityProperties;

    public GoogleOAuth2AuthoritiesMapper(AppSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
        mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        String email = extractEmail(authorities);
        if (email != null && isAdminEmail(email)) {
            mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        return mappedAuthorities;
    }

    private String extractEmail(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .filter(OAuth2UserAuthority.class::isInstance)
                .map(OAuth2UserAuthority.class::cast)
                .map(authority -> authority.getAttributes().get("email"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst()
                .orElse(null);
    }

    private boolean isAdminEmail(String email) {
        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        return securityProperties.getAdminEmails().stream()
                .map(adminEmail -> adminEmail.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedEmail::equals);
    }
}
