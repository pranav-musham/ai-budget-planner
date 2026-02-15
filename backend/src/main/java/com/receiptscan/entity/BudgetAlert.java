package com.receiptscan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "budget_alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id")
    private Budget budget;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "current_spending", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentSpending;

    @Column(name = "budget_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal budgetLimit;

    @Column(name = "percentage_used", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentageUsed;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_suggestions", columnDefinition = "jsonb")
    private List<AISuggestion> aiSuggestions;

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum AlertType {
        REAL_TIME,
        WEEKLY_SUMMARY,
        MONTHLY_SUMMARY,
        ON_DEMAND
    }

    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AISuggestion {
        private String title;
        private String description;
        private BigDecimal potentialSavings;
        private String category;
    }
}
