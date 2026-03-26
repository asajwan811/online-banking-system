package com.bank.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdminDashboardDTO {
    private long totalUsers;
    private long totalAccounts;
    private long activeAccounts;
    private long frozenAccounts;
    private long totalTransactions;
    private long completedTransactions;
    private long failedTransactions;
    private BigDecimal totalMoneyInSystem;
}
