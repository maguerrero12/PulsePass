package com.pulsepass.controller;

import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.RestAccessDeniedHandler;
import com.pulsepass.security.RestAuthenticationEntryPoint;
import com.pulsepass.security.SecurityConfig;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.AuthorizationService;
import com.pulsepass.service.UserService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AuthController.class, UserController.class})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, GlobalExceptionHandler.class})
public class AuthControllerSecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean private AuthService authService;
    @MockitoBean private UserService userService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;
    @MockitoBean private AuthorizationService authorizationService;

    @BeforeEach
    public void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // T-SEC-005: Anónimo POST /api/auth/login correcto -> 200 con Bearer
    @Test
    public void tSec005_anonymous_LoginCorrecto_ReturnsOkWithBearer() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("jwt-token-valido", "Bearer", 900));

        String json = "{\"identifier\":\"andrea\", \"password\":\"PasswordSegura123!\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("jwt-token-valido"));
    }

    // T-SEC-006: Login con contraseña incorrecta -> 401 genérico
    @Test
    public void tSec006_anonymous_LoginIncorrecto_ReturnsUnauthorized() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        String json = "{\"identifier\":\"andrea\", \"password\":\"Mal123456789!\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }

    // T-SEC-007: Token expirado en ruta protegida -> 401 con ErrorResponse
    @Test
    public void tSec007_expiredToken_ReturnsUnauthorizedWithErrorResponse() throws Exception {
        when(jwtService.validateAndGetClaims("token-expirado"))
                .thenThrow(new RuntimeException("Token expired"));

        mockMvc.perform(get("/api/users/by-email?email=andrea@mail.com")
                        .header("Authorization", "Bearer token-expirado"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // T-SEC-008: Token con firma alterada -> 401, sin invocar Service
    @Test
    public void tSec008_tamperedToken_ReturnsUnauthorizedWithoutInvokingService() throws Exception {
        when(jwtService.validateAndGetClaims("token-alterado"))
                .thenThrow(new RuntimeException("Invalid signature"));

        mockMvc.perform(get("/api/users/by-email?email=andrea@mail.com")
                        .header("Authorization", "Bearer token-alterado"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).findByEmail(anyString());
    }

    // T-SEC-015: USER busca datos de otro usuario -> 403
    @Test
    @WithMockUser(roles = "USER", username = "andrea@mail.com")
    public void tSec015_user_BuscarDatosAjenos_ReturnsForbidden() throws Exception {
        doThrow(new AccessDeniedException("Denied"))
                .when(authorizationService).verifyUserAccessByEmail("carlos@mail.com");

        mockMvc.perform(get("/api/users/by-email?email=carlos@mail.com"))
                .andExpect(status().isForbidden());
    }

    // T-SEC-016: Un token válido pertenece a usuario inactivo -> 401
    @Test
    public void tSec016_inactiveUserToken_ReturnsUnauthorized() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(jwtService.validateAndGetClaims("token-usuario-inactivo")).thenReturn(claims);
        when(customUserDetailsService.loadUserByUsername("1"))
                .thenThrow(new DisabledException("User is disabled"));

        mockMvc.perform(get("/api/users/by-email?email=inactivo@mail.com")
                        .header("Authorization", "Bearer token-usuario-inactivo"))
                .andExpect(status().isUnauthorized());
    }

    // T-SEC-019: Login o JWT da error -> No devuelve stacktrace, password ni hash
    @Test
    public void tSec019_loginError_DoesNotLeakSensitiveDataOrStacktrace() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        String json = "{\"identifier\":\"andrea\", \"password\":\"ClaveSecreta123!\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString("ClaveSecreta123!"))))
                .andExpect(content().string(not(containsString("StackTrace"))))
                .andExpect(content().string(not(containsString("java.lang"))));
    }

    // T-SEC-020: Pruebas REST previas con identidad apropiada -> Mantienen códigos y DTOs exitosos
    @Test
    @WithMockUser(roles = "USER", username = "andrea@mail.com")
    public void tSec020_authenticatedUser_GetOwnData_MaintainsPreviousContract() throws Exception {
        when(userService.findByEmail("andrea@mail.com"))
                .thenReturn(new UserResponse(1L, "andrea", "andrea@mail.com", true));

        mockMvc.perform(get("/api/users/by-email?email=andrea@mail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@mail.com"));
    }
}