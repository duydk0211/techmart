package com.techmart.core.service.query.catalog.impl;

import com.techmart.core.dto.response.catalog.CategoryTreeResponse;
import com.techmart.core.entity.catalog.CategoryEntity;
import com.techmart.core.repository.catalog.CategoryRepository;
import com.techmart.core.service.query.catalog.CategoryQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CategoryQueryServiceImpl implements CategoryQueryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryQueryServiceImpl.class);

    private final CategoryRepository categoryRepository;

    public CategoryQueryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<CategoryTreeResponse> getCategoryTree() {
        log.info("[CategoryQueryServiceImpl][getCategoryTree] Fetching active categories to construct nested hierarchy");
        List<CategoryEntity> activeCategories = categoryRepository.findAllByActiveTrueOrderByLevelAscNameAsc();

        Map<UUID, CategoryTreeResponse> nodeMap = new LinkedHashMap<>();
        List<CategoryTreeResponse> rootNodes = new ArrayList<>();

        for (CategoryEntity entity : activeCategories) {
            CategoryTreeResponse node = CategoryTreeResponse.fromEntity(entity);
            nodeMap.put(entity.getId(), node);

            if (entity.getParent() == null) {
                rootNodes.add(node);
            } else {
                CategoryTreeResponse parentNode = nodeMap.get(entity.getParent().getId());
                if (parentNode != null) {
                    parentNode.children().add(node);
                } else {
                    // Fallback in case parent is not active or missing
                    rootNodes.add(node);
                }
            }
        }

        return rootNodes;
    }
}
