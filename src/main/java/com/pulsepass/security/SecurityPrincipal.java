package com.pulsepass.security;

import com.pulsepass.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class SecurityPrincipal implements UserDetails {

    private final User user;
    private final String passwordHash;
    private final Collection<? extends GrantedAuthority> authorities;

    public SecurityPrincipal(User user, String passwordHash, List<SimpleGrantedAuthority> authorities) {
        this.user = user;
        this.passwordHash = passwordHash;
        this.authorities = authorities;
    }

    // Método extra para poder acceder al usuario original si lo necesitamos en los Controllers
    public User getUser() {
        return user;
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return user.getUsername();
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

    // SEC-011: Si el usuario está inactivo, Spring Security le niega el acceso
    @Override
    public boolean isEnabled() {
        return user.getActive();
    }
}