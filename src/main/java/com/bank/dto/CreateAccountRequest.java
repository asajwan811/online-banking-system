package com.bank.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAccountRequest {

    @NotBlank(message = "Account type is required")
    private String accountType;

    private String currency = "USD";
}
