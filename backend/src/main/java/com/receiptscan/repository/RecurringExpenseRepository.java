package com.receiptscan.repository;

import com.receiptscan.entity.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {

    List<RecurringExpense> findByUserId(Long userId);

    @Query("SELECT r FROM RecurringExpense r WHERE r.user.id = :userId AND r.isActive = true ORDER BY r.nextDueDate ASC")
    List<RecurringExpense> findActiveByUserId(@Param("userId") Long userId);

    @Query("SELECT r FROM RecurringExpense r WHERE r.user.id = :userId AND r.isActive = true AND r.isPaused = false ORDER BY r.nextDueDate ASC")
    List<RecurringExpense> findActiveNotPausedByUserId(@Param("userId") Long userId);

    @Query("SELECT r FROM RecurringExpense r WHERE r.isActive = true AND r.isPaused = false AND r.nextDueDate <= :date")
    List<RecurringExpense> findDueOrOverdue(@Param("date") LocalDate date);

    @Query("SELECT r FROM RecurringExpense r WHERE r.isActive = true AND r.isPaused = false " +
           "AND r.nextDueDate > :today AND r.nextDueDate <= :reminderDate")
    List<RecurringExpense> findNeedingReminder(@Param("today") LocalDate today, @Param("reminderDate") LocalDate reminderDate);

    @Query("SELECT r FROM RecurringExpense r WHERE r.user.id = :userId AND r.isActive = true " +
           "AND r.nextDueDate BETWEEN :startDate AND :endDate ORDER BY r.nextDueDate ASC")
    List<RecurringExpense> findUpcomingByUserId(@Param("userId") Long userId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(CASE " +
           "WHEN r.frequency = 'DAILY' THEN r.amount * 30.44 " +
           "WHEN r.frequency = 'WEEKLY' THEN r.amount * 4.33 " +
           "WHEN r.frequency = 'MONTHLY' THEN r.amount " +
           "WHEN r.frequency = 'YEARLY' THEN r.amount / 12 " +
           "ELSE r.amount END), 0) " +
           "FROM RecurringExpense r WHERE r.user.id = :userId AND r.isActive = true AND r.isPaused = false")
    BigDecimal calculateTotalMonthlyRecurring(@Param("userId") Long userId);

    long countByUserId(Long userId);

    @Query("SELECT COUNT(r) FROM RecurringExpense r WHERE r.user.id = :userId AND r.isActive = true AND r.isPaused = false")
    long countActiveByUserId(@Param("userId") Long userId);
}
