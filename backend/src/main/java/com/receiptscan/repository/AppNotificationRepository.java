package com.receiptscan.repository;

import com.receiptscan.entity.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {

    @Query("SELECT n FROM AppNotification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<AppNotification> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT n FROM AppNotification n WHERE n.user.id = :userId AND n.isRead = false ORDER BY n.createdAt DESC")
    List<AppNotification> findUnreadByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(n) FROM AppNotification n WHERE n.user.id = :userId AND n.isRead = false")
    long countUnreadByUserId(@Param("userId") Long userId);

    @Query("SELECT n FROM AppNotification n WHERE n.user.id = :userId AND n.type = :type ORDER BY n.createdAt DESC")
    List<AppNotification> findByUserIdAndType(@Param("userId") Long userId,
                                               @Param("type") AppNotification.NotificationType type);

    @Modifying
    @Query("UPDATE AppNotification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM AppNotification n WHERE n.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
