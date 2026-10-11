package com.pulsepass.controller;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService service;

    //TEST-CTRL-USR-001 registrar válido → 201
    @Test
    void shouldRegisterValidUser() throws Exception{

        UserResponse response = new UserResponse(
                1L,
                "andrea",
                "andrea@email.com",
                true
        );

        when(service.register(any(RegisterUserRequest.class))).thenReturn(response);

        String request = """
                {
                    "username": "andrea",
                    "email": "andrea@email.com",
                    "firstName": "Andrea",
                    "lastName": "Garcia",
                    "phone": "3001234567",
                    "city": "Santa Marta",
                    "birthDate": "2001-06-10"
                }
                """;

        mockMvc.perform(post("/api/users")
                    .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("andrea"));

        verify(service).register(any(RegisterUserRequest.class));
    }

    //TEST-CTRL-USR-002 email inválido → 400
    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception{

        String request = """
                {
                    "username": "laura",
                    "email": "laura-email-invalido",
                    "firstName": "Laura",
                    "lastName": "Martinez",
                    "phone": "3009876543",
                    "city": "Santa Marta",
                    "birthDate": "2009-07-20"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.email")
                        .value("Email must be valid"));

        verify(service, never()).register(any(RegisterUserRequest.class));
    }

    //TEST-CTRL-USR-003 username duplicado → 409
    @Test
    void shouldReturn409WhenUsernameExists() throws Exception{

        when(service.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException(
                        "This username already exists: carlos"));

        String request = """
                {
                    "username": "carlos",
                    "email": "carlos@email.com",
                    "firstName": "Carlos",
                    "lastName": "Rodriguez",
                    "phone": "3011234567",
                    "city": "Santa Marta",
                    "birthDate": "2005-04-15"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("This username already exists: carlos"));

        verify(service).register(any(RegisterUserRequest.class));
    }

    //TEST-CTRL-USR-004 consultar por email → 200
    @Test
    void shouldReturnUserByEmail() throws Exception{

        UserResponse response = new UserResponse(
                4L,
                "miguel",
                "miguel@email.com",
                false
        );

        when(service.findByEmail("miguel@email.com")).thenReturn(response);

        mockMvc.perform(get("/api/users/by-email")
                    .param("email", "miguel@email.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("miguel@email.com"));

        verify(service).findByEmail("miguel@email.com");
    }

    //TEST-CTRL-USR-005 consultar por username → 200
    @Test
    void shouldReturnUserByUsername() throws Exception{

        UserResponse response = new UserResponse(
                2L,
                "carlos",
                "carlos@email.com",
                true
        );

        when(service.findByUsername("carlos")).thenReturn(response);

        mockMvc.perform(get("/api/users/by-username").param("username", "carlos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("carlos"));

        verify(service).findByUsername("carlos");
    }
}