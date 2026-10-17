package com.pulsepass.security;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCredentialRepository credentialRepository;

    @Mock
    private UserRoleRepository roleRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    // Usuario existente
    @Test
    void shouldLoadUserByUsernameWhenUserExists() {
        String identifier = "andrea@email.com";
        Long userId = 1L;

        User user = new User();
        user.setId(userId);
        user.setEmail(identifier);
        user.setUsername("andrea");
        user.setActive(true);

        UserCredential credential = new UserCredential(
                user, "$2a$10$hashedPassword");
        UserRole userRole = new UserRole(user, "USER");

        when(userRepository.findByEmail(identifier)).thenReturn(Optional.of(user));
        when(credentialRepository.findById(userId)).thenReturn(Optional.of(credential));
        when(roleRepository.findByUserId(userId)).thenReturn(List.of(userRole));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(identifier);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("andrea");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");

        verify(userRepository).findByEmail(identifier);
        verify(credentialRepository).findById(userId);
        verify(roleRepository).findByUserId(userId);
    }

    // Usuario inexistente
    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        String nonexistentIdentifier = "noexist@email.com";

        when(userRepository.findByEmail(nonexistentIdentifier))
                .thenReturn(Optional.empty());
        when(userRepository.findByUsername(nonexistentIdentifier))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(nonexistentIdentifier))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(userRepository).findByEmail(nonexistentIdentifier);
        verify(userRepository).findByUsername(nonexistentIdentifier);
        verify(credentialRepository, never()).findById(any());
        verify(roleRepository, never()).findByUserId(any());
    }

    // Roles actualizados
    @Test
    void shouldReflectUpdatedRolesFromPersistence() {
        String identifier = "admin@email.com";
        Long userId = 2L;

        User user = new User();
        user.setId(userId);
        user.setEmail(identifier);
        user.setActive(true);

        UserCredential credential = new UserCredential(
                user, "$2a$10$hashedPassword");

        UserRole roleUser = new UserRole(user, "USER");
        UserRole roleAdmin = new UserRole(user, "ADMIN");

        when(userRepository.findByEmail(identifier)).thenReturn(Optional.of(user));
        when(credentialRepository.findById(userId)).thenReturn(Optional.of(credential));
        when(roleRepository.findByUserId(userId)).thenReturn(List.of(roleUser, roleAdmin));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(identifier);

        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");

        verify(roleRepository).findByUserId(userId);
    }

    // Estado inactivo
    @Test
    void shouldLoadInactiveUserStatusCorrectly() {
        String identifier = "inactive@email.com";
        Long userId = 3L;

        User user = new User();
        user.setId(userId);
        user.setEmail(identifier);
        user.setActive(false);

        UserCredential credential = new UserCredential(
                user, "$2a$10$hashedPassword");
        UserRole userRole = new UserRole(user, "USER");

        when(userRepository.findByEmail(identifier)).thenReturn(Optional.of(user));
        when(credentialRepository.findById(userId)).thenReturn(Optional.of(credential));
        when(roleRepository.findByUserId(userId)).thenReturn(List.of(userRole));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(identifier);

        assertThat(userDetails.isEnabled()).isFalse();

        verify(userRepository).findByEmail(identifier);
        verify(credentialRepository).findById(userId);
    }
}