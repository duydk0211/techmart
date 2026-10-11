package com.techmart.core.controller.catalog.impl;

import com.techmart.core.bus.SpringBus;
import com.techmart.core.bus.command.catalog.CreateCategoryCommand;
import com.techmart.core.bus.command.catalog.DeleteCategoryCommand;
import com.techmart.core.bus.command.catalog.UpdateCategoryCommand;
import com.techmart.core.bus.query.catalog.GetCategoryTreeQuery;
import com.techmart.core.controller.catalog.InternalCategoryApiController;
import com.techmart.core.dto.request.catalog.CategoryRequest;
import com.techmart.core.dto.response.catalog.CategoryResponse;
import com.techmart.core.dto.response.catalog.CategoryTreeResponse;
import com.techmart.shared.security.TechmartPreauthorize;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/categories")
public class InternalCategoryApiControllerImpl implements InternalCategoryApiController {

    private static final Logger log = LoggerFactory.getLogger(InternalCategoryApiControllerImpl.class);

    private final SpringBus springBus;

    public InternalCategoryApiControllerImpl(SpringBus springBus) {
        this.springBus = springBus;
    }

    @Override
    @GetMapping("/tree")
    @TechmartPreauthorize(gateway = "TECHMART_GW", authenticated = false)
    public ResponseEntity<List<CategoryTreeResponse>> getCategoryTree() {
        log.info("[InternalCategoryApiControllerImpl][getCategoryTree] Fetching nested category tree");
        List<CategoryTreeResponse> tree = springBus.query(new GetCategoryTreeQuery());
        return ResponseEntity.ok(tree);
    }

    @Override
    @PostMapping
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"ADMIN"})
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        log.info("[InternalCategoryApiControllerImpl][createCategory] Creating category: {}", request.name());
        CategoryResponse response = springBus.execute(new CreateCategoryCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PutMapping("/{id}")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"ADMIN"})
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable("id") UUID id, @Valid @RequestBody CategoryRequest request) {
        log.info("[InternalCategoryApiControllerImpl][updateCategory] Updating category id: {}", id);
        CategoryResponse response = springBus.execute(new UpdateCategoryCommand(id, request));
        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/{id}")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"ADMIN"})
    public ResponseEntity<Void> deleteCategory(@PathVariable("id") UUID id) {
        log.info("[InternalCategoryApiControllerImpl][deleteCategory] Deleting category id: {}", id);
        springBus.execute(new DeleteCategoryCommand(id));
        return ResponseEntity.noContent().build();
    }
}
