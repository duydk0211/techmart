package com.techmart.core.controller.catalog;

import com.techmart.core.dto.request.catalog.BrandRequest;
import com.techmart.core.dto.response.catalog.BrandResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface InternalBrandApiController {

    ResponseEntity<List<BrandResponse>> getBrandList();

    ResponseEntity<BrandResponse> createBrand(BrandRequest request);
}
