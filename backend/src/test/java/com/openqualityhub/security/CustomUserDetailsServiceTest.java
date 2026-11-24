package com.openqualityhub.security;

import com.openqualityhub.entity.Organization;
import com.openqualityhub.entity.Permission;
import com.openqualityhub.entity.Role;
import com.openqualityhub.entity.User;
import com.openqualityhub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private User buildUserWithPermissions(String... permissionNames) {
        Organization org = Organization.builder().name("TestOrg").slug("test-org").build();
        org.setId(UUID.randomUUID());

        Set<Permission> permissions = new HashSet<>();
        for (String pn : permissionNames) {
            Permission p = Permission.builder()
                .name(pn)
                .module("test")
                .createdAt(Instant.now())
                .build();
            p.setId(UUID.randomUUID());
            permissions.add(p);
        }

        Role role = Role.builder()
            .name("QA_ENGINEER")
            .organization(org)
            .permissions(permissions)
            .build();
        role.setId(UUID.randomUUID());

        User user = User.builder()
            .email("test@example.com")
            .passwordHash("$2a$10$hashedPassword")
            .firstName("Test")
            .lastName("User")
            .displayName("Test User")
            .active(true)
            .organization(org)
            .roles(Set.of(role))
            .build();
        user.setId(UUID.randomUUID());

        return user;
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetails() {
        User user = buildUserWithPermissions("TEAM_READ", "PROJECT_READ");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("test@example.com");

        assertNotNull(details);
        assertEquals("test@example.com", details.getUsername());
        assertTrue(details.isEnabled());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.isAccountNonExpired());
        assertTrue(details.isCredentialsNonExpired());
    }

    @Test
    void loadUserByUsername_shouldThrowWhenNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
            service.loadUserByUsername("unknown@example.com"));
    }

    @Test
    void shouldMapPermissionsToAuthorities() {
        User user = buildUserWithPermissions("TEAM_READ", "TEAM_WRITE", "PROJECT_READ");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("test@example.com");

        Set<String> authorityNames = new HashSet<>();
        for (GrantedAuthority authority : details.getAuthorities()) {
            authorityNames.add(authority.getAuthority());
        }

        assertEquals(3, authorityNames.size());
        assertTrue(authorityNames.contains("TEAM_READ"));
        assertTrue(authorityNames.contains("TEAM_WRITE"));
        assertTrue(authorityNames.contains("PROJECT_READ"));
    }

    @Test
    void shouldMapMultipleRolesPermissions() {
        Organization org = Organization.builder().name("TestOrg").slug("test-org").build();
        org.setId(UUID.randomUUID());

        Permission p1 = Permission.builder().name("TEAM_READ").module("team").createdAt(Instant.now()).build();
        p1.setId(UUID.randomUUID());
        Permission p2 = Permission.builder().name("PROJECT_WRITE").module("project").createdAt(Instant.now()).build();
        p2.setId(UUID.randomUUID());
        Permission p3 = Permission.builder().name("DEFECT_READ").module("defect").createdAt(Instant.now()).build();
        p3.setId(UUID.randomUUID());

        Role role1 = Role.builder().name("QA_ENGINEER").organization(org).permissions(Set.of(p1, p2)).build();
        role1.setId(UUID.randomUUID());

        Role role2 = Role.builder().name("DEVELOPER").organization(org).permissions(Set.of(p2, p3)).build();
        role2.setId(UUID.randomUUID());

        User user = User.builder()
            .email("multi@example.com")
            .passwordHash("hash")
            .firstName("Multi")
            .lastName("Role")
            .displayName("Multi Role")
            .active(true)
            .organization(org)
            .roles(Set.of(role1, role2))
            .build();
        user.setId(UUID.randomUUID());

        when(userRepository.findByEmail("multi@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("multi@example.com");

        Set<String> authorityNames = new HashSet<>();
        for (GrantedAuthority authority : details.getAuthorities()) {
            authorityNames.add(authority.getAuthority());
        }

        // p2 is shared, so distinct count is 3
        assertEquals(3, authorityNames.size());
        assertTrue(authorityNames.contains("TEAM_READ"));
        assertTrue(authorityNames.contains("PROJECT_WRITE"));
        assertTrue(authorityNames.contains("DEFECT_READ"));
    }

    @Test
    void shouldReturnUserPrincipalType() {
        User user = buildUserWithPermissions("TEAM_READ");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("test@example.com");

        assertInstanceOf(UserPrincipal.class, details);
        UserPrincipal principal = (UserPrincipal) details;
        assertEquals(user.getId(), principal.getId());
        assertEquals(user.getOrganization().getId(), principal.getOrganizationId());
    }

    @Test
    void inactiveUser_shouldNotBeEnabled() {
        Organization org = Organization.builder().name("Org").slug("org").build();
        org.setId(UUID.randomUUID());

        User user = User.builder()
            .email("inactive@example.com")
            .passwordHash("hash")
            .firstName("Inactive")
            .lastName("User")
            .displayName("Inactive User")
            .active(false)
            .organization(org)
            .roles(Set.of())
            .build();
        user.setId(UUID.randomUUID());

        when(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("inactive@example.com");

        assertFalse(details.isEnabled());
        assertFalse(details.isAccountNonLocked());
    }
}
