package com.pulsepass.dto.response;

import java.util.List;

public record AuthenticatedUserResponse(
        Long id,
        String username,
        String email,
        boolean active,
        List<String> roles
) {
}
