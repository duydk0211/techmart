package com.techmart.core.bus.query.user;

import com.techmart.core.bus.Query;
import com.techmart.core.dto.response.user.UserProfileResponse;
import com.techmart.shared.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

public record GetStaffListQuery(Pageable pageable) implements Query<PagedResponse<UserProfileResponse>> {}
