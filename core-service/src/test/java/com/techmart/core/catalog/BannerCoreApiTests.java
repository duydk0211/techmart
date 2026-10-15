package com.techmart.core.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techmart.core.authorization.UserContext;
import com.techmart.core.dto.request.catalog.BannerRequest;
import com.techmart.core.entity.catalog.BannerEntity;
import com.techmart.core.repository.catalog.BannerRepository;
import com.techmart.shared.constant.HeaderConstants;
import com.techmart.shared.constant.ResponseCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BannerCoreApiTests {

    private static final String VALID_INTERNAL_SECRET = "techmart_internal_secret_2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BannerRepository bannerRepository;

    @BeforeEach
    void setUp() {
        bannerRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
        bannerRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /internal/api/v1/banners returns 200 OK with only active banners in correct order")
    void testGetActiveBannersSuccess() throws Exception {
        BannerEntity b1 = new BannerEntity();
        b1.setTitle("Banner Order 2");
        b1.setImageUrl("http://localhost:9000/techmart-banners/b1.jpg");
        b1.setTargetUrl("https://techmart.vn/promo/b1");
        b1.setDisplayOrder(2);
        b1.setActive(true);
        bannerRepository.save(b1);

        BannerEntity b2 = new BannerEntity();
        b2.setTitle("Banner Order 1");
        b2.setImageUrl("http://localhost:9000/techmart-banners/b2.jpg");
        b2.setDisplayOrder(1);
        b2.setActive(true);
        bannerRepository.save(b2);

        BannerEntity inactiveBanner = new BannerEntity();
        inactiveBanner.setTitle("Banner Inactive");
        inactiveBanner.setImageUrl("http://localhost:9000/techmart-banners/inactive.jpg");
        inactiveBanner.setDisplayOrder(0);
        inactiveBanner.setActive(false);
        bannerRepository.save(inactiveBanner);

        mockMvc.perform(get("/internal/api/v1/banners")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Banner Order 1"))
                .andExpect(jsonPath("$[0].displayOrder").value(1))
                .andExpect(jsonPath("$[1].title").value("Banner Order 2"))
                .andExpect(jsonPath("$[1].displayOrder").value(2));
    }

    @Test
    @DisplayName("POST /internal/api/v1/banners creates banner for STAFF/ADMIN and sets default values")
    void testCreateBannerSuccess() throws Exception {
        BannerRequest request = new BannerRequest(
                "Super Tech Sale 2026",
                "http://localhost:9000/techmart-banners/sale2026.png",
                "https://techmart.vn/promo/sale2026",
                null,
                null
        );

        mockMvc.perform(post("/internal/api/v1/banners")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "STAFF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Super Tech Sale 2026"))
                .andExpect(jsonPath("$.imageUrl").value("http://localhost:9000/techmart-banners/sale2026.png"))
                .andExpect(jsonPath("$.targetUrl").value("https://techmart.vn/promo/sale2026"))
                .andExpect(jsonPath("$.displayOrder").value(0))
                .andExpect(jsonPath("$.active").value(true));

        assertEquals(1, bannerRepository.count());
    }

    @Test
    @DisplayName("POST /internal/api/v1/banners creates banner for ADMIN with custom displayOrder and active")
    void testCreateBannerAdminSuccess() throws Exception {
        BannerRequest request = new BannerRequest(
                "Admin Tech Banner",
                "http://localhost:9000/techmart-banners/admin-banner.png",
                "https://techmart.vn/admin/promo",
                5,
                true
        );

        mockMvc.perform(post("/internal/api/v1/banners")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Admin Tech Banner"))
                .andExpect(jsonPath("$.displayOrder").value(5))
                .andExpect(jsonPath("$.active").value(true));

        assertEquals(1, bannerRepository.count());
    }

    @Test
    @DisplayName("GET /internal/api/v1/banners sorts by displayOrder ASC and createdAt DESC when displayOrder is identical")
    void testGetActiveBannersSortCreatedAtDesc() throws Exception {
        BannerEntity bFirst = new BannerEntity();
        bFirst.setTitle("Older Banner");
        bFirst.setImageUrl("http://localhost:9000/techmart-banners/old.jpg");
        bFirst.setDisplayOrder(1);
        bFirst.setActive(true);
        bannerRepository.saveAndFlush(bFirst);

        Thread.sleep(20);

        BannerEntity bSecond = new BannerEntity();
        bSecond.setTitle("Newer Banner");
        bSecond.setImageUrl("http://localhost:9000/techmart-banners/new.jpg");
        bSecond.setDisplayOrder(1);
        bSecond.setActive(true);
        bannerRepository.saveAndFlush(bSecond);

        mockMvc.perform(get("/internal/api/v1/banners")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Newer Banner"))
                .andExpect(jsonPath("$[1].title").value("Older Banner"));
    }

    @Test
    @DisplayName("POST /internal/api/v1/banners rejects empty title and imageUrl with 400 VALIDATION_FAILED")
    void testCreateBannerValidationFailure() throws Exception {
        BannerRequest request = new BannerRequest(
                "",
                "",
                null,
                1,
                true
        );

        mockMvc.perform(post("/internal/api/v1/banners")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResponseCode.VALIDATION_FAILED.getCode()));
    }

    @Test
    @DisplayName("POST /internal/api/v1/banners rejects CUSTOMER role with 403 ACCESS_DENIED")
    void testCreateBannerCustomerForbidden() throws Exception {
        BannerRequest request = new BannerRequest(
                "Customer Banner",
                "http://localhost:9000/techmart-banners/test.png",
                null,
                0,
                true
        );

        mockMvc.perform(post("/internal/api/v1/banners")
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.ACCESS_DENIED.getCode()));
    }

    @Test
    @DisplayName("DELETE /internal/api/v1/banners/{id} removes banner for ADMIN")
    void testDeleteBannerSuccess() throws Exception {
        BannerEntity banner = new BannerEntity();
        banner.setTitle("To Delete");
        banner.setImageUrl("http://localhost:9000/techmart-banners/delete.jpg");
        banner.setDisplayOrder(0);
        banner.setActive(true);
        BannerEntity saved = bannerRepository.save(banner);

        mockMvc.perform(delete("/internal/api/v1/banners/" + saved.getId())
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNoContent());

        assertFalse(bannerRepository.existsById(saved.getId()));
    }

    @Test
    @DisplayName("DELETE /internal/api/v1/banners/{id} rejects STAFF role with 403 ACCESS_DENIED")
    void testDeleteBannerStaffForbidden() throws Exception {
        BannerEntity banner = new BannerEntity();
        banner.setTitle("Protected Banner");
        banner.setImageUrl("http://localhost:9000/techmart-banners/protected.jpg");
        banner.setDisplayOrder(0);
        banner.setActive(true);
        BannerEntity saved = bannerRepository.save(banner);

        mockMvc.perform(delete("/internal/api/v1/banners/" + saved.getId())
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "STAFF"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.ACCESS_DENIED.getCode()));
    }

    @Test
    @DisplayName("DELETE /internal/api/v1/banners/{id} for non-existent id returns 404 RESOURCE_NOT_FOUND")
    void testDeleteBannerNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(delete("/internal/api/v1/banners/" + nonExistentId)
                        .header(HeaderConstants.X_INTERNAL_SECRET, VALID_INTERNAL_SECRET)
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ResponseCode.RESOURCE_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("Banner APIs reject requests with missing or invalid internal secret")
    void testBannerApiMissingSecret() throws Exception {
        mockMvc.perform(get("/internal/api/v1/banners"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INTERNAL_SECRET.getCode()));
    }
}
