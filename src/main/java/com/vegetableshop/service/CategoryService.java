package com.vegetableshop.service;

import com.vegetableshop.entity.Category;
import com.vegetableshop.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Profile("mysql")
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> findAllActiveCategories() {
        return categoryRepository.findByStatusTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Category findById(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục có id: " + id));
    }
}
