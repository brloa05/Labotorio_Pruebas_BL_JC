package edu.eci.arsw.testing.dto;

import java.time.Instant;

public record OrderResponse(
        String id,
        String customerId,
        double total,
        String status,
        Instant createdAt
) {}
