package com.receiptscan.service;

import com.receiptscan.dto.CategoryResponse;
import com.receiptscan.entity.CustomCategory;
import com.receiptscan.entity.PredefinedCategory;
import com.receiptscan.repository.CustomCategoryRepository;
import com.receiptscan.repository.PredefinedCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final PredefinedCategoryRepository predefinedCategoryRepository;
    private final CustomCategoryRepository customCategoryRepository;

    /**
     * Get all categories (predefined + custom) for a user
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(Long userId) {
        log.info("Fetching all categories for user {}", userId);
        List<CategoryResponse> categories = new ArrayList<>();

        // Add predefined categories first
        List<PredefinedCategory> predefined = predefinedCategoryRepository.findAllActiveOrdered();
        categories.addAll(predefined.stream()
            .map(p -> CategoryResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .icon(p.getIcon())
                .color(p.getColor())
                .displayOrder(p.getDisplayOrder())
                .type("PREDEFINED")
                .build())
            .collect(Collectors.toList()));

        // Add custom categories
        List<CustomCategory> custom = customCategoryRepository.findByUserId(userId);
        categories.addAll(custom.stream()
            .map(c -> CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .icon(c.getIcon())
                .color(c.getColor())
                .displayOrder(100 + c.getId().intValue()) // Custom after predefined
                .type("CUSTOM")
                .build())
            .collect(Collectors.toList()));

        log.info("Found {} total categories ({} predefined, {} custom)",
            categories.size(), predefined.size(), custom.size());
        return categories;
    }

    /**
     * Get only predefined categories
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getPredefinedCategories() {
        log.info("Fetching predefined categories");
        return predefinedCategoryRepository.findAllActiveOrdered().stream()
            .map(p -> CategoryResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .icon(p.getIcon())
                .color(p.getColor())
                .displayOrder(p.getDisplayOrder())
                .type("PREDEFINED")
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Get category names only (for dropdowns)
     */
    @Transactional(readOnly = true)
    public List<String> getCategoryNames(Long userId) {
        return getAllCategories(userId).stream()
            .map(CategoryResponse::getName)
            .collect(Collectors.toList());
    }
}
