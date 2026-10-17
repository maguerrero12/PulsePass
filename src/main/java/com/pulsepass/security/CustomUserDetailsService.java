package com.pulsepass.security;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

//Spring Security llamará automáticamente cuando alguien intente hacer login o cuando se valide un token.
// Buscará al usuario, sus credenciales y sus roles.

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final UserRoleRepository roleRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    UserCredentialRepository credentialRepository,
                                    UserRoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // Buscar al usuario por email o por username (soportamos ambos logins)
        User user = userRepository.findByEmail(identifier)
                .orElseGet(() -> userRepository.findByUsername(identifier)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + identifier)));

        // Buscar el hash de su contraseña
        UserCredential credential = credentialRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Credentials not found for user"));

        // cuscamos roles
        List<UserRole> roles = roleRepository.findByUserId(user.getId());

        // Convertir los roles al formato que entiende Spring Security ("ROLE_USER", "ROLE_ADMIN")
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(userRole -> new SimpleGrantedAuthority("ROLE_" + userRole.getRole()))
                .collect(Collectors.toList());

        // Retornar el Principal ensamblado
        return new SecurityPrincipal(user, credential.getPasswordHash(), authorities);
    }
}