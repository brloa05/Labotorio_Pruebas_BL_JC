package edu.eci.arsw.testing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
        @NotBlank String customerId,
        @Min(1) double total
) {}
