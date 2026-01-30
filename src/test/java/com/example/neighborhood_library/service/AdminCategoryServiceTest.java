package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.Category;
import com.example.neighborhood_library.repo.CategoryRepository;
import com.example.neighborhood_library.support.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private AdminCategoryService adminCategoryService;

    @Test
    void create_ShouldTrimNameAndSaveCategory() {
        // given
        String dirtyName = "  Nowa Kategoria  ";

        // when
        adminCategoryService.create(dirtyName);

        // then
        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());

        Category savedCategory = categoryCaptor.getValue();
        assertEquals("Nowa Kategoria", savedCategory.getName());
    }

    @Test
    void rename_ShouldUpdateNameAndSave_WhenCategoryExists() {
        // given
        long categoryId = 1L;
        String newName = "  Zmieniona Nazwa  ";
        Category existingCategory = new Category();

        // Poprawka: Ustawiamy ID za pomocą refleksji
        ReflectionTestUtils.setField(existingCategory, "id", categoryId);
        existingCategory.setName("Stara Nazwa");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));

        // when
        adminCategoryService.rename(categoryId, newName);

        // then
        verify(categoryRepository).save(existingCategory);
        assertEquals("Zmieniona Nazwa", existingCategory.getName());
    }

    @Test
    void rename_ShouldThrowNotFoundException_WhenCategoryDoesNotExist() {
        // given
        long categoryId = 99L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(NotFoundException.class, () ->
                adminCategoryService.rename(categoryId, "Nowa nazwa")
        );
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void delete_ShouldCallDeleteById_WhenNoConstraintViolation() {
        // given
        long categoryId = 1L;

        // when
        adminCategoryService.delete(categoryId);

        // then
        verify(categoryRepository).deleteById(categoryId);
    }

    @Test
    void delete_ShouldThrowIllegalStateException_WhenCategoryIsUsed() {
        // given
        long categoryId = 1L;
        doThrow(new DataIntegrityViolationException("Constraint violation"))
                .when(categoryRepository).deleteById(categoryId);

        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                adminCategoryService.delete(categoryId)
        );
        assertEquals("Nie można usunąć kategorii, która ma przypisane publikacje.", exception.getMessage());
    }
}