package com.techmart.core.bus.query.catalog;

import com.techmart.core.bus.Query;
import com.techmart.core.dto.response.catalog.CategoryTreeResponse;

import java.util.List;

public record GetCategoryTreeQuery() implements Query<List<CategoryTreeResponse>> {
}
