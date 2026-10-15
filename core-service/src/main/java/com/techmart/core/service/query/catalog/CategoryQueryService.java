package com.techmart.core.service.query.catalog;

import com.techmart.core.dto.response.catalog.CategoryTreeResponse;

import java.util.List;

public interface CategoryQueryService {

    List<CategoryTreeResponse> getCategoryTree();
}
