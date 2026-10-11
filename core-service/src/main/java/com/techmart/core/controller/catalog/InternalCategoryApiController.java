package com.techmart.core.controller.catalog;

import com.techmart.core.dto.request.catalog.CategoryRequest;
import com.techmart.core.dto.response.catalog.CategoryResponse;
import com.techmart.core.dto.response.catalog.CategoryTreeResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public interface InternalCategoryApiController {

    ResponseEntity<List<CategoryTreeResponse>> getCategoryTree();

    ResponseEntity<CategoryResponse> createCategory(CategoryRequest request);

    ResponseEntity<CategoryResponse> updateCategory(UUID id, CategoryRequest request);

    ResponseEntity<Void> deleteCategory(UUID id);
}
