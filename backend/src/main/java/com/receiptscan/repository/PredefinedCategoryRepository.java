package com.receiptscan.repository;

import com.receiptscan.entity.PredefinedCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PredefinedCategoryRepository extends JpaRepository<PredefinedCategory, Long> {

    @Query("SELECT p FROM PredefinedCategory p WHERE p.isActive = true ORDER BY p.displayOrder ASC")
    List<PredefinedCategory> findAllActiveOrdered();

    Optional<PredefinedCategory> findByName(String name);

    boolean existsByName(String name);
}
