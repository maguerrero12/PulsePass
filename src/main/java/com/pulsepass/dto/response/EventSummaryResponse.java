package com.pulsepass.dto.response;

import com.pulsepass.domain.EventStatus;
import java.time.LocalDateTime;

// Versión reducida requerida por findPublishedEvents
public record EventSummaryResponse(
        String eventCode,
        String name,
        EventStatus status,
        LocalDateTime eventDate
) {}