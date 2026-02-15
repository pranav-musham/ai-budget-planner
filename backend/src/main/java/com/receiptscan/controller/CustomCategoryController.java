package com.receiptscan.controller;

import com.receiptscan.dto.CustomCategoryRequest;
import com.receiptscan.dto.CustomCategoryResponse;
import com.receiptscan.entity.User;
import com.receiptscan.service.CustomCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories/custom")
@RequiredArgsConstructor
@Slf4j
public class CustomCategoryController {

    private final CustomCategoryService customCategoryService;

    @PostMapping
    public ResponseEntity<CustomCategoryResponse> createCategory(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CustomCategoryRequest request
    ) {
        try {
            CustomCategoryResponse response = customCategoryService.createCategory(user.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Error creating custom category", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<CustomCategoryResponse>> getAllCategories(
        @AuthenticationPrincipal User user
    ) {
        try {
            List<CustomCategoryResponse> categories = customCategoryService.getAllCategories(user.getId());
            return ResponseEntity.ok(categories);
        } catch (RuntimeException e) {
            log.error("Error fetching custom categories", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomCategoryResponse> getCategory(
        @AuthenticationPrincipal User user,
        @PathVariable Long id
    ) {
        try {
            CustomCategoryResponse response = customCategoryService.getCategoryById(user.getId(), id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error fetching custom category", e);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomCategoryResponse> updateCategory(
        @AuthenticationPrincipal User user,
        @PathVariable Long id,
        @Valid @RequestBody CustomCategoryRequest request
    ) {
        try {
            CustomCategoryResponse response = customCategoryService.updateCategory(user.getId(), id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error updating custom category", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
        @AuthenticationPrincipal User user,
        @PathVariable Long id
    ) {
        try {
            customCategoryService.deleteCategory(user.getId(), id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error deleting custom category", e);
            return ResponseEntity.notFound().build();
        }
    }
}
