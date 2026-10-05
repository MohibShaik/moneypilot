package com.moneypilot.expense;

import com.moneypilot.category.CategoryNotFoundException;
import com.moneypilot.category.CategoryRepository;
import com.moneypilot.expense.dto.CreateExpenseRequest;
import com.moneypilot.expense.dto.ExpenseResponse;
import com.moneypilot.expense.dto.UpdateExpenseRequest;
import com.moneypilot.paymentmethod.PaymentMethodNotFoundException;
import com.moneypilot.paymentmethod.PaymentMethodRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for expenses.
 *
 * <p>Identity always comes from the authenticated user id passed by the
 * controller (the JWT subject). Ownership is enforced on every operation: a
 * user may only read, update, or delete their own expenses; a non-owned one is
 * reported as not found.
 *
 * <p>Optional references are validated before persisting so a user cannot
 * attach another user's resource: a {@code categoryId} must reference a
 * category visible to the user (system or owned), and a {@code paymentMethodId}
 * must reference a payment method owned by the user.
 */
@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final ExpenseMapper mapper;

    public ExpenseService(ExpenseRepository expenseRepository,
                          CategoryRepository categoryRepository,
                          PaymentMethodRepository paymentMethodRepository,
                          ExpenseMapper mapper) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(UUID userId, LocalDate from, LocalDate to, UUID categoryId) {
        return expenseRepository.findForUserFiltered(userId, from, to, categoryId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(UUID userId, UUID id) {
        return mapper.toResponse(requireOwned(userId, id));
    }

    @Transactional
    public ExpenseResponse create(UUID userId, CreateExpenseRequest request) {
        validateReferences(userId, request.categoryId(), request.paymentMethodId());
        Expense expense = new Expense(
                userId,
                request.categoryId(),
                request.paymentMethodId(),
                request.amount(),
                request.expenseDate(),
                request.description(),
                request.notes());
        return mapper.toResponse(expenseRepository.saveAndFlush(expense));
    }

    @Transactional
    public ExpenseResponse update(UUID userId, UUID id, UpdateExpenseRequest request) {
        validateReferences(userId, request.categoryId(), request.paymentMethodId());
        Expense expense = requireOwned(userId, id);
        expense.setAmount(request.amount());
        expense.setExpenseDate(request.expenseDate());
        expense.setDescription(request.description());
        expense.setNotes(request.notes());
        expense.setCategoryId(request.categoryId());
        expense.setPaymentMethodId(request.paymentMethodId());
        return mapper.toResponse(expenseRepository.saveAndFlush(expense));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Expense expense = requireOwned(userId, id);
        expenseRepository.delete(expense);
    }

    private Expense requireOwned(UUID userId, UUID id) {
        return expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(ExpenseNotFoundException::new);
    }

    /**
     * Validates optional references belong to the user. A category must be
     * visible (system or owned); a payment method must be owned. Null
     * references are allowed and skipped.
     */
    private void validateReferences(UUID userId, UUID categoryId, UUID paymentMethodId) {
        if (categoryId != null) {
            categoryRepository.findVisibleByIdForUser(categoryId, userId)
                    .orElseThrow(CategoryNotFoundException::new);
        }
        if (paymentMethodId != null) {
            paymentMethodRepository.findByIdAndUserId(paymentMethodId, userId)
                    .orElseThrow(PaymentMethodNotFoundException::new);
        }
    }
}
