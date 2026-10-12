package com.techmart.core.bus.command.catalog;

import com.techmart.core.bus.Command;
import com.techmart.core.dto.request.catalog.CategoryRequest;
import com.techmart.core.dto.response.catalog.CategoryResponse;

public record CreateCategoryCommand(
        CategoryRequest request
) implements Command<CategoryResponse> {
}
