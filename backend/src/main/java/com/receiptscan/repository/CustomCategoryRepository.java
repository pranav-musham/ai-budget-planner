package com.receiptscan.repository;

import com.receiptscan.entity.CustomCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomCategoryRepository extends JpaRepository<CustomCategory, Long> {

    List<CustomCategory> findByUserId(Long userId);

    Optional<CustomCategory> findByUserIdAndName(Long userId, String name);

    boolean existsByUserIdAndName(Long userId, String name);

    long countByUserId(Long userId);
}
