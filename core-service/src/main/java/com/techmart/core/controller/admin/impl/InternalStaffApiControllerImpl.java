package com.techmart.core.controller.admin.impl;

import com.techmart.core.authorization.UserContext;
import com.techmart.core.bus.SpringBus;
import com.techmart.core.bus.command.user.CreateStaffCommand;
import com.techmart.core.bus.command.user.DeleteStaffCommand;
import com.techmart.core.bus.query.user.GetStaffListQuery;
import com.techmart.core.controller.admin.InternalStaffApiController;
import com.techmart.core.dto.request.user.CreateStaffRequest;
import com.techmart.core.dto.response.user.UserProfileResponse;
import com.techmart.shared.dto.PagedResponse;
import com.techmart.shared.security.TechmartPreauthorize;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/admin/staff")
public class InternalStaffApiControllerImpl implements InternalStaffApiController {

    private static final Logger log = LoggerFactory.getLogger(InternalStaffApiControllerImpl.class);

    private final SpringBus springBus;

    public InternalStaffApiControllerImpl(SpringBus springBus) {
        this.springBus = springBus;
    }

    @Override
    @PostMapping
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"ADMIN"})
    public ResponseEntity<UserProfileResponse> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        UUID createdBy = UserContext.getCurrentUser() != null ? UserContext.getCurrentUser().userId() : null;
        log.info("[InternalStaffApiControllerImpl][createStaff] Admin creating staff account: {}", request.email());
        UserProfileResponse response = springBus.execute(new CreateStaffCommand(request, createdBy));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @GetMapping
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"ADMIN"})
    public ResponseEntity<PagedResponse<UserProfileResponse>> getStaffList(@PageableDefault(page = 0, size = 10) Pageable pageable) {
        log.info("[InternalStaffApiControllerImpl][getStaffList] Admin fetching staff list");
        PagedResponse<UserProfileResponse> response = springBus.query(new GetStaffListQuery(pageable));
        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/{id}")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"ADMIN"})
    public ResponseEntity<Void> deleteStaff(@PathVariable("id") UUID id) {
        log.info("[InternalStaffApiControllerImpl][deleteStaff] Admin deactivating staff account: {}", id);
        springBus.execute(new DeleteStaffCommand(id));
        return ResponseEntity.noContent().build();
    }
}
