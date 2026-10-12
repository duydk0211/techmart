package com.techmart.core.controller.admin.impl;

import com.techmart.core.bus.SpringBus;
import com.techmart.core.bus.query.user.GetCustomerDetailQuery;
import com.techmart.core.bus.query.user.GetCustomerListQuery;
import com.techmart.core.controller.admin.InternalCustomerApiController;
import com.techmart.core.dto.response.user.CustomerDetailResponse;
import com.techmart.core.dto.response.user.UserProfileResponse;
import com.techmart.shared.dto.PagedResponse;
import com.techmart.shared.security.TechmartPreauthorize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/admin/customers")
public class InternalCustomerApiControllerImpl implements InternalCustomerApiController {

    private static final Logger log = LoggerFactory.getLogger(InternalCustomerApiControllerImpl.class);

    private final SpringBus springBus;

    public InternalCustomerApiControllerImpl(SpringBus springBus) {
        this.springBus = springBus;
    }

    @Override
    @GetMapping
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"ADMIN"})
    public ResponseEntity<PagedResponse<UserProfileResponse>> getCustomerList(
            @RequestParam(name = "keyword", required = false) String keyword,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        log.info("[InternalCustomerApiControllerImpl][getCustomerList] Admin fetching customer list with keyword: {}", keyword);
        PagedResponse<UserProfileResponse> response = springBus.query(new GetCustomerListQuery(keyword, pageable));
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/{id}")
    @TechmartPreauthorize(gateway = "TECHMART_GW", roles = {"ADMIN"})
    public ResponseEntity<CustomerDetailResponse> getCustomerDetail(@PathVariable("id") UUID id) {
        log.info("[InternalCustomerApiControllerImpl][getCustomerDetail] Admin fetching customer detail: {}", id);
        CustomerDetailResponse response = springBus.query(new GetCustomerDetailQuery(id));
        return ResponseEntity.ok(response);
    }
}
