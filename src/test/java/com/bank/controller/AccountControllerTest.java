package com.bank.controller;

import com.bank.dto.CreateAccountRequest;
import com.bank.security.JwtUtil;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        UserDetails adminDetails = new User("admin", "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        adminToken = jwtUtil.generateToken(adminDetails);

        UserDetails userDetails = new User("regularuser", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        userToken = jwtUtil.generateToken(userDetails);
    }

    @Test
    void getAccount_WithoutAuth_Returns401() throws Exception {
        mockMvc.perform(get("/api/accounts/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAccount_NonExistent_Returns404() throws Exception {
        mockMvc.perform(get("/api/accounts/99999")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void freezeAccount_AsUser_Returns403() throws Exception {
        mockMvc.perform(put("/api/accounts/1/freeze")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAccount_InvalidType_Returns400() throws Exception {
        // First register a user to authenticate as
        com.bank.dto.RegisterRequest reg = new com.bank.dto.RegisterRequest();
        reg.setUsername("accounttest_" + System.currentTimeMillis());
        reg.setPassword("Password123!");
        reg.setEmail("accounttest_" + System.currentTimeMillis() + "@test.com");
        reg.setFullName("Account Test User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());
    }

    @Test
    void getMyAccounts_WithValidToken_ReturnsOk() throws Exception {
        // Register then login to get a real token
        long ts = System.currentTimeMillis();
        com.bank.dto.RegisterRequest reg = new com.bank.dto.RegisterRequest();
        reg.setUsername("myaccts_" + ts);
        reg.setPassword("Password123!");
        reg.setEmail("myaccts_" + ts + "@test.com");
        reg.setFullName("My Accounts User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        com.bank.dto.LoginRequest login = new com.bank.dto.LoginRequest();
        login.setUsername("myaccts_" + ts);
        login.setPassword("Password123!");

        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).get("token").asText();

        mockMvc.perform(get("/api/accounts/my-accounts")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
