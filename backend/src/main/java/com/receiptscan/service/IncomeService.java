package com.receiptscan.service;

import com.receiptscan.dto.IncomeSourceRequest;
import com.receiptscan.dto.IncomeSourceResponse;
import com.receiptscan.entity.IncomeSource;
import com.receiptscan.entity.User;
import com.receiptscan.exception.BadRequestException;
import com.receiptscan.exception.NotFoundException;
import com.receiptscan.repository.IncomeSourceRepository;
import com.receiptscan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class IncomeService {

    private final IncomeSourceRepository incomeSourceRepository;
    private final UserRepository userRepository;

    @Transactional
    public IncomeSourceResponse createIncomeSource(Long userId, IncomeSourceRequest request) {
        log.info("Creating income entry for user {}: {}", userId, request.getSourceName());

        User user = userRepository.findById(Objects.requireNonNull(userId))
            .orElseThrow(() -> new NotFoundException("User not found"));

        IncomeSource incomeSource = IncomeSource.builder()
            .user(user)
            .sourceName(request.getSourceName())
            .amount(request.getAmount())
            .transactionDate(request.getTransactionDate())
            .paymentMethod(request.getPaymentMethod())
            .notes(request.getNotes())
            .build();

        IncomeSource saved = incomeSourceRepository.save(incomeSource);
        log.info("Income entry created with ID: {}", saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<IncomeSourceResponse> getAllIncomeSources(Long userId) {
        log.info("Fetching all income entries for user {}", userId);
        return incomeSourceRepository.findByUserIdOrderByTransactionDateDesc(userId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalMonthlyIncome(Long userId) {
        log.info("Calculating total income for current month for user {}", userId);
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        return incomeSourceRepository.sumAmountByUserIdAndDateRange(userId, startOfMonth, endOfMonth);
    }

    @Transactional(readOnly = true)
    public IncomeSourceResponse getIncomeSourceById(Long userId, Long incomeId) {
        log.info("Fetching income entry {} for user {}", incomeId, userId);
        IncomeSource incomeSource = incomeSourceRepository.findById(Objects.requireNonNull(incomeId))
            .orElseThrow(() -> new NotFoundException("Income entry not found"));

        if (!incomeSource.getUser().getId().equals(userId)) {
            throw new BadRequestException("Income entry does not belong to user");
        }

        return toResponse(incomeSource);
    }

    @Transactional
    public IncomeSourceResponse updateIncomeSource(Long userId, Long incomeId, IncomeSourceRequest request) {
        log.info("Updating income entry {} for user {}", incomeId, userId);

        IncomeSource incomeSource = incomeSourceRepository.findById(Objects.requireNonNull(incomeId))
            .orElseThrow(() -> new NotFoundException("Income entry not found"));

        if (!incomeSource.getUser().getId().equals(userId)) {
            throw new BadRequestException("Income entry does not belong to user");
        }

        incomeSource.setSourceName(request.getSourceName());
        incomeSource.setAmount(request.getAmount());
        incomeSource.setTransactionDate(request.getTransactionDate());
        incomeSource.setPaymentMethod(request.getPaymentMethod());
        incomeSource.setNotes(request.getNotes());

        IncomeSource saved = incomeSourceRepository.save(incomeSource);
        log.info("Income entry {} updated successfully", incomeId);
        return toResponse(saved);
    }

    @Transactional
    public void deleteIncomeSource(Long userId, Long incomeId) {
        log.info("Deleting income entry {} for user {}", incomeId, userId);

        IncomeSource incomeSource = incomeSourceRepository.findById(Objects.requireNonNull(incomeId))
            .orElseThrow(() -> new NotFoundException("Income entry not found"));

        if (!incomeSource.getUser().getId().equals(userId)) {
            throw new BadRequestException("Income entry does not belong to user");
        }

        incomeSourceRepository.delete(incomeSource);
        log.info("Income entry {} deleted successfully", incomeId);
    }

    private IncomeSourceResponse toResponse(IncomeSource incomeSource) {
        return IncomeSourceResponse.builder()
            .id(incomeSource.getId())
            .sourceName(incomeSource.getSourceName())
            .amount(incomeSource.getAmount())
            .transactionDate(incomeSource.getTransactionDate())
            .paymentMethod(incomeSource.getPaymentMethod())
            .notes(incomeSource.getNotes())
            .createdAt(incomeSource.getCreatedAt())
            .build();
    }
}
