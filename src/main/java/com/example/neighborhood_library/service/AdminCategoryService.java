package com.example.neighborhood_library.service;

import com.example.neighborhood_library.domain.Category;
import com.example.neighborhood_library.repo.CategoryRepository;
import com.example.neighborhood_library.support.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;

    public AdminCategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public void create(String name) {
        Category c = new Category();
        c.setName(name.trim());
        categoryRepository.save(c);
    }

    @Transactional
    public void rename(long id, String newName) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Nie znaleziono kategorii id=" + id));
        c.setName(newName.trim());
        categoryRepository.save(c);
    }

    @Transactional
    public void delete(long id) {
        try {
            categoryRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            // ON DELETE RESTRICT gdy kategoria niepusta
            throw new IllegalStateException("Nie można usunąć kategorii, która ma przypisane publikacje.");
        }
    }
}
