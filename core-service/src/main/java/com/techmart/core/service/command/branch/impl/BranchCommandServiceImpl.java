package com.techmart.core.service.command.branch.impl;

import com.techmart.core.dto.request.branch.BranchRequest;
import com.techmart.core.dto.response.branch.BranchResponse;
import com.techmart.core.entity.branch.BranchEntity;
import com.techmart.core.repository.branch.BranchRepository;
import com.techmart.core.service.command.branch.BranchCommandService;
import com.techmart.shared.constant.ResponseCode;
import com.techmart.shared.exception.CoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BranchCommandServiceImpl implements BranchCommandService {

    private static final Logger log = LoggerFactory.getLogger(BranchCommandServiceImpl.class);

    private final BranchRepository branchRepository;

    public BranchCommandServiceImpl(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    @Override
    public BranchResponse createBranch(BranchRequest request) {
        log.info("[BranchCommandServiceImpl][createBranch] Creating branch: {}", request.name());

        if (branchRepository.existsByNameIgnoreCase(request.name())) {
            throw new CoreException(ResponseCode.DUPLICATE_RESOURCE, "Branch with name already exists: " + request.name());
        }

        BranchEntity entity = new BranchEntity();
        entity.setName(request.name());
        entity.setAddress(request.address());
        entity.setPhone(request.phone());
        entity.setQuantityAvailable(0);
        entity.setActive(true);

        BranchEntity saved = branchRepository.save(entity);
        log.info("[BranchCommandServiceImpl][createBranch] Created branch id: {}", saved.getId());
        return BranchResponse.fromEntity(saved);
    }
}
