package com.pulsepass.service.impl;

import com.pulsepass.domain.User;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository repository;
    @Mock
    private UserMapper mapper;

    @InjectMocks
    private UserServiceImpl service;

    @Test
    // TEST-USER-001
    void shouldRegisterValidUser(){

        User user = mock(User.class);

        RegisterUserRequest request = new RegisterUserRequest(
                "andrea",
                "andrea@email.com",
                "Andrea",
                "Gomez",
                "3001234567",
                "Santa Marta",
                LocalDate.of(2001, 5, 10)
        );

        UserResponse response = new UserResponse(
                1L,
                "andrea",
                "andrea@email.com",
                true
        );

        when(repository.existsByUsername("andrea")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);
        when(repository.save(any(User.class))).thenReturn(user);
        when(mapper.toResponse(user)) .thenReturn(response);

        UserResponse result = service.register(request);

        assertThat(result).isEqualTo(response);

        verify(repository).existsByUsername("andrea");
        verify(repository).existsByEmailIgnoreCase("andrea@email.com");
        verify(repository).save(any(User.class));
        verify(mapper).toResponse(user);
    }

    @Test
    // TEST-USER-002
    void shouldThrowExceptionWhenUsernameExists(){

        RegisterUserRequest request = new RegisterUserRequest(
                "carlos",
                "carlos@email.com",
                "Carlos",
                "Perez",
                "3009876543",
                "Santa Marta",
                LocalDate.of(2005, 3, 15)
        );

        when(repository.existsByUsername("carlos")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(repository).existsByUsername("carlos");
        verify(repository, never()).existsByEmailIgnoreCase(anyString());
        verify(repository, never()).save(any());
    }

    @Test
    // TEST-USER-003
    void shouldThrowExceptionWhenEmailExists(){

        RegisterUserRequest request = new RegisterUserRequest(
                "laura",
                "laura@email.com",
                "Laura",
                "Martinez",
                "3011234567",
                "Santa Marta",
                LocalDate.of(2009, 7, 20)
        );

        when(repository.existsByUsername("laura")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("laura@email.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(repository).existsByUsername("laura");
        verify(repository).existsByEmailIgnoreCase("laura@email.com");
        verify(repository, never()).save(any());
    }

    @Test
    // TEST-USER-004
    void shouldThrowExceptionWhenBirthDateIsInTheFuture(){
        RegisterUserRequest request = new RegisterUserRequest(
                "miguel",
                "miguel@email.com",
                "Miguel",
                "Rodriguez",
                "3007654321",
                "Santa Marta",
                LocalDate.of(2027, 1, 10)
        );

        when(repository.existsByUsername("miguel")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("miguel@email.com")).thenReturn(false);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(repository).existsByUsername("miguel");
        verify(repository).existsByEmailIgnoreCase("miguel@email.com");
        verify(repository, never()).save(any());
    }
}