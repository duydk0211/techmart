package com.techmart.core.service.command.catalog;

import com.techmart.core.dto.request.catalog.CategoryRequest;
import com.techmart.core.dto.response.catalog.CategoryResponse;

import java.util.UUID;

public interface CategoryCommandService {

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(UUID id, CategoryRequest request);

    void deleteCategory(UUID id);
}
