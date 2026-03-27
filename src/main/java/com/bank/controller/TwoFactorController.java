package com.bank.controller;

import com.bank.dto.TwoFactorSetupResponse;
import com.bank.dto.TwoFactorVerifyRequest;
import com.bank.service.TwoFactorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/2fa")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Two-Factor Authentication", description = "TOTP-based 2FA management (Google Authenticator compatible)")
public class TwoFactorController {

    @Autowired
    private TwoFactorService twoFactorService;

    @PostMapping("/setup")
    @Operation(summary = "Generate a TOTP secret and QR code URL for Google Authenticator")
    public ResponseEntity<TwoFactorSetupResponse> setup(@AuthenticationPrincipal UserDetails userDetails) {
        String qrUrl = twoFactorService.setupTwoFactor(userDetails.getUsername());
        return ResponseEntity.ok(new TwoFactorSetupResponse(qrUrl,
                "Scan the QR code URL with Google Authenticator, then call /api/2fa/verify to activate."));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify TOTP code and enable 2FA on the account")
    public ResponseEntity<Map<String, Object>> verify(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TwoFactorVerifyRequest request) {
        boolean success = twoFactorService.verifyAndEnable(userDetails.getUsername(), request.getCode());
        if (success) {
            return ResponseEntity.ok(Map.of("enabled", true, "message", "2FA enabled successfully"));
        }
        return ResponseEntity.badRequest().body(Map.of("enabled", false, "message", "Invalid TOTP code"));
    }

    @PostMapping("/disable")
    @Operation(summary = "Disable 2FA (requires valid TOTP code to confirm)")
    public ResponseEntity<Map<String, Object>> disable(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TwoFactorVerifyRequest request) {
        boolean success = twoFactorService.disableTwoFactor(userDetails.getUsername(), request.getCode());
        if (success) {
            return ResponseEntity.ok(Map.of("disabled", true, "message", "2FA disabled successfully"));
        }
        return ResponseEntity.badRequest().body(Map.of("disabled", false, "message", "Invalid TOTP code"));
    }

    @GetMapping("/status")
    @Operation(summary = "Check whether 2FA is enabled for current user")
    public ResponseEntity<Map<String, Object>> status(@AuthenticationPrincipal UserDetails userDetails) {
        boolean enabled = twoFactorService.isTwoFactorEnabled(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("twoFactorEnabled", enabled, "username", userDetails.getUsername()));
    }
}
