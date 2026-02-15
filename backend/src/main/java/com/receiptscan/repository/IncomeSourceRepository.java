package com.receiptscan.repository;

import com.receiptscan.entity.IncomeSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface IncomeSourceRepository extends JpaRepository<IncomeSource, Long> {

    List<IncomeSource> findByUserIdOrderByTransactionDateDesc(Long userId);

    @Query("SELECT i FROM IncomeSource i WHERE i.user.id = :userId " +
           "AND i.transactionDate >= :startDate AND i.transactionDate <= :endDate " +
           "ORDER BY i.transactionDate DESC")
    List<IncomeSource> findByUserIdAndDateRange(@Param("userId") Long userId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM IncomeSource i " +
           "WHERE i.user.id = :userId " +
           "AND i.transactionDate >= :startDate AND i.transactionDate <= :endDate")
    BigDecimal sumAmountByUserIdAndDateRange(@Param("userId") Long userId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    long countByUserId(Long userId);
}
