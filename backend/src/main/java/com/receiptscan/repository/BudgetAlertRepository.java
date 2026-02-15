package com.receiptscan.repository;

import com.receiptscan.entity.BudgetAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BudgetAlertRepository extends JpaRepository<BudgetAlert, Long> {

    List<BudgetAlert> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<BudgetAlert> findByUserIdAndIsReadOrderByCreatedAtDesc(Long userId, Boolean isRead);

    List<BudgetAlert> findByUserIdAndAlertTypeOrderByCreatedAtDesc(
        Long userId,
        BudgetAlert.AlertType alertType
    );

    List<BudgetAlert> findByUserIdAndCategoryOrderByCreatedAtDesc(Long userId, String category);

    @Query("SELECT a FROM BudgetAlert a WHERE a.user.id = :userId AND a.createdAt >= :startDate ORDER BY a.createdAt DESC")
    List<BudgetAlert> findRecentAlerts(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT a FROM BudgetAlert a WHERE a.user.id = :userId AND a.isRead = false ORDER BY a.createdAt DESC")
    List<BudgetAlert> findUnreadAlerts(@Param("userId") Long userId);

    @Query("SELECT COUNT(a) FROM BudgetAlert a WHERE a.user.id = :userId AND a.isRead = false")
    long countUnreadByUserId(@Param("userId") Long userId);

    long countByUserId(Long userId);
}
