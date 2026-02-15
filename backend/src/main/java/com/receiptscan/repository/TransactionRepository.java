package com.receiptscan.repository;

import com.receiptscan.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserId(Long userId);

    List<Transaction> findByUserIdAndTransactionDateBetween(
        Long userId,
        LocalDate startDate,
        LocalDate endDate
    );

    List<Transaction> findByUserIdAndCategory(Long userId, String category);

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserIdOrderByDateDesc(@Param("userId") Long userId);

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.transactionDate >= :startDate ORDER BY t.transactionDate DESC")
    List<Transaction> findRecentTransactions(@Param("userId") Long userId, @Param("startDate") LocalDate startDate);

    long countByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.userId = :userId AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate")
    BigDecimal sumAmountByUserIdAndDateRange(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.isRecurring = true ORDER BY t.transactionDate DESC")
    List<Transaction> findRecurringByUserId(@Param("userId") Long userId);

    List<Transaction> findByRecurringExpenseId(Long recurringExpenseId);
}
