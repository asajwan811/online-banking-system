package com.bank.controller;

import com.bank.dto.DepositWithdrawRequest;
import com.bank.dto.TransactionDTO;
import com.bank.dto.TransferRequest;
import com.bank.security.JwtUtil;
import com.bank.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private String userToken;

    @BeforeEach
    void setUp() {
        UserDetails userDetails = new User("testuser", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        userToken = jwtUtil.generateToken(userDetails);
    }

    @Test
    void transfer_WithoutAuth_Returns401() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("1234567890123456");
        request.setToAccountNumber("9876543210987654");
        request.setAmount(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void transfer_WithAuth_InvalidAccount_Returns404() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("0000000000000000");
        request.setToAccountNumber("9999999999999999");
        request.setAmount(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/transactions/transfer")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void transfer_InvalidAmount_Returns400() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromAccountNumber("1234567890123456");
        request.setToAccountNumber("9876543210987654");
        request.setAmount(new BigDecimal("-50.00"));

        mockMvc.perform(post("/api/transactions/transfer")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHistory_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(get("/api/transactions/history/1234567890123456"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deposit_WithAuth_InvalidAccount_Returns404() throws Exception {
        DepositWithdrawRequest request = new DepositWithdrawRequest();
        request.setAmount(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/transactions/deposit/0000000000000000")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
