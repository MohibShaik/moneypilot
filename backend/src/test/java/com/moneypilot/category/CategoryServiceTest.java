package com.moneypilot.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moneypilot.category.dto.CreateCategoryRequest;
import com.moneypilot.category.dto.UpdateCategoryRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoryServiceTest {

    private CategoryRepository repository;
    private CategoryService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(CategoryRepository.class);
        service = new CategoryService(repository, new CategoryMapper());
    }

    @Test
    void createRejectsDuplicateNameViaFastPath() {
        when(repository.existsByUserIdAndName(userId, "Food")).thenReturn(true);

        assertThatThrownBy(() ->
                service.create(userId, new CreateCategoryRequest("Food", CategoryType.EXPENSE)))
                .isInstanceOf(DuplicateCategoryException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createSavesUserOwnedCategory() {
        when(repository.existsByUserIdAndName(userId, "Food")).thenReturn(false);
        when(repository.saveAndFlush(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(userId, new CreateCategoryRequest("Food", CategoryType.EXPENSE));

        assertThat(response.name()).isEqualTo("Food");
        assertThat(response.categoryType()).isEqualTo(CategoryType.EXPENSE);
        // A user-owned category (userId set) is not a system category.
        assertThat(response.system()).isFalse();
    }

    @Test
    void updateThrowsNotFoundWhenNotOwnedByUser() {
        when(repository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.update(userId, categoryId, new UpdateCategoryRequest("New", CategoryType.EXPENSE)))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void updateRejectsRenameToExistingName() {
        Category owned = new Category(userId, "Food", CategoryType.EXPENSE);
        when(repository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(owned));
        when(repository.existsByUserIdAndName(userId, "Shopping")).thenReturn(true);

        assertThatThrownBy(() ->
                service.update(userId, categoryId, new UpdateCategoryRequest("Shopping", CategoryType.EXPENSE)))
                .isInstanceOf(DuplicateCategoryException.class);
    }

    @Test
    void deleteThrowsNotFoundWhenNotOwnedByUser() {
        when(repository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(userId, categoryId))
                .isInstanceOf(CategoryNotFoundException.class);

        verify(repository, never()).delete(any());
    }

    @Test
    void deleteRemovesOwnedCategory() {
        Category owned = new Category(userId, "Food", CategoryType.EXPENSE);
        when(repository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(owned));

        service.delete(userId, categoryId);

        verify(repository).delete(eq(owned));
    }
}
