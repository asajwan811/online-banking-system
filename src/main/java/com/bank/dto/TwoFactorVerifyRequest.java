package com.bank.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TwoFactorVerifyRequest {
    @NotNull(message = "TOTP code is required")
    private Integer code;
}
