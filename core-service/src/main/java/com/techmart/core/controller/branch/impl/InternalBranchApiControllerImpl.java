package com.techmart.core.controller.branch.impl;

import com.techmart.core.bus.SpringBus;
import com.techmart.core.bus.command.branch.CreateBranchCommand;
import com.techmart.core.bus.query.branch.GetBranchInventoryQuery;
import com.techmart.core.bus.query.branch.GetBranchListQuery;
import com.techmart.core.controller.branch.InternalBranchApiController;
import com.techmart.core.dto.request.branch.BranchRequest;
import com.techmart.core.dto.response.branch.BranchInventoryItemResponse;
import com.techmart.core.dto.response.branch.BranchResponse;
import com.techmart.shared.dto.PagedResponse;
import com.techmart.shared.security.TechmartPreauthorize;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/branches")
public class InternalBranchApiControllerImpl implements InternalBranchApiController {

    private static final Logger log = LoggerFactory.getLogger(InternalBranchApiControllerImpl.class);

    private final SpringBus springBus;

    public InternalBranchApiControllerImpl(SpringBus springBus) {
        this.springBus = springBus;
    }

    @Override
    @GetMapping
    @TechmartPreauthorize(gateway = "TECHMART_GW", authenticated = false)
    public ResponseEntity<List<BranchResponse>> getBranchList() {
        log.info("[InternalBranchApiControllerImpl][getBranchList] Fetching branch list");
        List<BranchResponse> branches = springBus.query(new GetBranchListQuery());
        return ResponseEntity.ok(branches);
    }

    @Override
    @PostMapping
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"ADMIN"})
    public ResponseEntity<BranchResponse> createBranch(@Valid @RequestBody BranchRequest request) {
        log.info("[InternalBranchApiControllerImpl][createBranch] Creating branch: {}", request.name());
        BranchResponse response = springBus.execute(new CreateBranchCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @GetMapping("/{id}/inventory")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"STAFF", "ADMIN"})
    public ResponseEntity<PagedResponse<BranchInventoryItemResponse>> getBranchInventory(
            @PathVariable("id") UUID id,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {
        log.info("[InternalBranchApiControllerImpl][getBranchInventory] Fetching branch inventory for branch: {}", id);
        PagedResponse<BranchInventoryItemResponse> response = springBus.query(new GetBranchInventoryQuery(id, pageable));
        return ResponseEntity.ok(response);
    }
}
