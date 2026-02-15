package com.receiptscan.controller;

import com.receiptscan.dto.CategoryResponse;
import com.receiptscan.entity.User;
import com.receiptscan.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Get all categories (predefined + custom)
     * GET /api/categories/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<CategoryResponse>> getAllCategories(
        @AuthenticationPrincipal User user
    ) {
        log.info("Fetching all categories for user {}", user.getId());
        return ResponseEntity.ok(categoryService.getAllCategories(user.getId()));
    }

    /**
     * Get predefined categories only
     * GET /api/categories/predefined
     */
    @GetMapping("/predefined")
    public ResponseEntity<List<CategoryResponse>> getPredefinedCategories() {
        log.info("Fetching predefined categories");
        return ResponseEntity.ok(categoryService.getPredefinedCategories());
    }

    /**
     * Get category names (for dropdowns)
     * GET /api/categories/names
     */
    @GetMapping("/names")
    public ResponseEntity<List<String>> getCategoryNames(
        @AuthenticationPrincipal User user
    ) {
        log.info("Fetching category names for user {}", user.getId());
        return ResponseEntity.ok(categoryService.getCategoryNames(user.getId()));
    }
}
