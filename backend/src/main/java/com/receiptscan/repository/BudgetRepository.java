package com.receiptscan.repository;

import com.receiptscan.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserId(Long userId);

    List<Budget> findByUserIdAndIsActive(Long userId, Boolean isActive);

    Optional<Budget> findByUserIdAndCategoryAndPeriodType(
        Long userId,
        String category,
        Budget.PeriodType periodType
    );

    List<Budget> findByUserIdAndCategory(Long userId, String category);

    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.isActive = true")
    List<Budget> findActiveByUserId(@Param("userId") Long userId);

    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.category = :category AND b.isActive = true")
    Optional<Budget> findActiveBudgetByCategory(@Param("userId") Long userId, @Param("category") String category);

    boolean existsByUserIdAndCategoryAndPeriodType(
        Long userId,
        String category,
        Budget.PeriodType periodType
    );

    long countByUserId(Long userId);
}
