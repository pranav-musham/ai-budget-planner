package com.receiptscan.repository;

import com.receiptscan.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Find all chat messages for a specific user, ordered by most recent first
     */
    List<ChatMessage> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find recent chat messages for a user with limit
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.userId = :userId ORDER BY cm.createdAt DESC")
    List<ChatMessage> findRecentMessages(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);

    /**
     * Delete all chat messages for a specific user
     */
    void deleteByUserId(Long userId);

    /**
     * Count total messages for a user
     */
    long countByUserId(Long userId);
}
