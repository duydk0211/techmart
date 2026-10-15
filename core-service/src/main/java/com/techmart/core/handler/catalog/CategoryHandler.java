package com.techmart.core.handler.catalog;

import com.techmart.core.bus.CommandHandler;
import com.techmart.core.bus.QueryHandler;
import com.techmart.core.bus.command.catalog.CreateCategoryCommand;
import com.techmart.core.bus.command.catalog.DeleteCategoryCommand;
import com.techmart.core.bus.command.catalog.UpdateCategoryCommand;
import com.techmart.core.bus.query.catalog.GetCategoryTreeQuery;
import com.techmart.core.dto.response.catalog.CategoryResponse;
import com.techmart.core.dto.response.catalog.CategoryTreeResponse;
import com.techmart.core.service.command.catalog.CategoryCommandService;
import com.techmart.core.service.query.catalog.CategoryQueryService;
import org.springframework.stereotype.Component;

import java.util.List;

public class CategoryHandler {

    @Component
    public static class CreateCategoryCommandHandler implements CommandHandler<CreateCategoryCommand, CategoryResponse> {
        private final CategoryCommandService categoryCommandService;

        public CreateCategoryCommandHandler(CategoryCommandService categoryCommandService) {
            this.categoryCommandService = categoryCommandService;
        }

        @Override
        public CategoryResponse handle(CreateCategoryCommand command) {
            return categoryCommandService.createCategory(command.request());
        }
    }

    @Component
    public static class UpdateCategoryCommandHandler implements CommandHandler<UpdateCategoryCommand, CategoryResponse> {
        private final CategoryCommandService categoryCommandService;

        public UpdateCategoryCommandHandler(CategoryCommandService categoryCommandService) {
            this.categoryCommandService = categoryCommandService;
        }

        @Override
        public CategoryResponse handle(UpdateCategoryCommand command) {
            return categoryCommandService.updateCategory(command.id(), command.request());
        }
    }

    @Component
    public static class DeleteCategoryCommandHandler implements CommandHandler<DeleteCategoryCommand, Void> {
        private final CategoryCommandService categoryCommandService;

        public DeleteCategoryCommandHandler(CategoryCommandService categoryCommandService) {
            this.categoryCommandService = categoryCommandService;
        }

        @Override
        public Void handle(DeleteCategoryCommand command) {
            categoryCommandService.deleteCategory(command.id());
            return null;
        }
    }

    @Component
    public static class GetCategoryTreeQueryHandler implements QueryHandler<GetCategoryTreeQuery, List<CategoryTreeResponse>> {
        private final CategoryQueryService categoryQueryService;

        public GetCategoryTreeQueryHandler(CategoryQueryService categoryQueryService) {
            this.categoryQueryService = categoryQueryService;
        }

        @Override
        public List<CategoryTreeResponse> handle(GetCategoryTreeQuery query) {
            return categoryQueryService.getCategoryTree();
        }
    }
}
