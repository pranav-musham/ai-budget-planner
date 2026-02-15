package com.receiptscan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recurring_expenses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 50)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Frequency frequency;

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    @Column(name = "last_processed_date")
    private LocalDate lastProcessedDate;

    @Column(name = "reminder_days")
    @Builder.Default
    private Integer reminderDays = 3;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_paused")
    @Builder.Default
    private Boolean isPaused = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate the next due date based on frequency
     */
    public void advanceToNextDueDate() {
        if (this.nextDueDate == null) return;

        switch (frequency) {
            case DAILY:
                this.nextDueDate = this.nextDueDate.plusDays(1);
                break;
            case WEEKLY:
                this.nextDueDate = this.nextDueDate.plusWeeks(1);
                break;
            case MONTHLY:
                this.nextDueDate = this.nextDueDate.plusMonths(1);
                break;
            case YEARLY:
                this.nextDueDate = this.nextDueDate.plusYears(1);
                break;
        }
        this.lastProcessedDate = LocalDate.now();
    }

    /**
     * Check if a reminder should be sent
     */
    public boolean shouldSendReminder() {
        if (isPaused || !isActive) return false;
        LocalDate reminderDate = nextDueDate.minusDays(reminderDays);
        return !LocalDate.now().isBefore(reminderDate) && LocalDate.now().isBefore(nextDueDate);
    }

    /**
     * Check if this expense is due today or overdue
     */
    public boolean isDueOrOverdue() {
        if (isPaused || !isActive) return false;
        return !LocalDate.now().isBefore(nextDueDate);
    }

    public enum Frequency {
        DAILY, WEEKLY, MONTHLY, YEARLY
    }
}
