package com.receiptscan.service;

import com.receiptscan.dto.CustomCategoryRequest;
import com.receiptscan.dto.CustomCategoryResponse;
import com.receiptscan.entity.CustomCategory;
import com.receiptscan.entity.User;
import com.receiptscan.exception.BadRequestException;
import com.receiptscan.exception.NotFoundException;
import com.receiptscan.repository.CustomCategoryRepository;
import com.receiptscan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomCategoryService {

    private final CustomCategoryRepository customCategoryRepository;
    private final UserRepository userRepository;

    @Transactional
    @SuppressWarnings("null")
    public CustomCategoryResponse createCategory(Long userId, CustomCategoryRequest request) {
        log.info("Creating custom category '{}' for user {}", request.getName(), userId);

        User user = userRepository.findById(Objects.requireNonNull(userId))
            .orElseThrow(() -> new NotFoundException("User not found"));

        // Check if category name already exists for this user
        if (customCategoryRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new BadRequestException("Category '" + request.getName() + "' already exists");
        }

        CustomCategory category = CustomCategory.builder()
            .user(user)
            .name(request.getName())
            .icon(request.getIcon())
            .color(request.getColor())
            .build();

        CustomCategory savedCategory = customCategoryRepository.save(category);
        log.info("Custom category created with ID: {}", savedCategory.getId());
        return toCategoryResponse(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<CustomCategoryResponse> getAllCategories(Long userId) {
        log.info("Fetching all custom categories for user {}", userId);
        List<CustomCategory> categories = customCategoryRepository.findByUserId(userId);
        return categories.stream()
            .map(this::toCategoryResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomCategoryResponse getCategoryById(Long userId, Long categoryId) {
        log.info("Fetching custom category {} for user {}", categoryId, userId);
        CustomCategory category = customCategoryRepository.findById(Objects.requireNonNull(categoryId))
            .orElseThrow(() -> new NotFoundException("Category not found"));

        if (!category.getUser().getId().equals(userId)) {
            throw new BadRequestException("Category does not belong to user");
        }

        return toCategoryResponse(category);
    }

    @Transactional
    public CustomCategoryResponse updateCategory(Long userId, Long categoryId, CustomCategoryRequest request) {
        log.info("Updating custom category {} for user {}", categoryId, userId);

        CustomCategory category = customCategoryRepository.findById(Objects.requireNonNull(categoryId))
            .orElseThrow(() -> new NotFoundException("Category not found"));

        if (!category.getUser().getId().equals(userId)) {
            throw new BadRequestException("Category does not belong to user");
        }

        // Check if new name conflicts with existing category
        if (!category.getName().equals(request.getName()) &&
            customCategoryRepository.existsByUserIdAndName(userId, request.getName())) {
            throw new BadRequestException("Category '" + request.getName() + "' already exists");
        }

        category.setName(request.getName());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());

        category = customCategoryRepository.save(category);
        log.info("Custom category {} updated successfully", categoryId);

        return toCategoryResponse(category);
    }

    @Transactional
    public void deleteCategory(Long userId, Long categoryId) {
        log.info("Deleting custom category {} for user {}", categoryId, userId);

        CustomCategory category = customCategoryRepository.findById(Objects.requireNonNull(categoryId))
            .orElseThrow(() -> new NotFoundException("Category not found"));

        if (!category.getUser().getId().equals(userId)) {
            throw new BadRequestException("Category does not belong to user");
        }

        customCategoryRepository.delete(category);
        log.info("Custom category {} deleted successfully", categoryId);
    }

    private CustomCategoryResponse toCategoryResponse(CustomCategory category) {
        return CustomCategoryResponse.builder()
            .id(category.getId())
            .name(category.getName())
            .icon(category.getIcon())
            .color(category.getColor())
            .createdAt(category.getCreatedAt())
            .updatedAt(category.getUpdatedAt())
            .build();
    }
}
