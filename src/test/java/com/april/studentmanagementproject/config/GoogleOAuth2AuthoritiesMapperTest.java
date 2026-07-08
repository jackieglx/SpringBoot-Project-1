package com.april.studentmanagementproject.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleOAuth2AuthoritiesMapperTest {

    @Test
    void mapsConfiguredAdminEmailToUserAndAdminRoles() {
        AppSecurityProperties properties = new AppSecurityProperties();
        properties.setAdminEmails(List.of("jackiewangjiayi513@gmail.com"));
        GoogleOAuth2AuthoritiesMapper mapper = new GoogleOAuth2AuthoritiesMapper(properties);

        var authorities = mapper.mapAuthorities(List.of(
                new OAuth2UserAuthority(Map.of("email", "jackiewangjiayi513@gmail.com"))));

        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void mapsNonAdminGoogleUserToUserRoleOnly() {
        AppSecurityProperties properties = new AppSecurityProperties();
        properties.setAdminEmails(List.of("jackiewangjiayi513@gmail.com"));
        GoogleOAuth2AuthoritiesMapper mapper = new GoogleOAuth2AuthoritiesMapper(properties);

        var authorities = mapper.mapAuthorities(List.of(
                new OAuth2UserAuthority(Map.of("email", "student@example.com"))));

        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertTrue(authorities.stream().noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")));
    }
}
