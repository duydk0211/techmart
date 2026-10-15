package com.techmart.core.service.command.catalog.impl;

import com.techmart.core.dto.request.catalog.CategoryRequest;
import com.techmart.core.dto.response.catalog.CategoryResponse;
import com.techmart.core.entity.catalog.CategoryEntity;
import com.techmart.core.repository.catalog.CategoryRepository;
import com.techmart.core.service.command.catalog.CategoryCommandService;
import com.techmart.core.util.SlugUtils;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.exception.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CategoryCommandServiceImpl implements CategoryCommandService {

    private static final Logger log = LoggerFactory.getLogger(CategoryCommandServiceImpl.class);

    private final CategoryRepository categoryRepository;

    public CategoryCommandServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("[CategoryCommandServiceImpl][createCategory] Creating category: {}", request.name());
        String slug = SlugUtils.toSlug(request.name());

        CategoryEntity entity = new CategoryEntity();
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setActive(true);

        if (request.parentId() == null) {
            categoryRepository.findByParentIsNullAndNameIgnoreCase(request.name())
                    .ifPresent(existing -> {
                        throw new CoreException(ResponseCode.DUPLICATE_RESOURCE, "Root category already exists: " + request.name());
                    });
            entity.setLevel(1);
            entity.setPath("/" + slug);
            entity.setParent(null);
        } else {
            CategoryEntity parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Parent category not found"));

            if (parent.getLevel() + 1 > 4) {
                throw new CoreException(ResponseCode.CATEGORY_MAX_LEVEL_EXCEEDED, "Category tree depth limit of 4 levels exceeded");
            }

            categoryRepository.findByParentIdAndNameIgnoreCase(parent.getId(), request.name())
                    .ifPresent(existing -> {
                        throw new CoreException(ResponseCode.DUPLICATE_RESOURCE, "Category with same name already exists under parent");
                    });

            entity.setParent(parent);
            entity.setLevel(parent.getLevel() + 1);
            entity.setPath(parent.getPath() + "/" + slug);
        }

        CategoryEntity saved = categoryRepository.save(entity);
        log.info("[CategoryCommandServiceImpl][createCategory] Created category id: {}, path: {}", saved.getId(), saved.getPath());
        return CategoryResponse.fromEntity(saved);
    }

    @Override
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        log.info("[CategoryCommandServiceImpl][updateCategory] Updating category id: {}", id);
        CategoryEntity current = categoryRepository.findById(id)
                .orElseThrow(() -> new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Category not found"));

        current.setName(request.name());
        current.setDescription(request.description());

        UUID oldParentId = current.getParent() != null ? current.getParent().getId() : null;
        UUID newParentId = request.parentId();

        boolean parentChanged = (newParentId == null && oldParentId != null) ||
                (newParentId != null && !newParentId.equals(oldParentId));

        if (parentChanged) {
            String oldPrefix = current.getPath();
            String newSlug = SlugUtils.toSlug(request.name());

            if (newParentId != null) {
                if (newParentId.equals(current.getId())) {
                    throw new CoreException(ResponseCode.CATEGORY_CYCLE_DETECTED, "Category cannot be its own parent");
                }

                CategoryEntity newParent = categoryRepository.findById(newParentId)
                        .orElseThrow(() -> new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "New parent category not found"));

                if (newParent.getPath().startsWith(current.getPath() + "/")) {
                    throw new CoreException(ResponseCode.CATEGORY_CYCLE_DETECTED, "Cannot move category into its own descendant");
                }

                int maxDescendantLevel = categoryRepository.findMaxDescendantLevel(current.getPath());
                int subtreeDepth = (maxDescendantLevel > 0) ? (maxDescendantLevel - current.getLevel()) : 0;
                int newLevel = newParent.getLevel() + 1;

                if (newLevel + subtreeDepth > 4) {
                    throw new CoreException(ResponseCode.CATEGORY_MAX_LEVEL_EXCEEDED, "Moving category would exceed maximum depth of 4 levels");
                }

                String newPrefix = newParent.getPath() + "/" + newSlug;
                int levelDelta = newLevel - current.getLevel();

                current.setParent(newParent);
                current.setLevel(newLevel);
                current.setPath(newPrefix);

                List<CategoryEntity> descendants = categoryRepository.findDescendants(oldPrefix);
                for (CategoryEntity desc : descendants) {
                    desc.setPath(newPrefix + desc.getPath().substring(oldPrefix.length()));
                    desc.setLevel(desc.getLevel() + levelDelta);
                }
            } else {
                int maxDescendantLevel = categoryRepository.findMaxDescendantLevel(current.getPath());
                int subtreeDepth = (maxDescendantLevel > 0) ? (maxDescendantLevel - current.getLevel()) : 0;
                int newLevel = 1;

                if (newLevel + subtreeDepth > 4) {
                    throw new CoreException(ResponseCode.CATEGORY_MAX_LEVEL_EXCEEDED, "Moving category would exceed maximum depth of 4 levels");
                }

                String newPrefix = "/" + newSlug;
                int levelDelta = 1 - current.getLevel();

                current.setParent(null);
                current.setLevel(1);
                current.setPath(newPrefix);

                List<CategoryEntity> descendants = categoryRepository.findDescendants(oldPrefix);
                for (CategoryEntity desc : descendants) {
                    desc.setPath(newPrefix + desc.getPath().substring(oldPrefix.length()));
                    desc.setLevel(desc.getLevel() + levelDelta);
                }
            }
        }

        CategoryEntity updated = categoryRepository.save(current);
        return CategoryResponse.fromEntity(updated);
    }

    @Override
    public void deleteCategory(UUID id) {
        log.info("[CategoryCommandServiceImpl][deleteCategory] Deactivating category id: {}", id);
        CategoryEntity current = categoryRepository.findById(id)
                .orElseThrow(() -> new CoreException(ResponseCode.RESOURCE_NOT_FOUND, "Category not found"));

        current.setActive(false);
        categoryRepository.save(current);
        categoryRepository.deactivateSubtree(current.getPath(), Instant.now());
    }
}
