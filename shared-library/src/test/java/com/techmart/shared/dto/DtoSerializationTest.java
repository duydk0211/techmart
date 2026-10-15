package com.techmart.shared.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void shouldSerializeErrorResponseMatchingApiSpec() throws Exception {
        ErrorResponse response = ErrorResponse.builder()
                .status(400)
                .code("VALIDATION_FAILED")
                .message("Validation failed for one or more fields.")
                .timestamp(Instant.parse("2026-10-09T16:30:00Z"))
                .path("/api/v1/products")
                .traceId("c83f9821-4fa3-4cb5-8857-e685f67b4b12")
                .errors(List.of(
                        FieldErrorItem.builder()
                                .field("price")
                                .rejectedValue(-1000)
                                .message("Giá bán sản phẩm phải lớn hơn 0")
                                .build()
                ))
                .build();

        String json = objectMapper.writeValueAsString(response);
        JsonNode node = objectMapper.readTree(json);

        assertEquals(400, node.get("status").asInt());
        assertEquals("VALIDATION_FAILED", node.get("code").asText());
        assertEquals("c83f9821-4fa3-4cb5-8857-e685f67b4b12", node.get("traceId").asText());
        assertEquals(1, node.get("errors").size());
        assertEquals("price", node.get("errors").get(0).get("field").asText());
    }

    @Test
    void shouldOmitErrorsFieldWhenErrorsListIsEmptyOrNull() throws Exception {
        // Case 1: errors is null
        ErrorResponse responseWithoutErrors = ErrorResponse.builder()
                .status(404)
                .code("USER_NOT_FOUND")
                .message("User not found.")
                .timestamp(Instant.parse("2026-10-09T16:30:00Z"))
                .path("/api/v1/users/123")
                .traceId("trace-123")
                .build();

        String jsonNullErrors = objectMapper.writeValueAsString(responseWithoutErrors);
        JsonNode nodeNullErrors = objectMapper.readTree(jsonNullErrors);
        assertFalse(nodeNullErrors.has("errors"), "errors field should be omitted when null");

        // Case 2: errors is empty list
        responseWithoutErrors.setErrors(List.of());
        String jsonEmptyErrors = objectMapper.writeValueAsString(responseWithoutErrors);
        JsonNode nodeEmptyErrors = objectMapper.readTree(jsonEmptyErrors);
        assertFalse(nodeEmptyErrors.has("errors"), "errors field should be omitted when empty list");
    }

    @Test
    void shouldSerializePagedResponseMatchingApiSpec() throws Exception {
        PagedResponse<String> response = PagedResponse.of(List.of("item1", "item2"), 0, 20, 150);

        String json = objectMapper.writeValueAsString(response);
        JsonNode node = objectMapper.readTree(json);

        assertEquals(0, node.get("page").asInt());
        assertEquals(20, node.get("size").asInt());
        assertEquals(150, node.get("totalElements").asLong());
        assertEquals(8, node.get("totalPages").asInt());
        assertTrue(node.get("first").asBoolean());
        assertFalse(node.get("last").asBoolean());
        assertEquals(2, node.get("content").size());
        assertEquals("item1", node.get("content").get(0).asText());
    }

    @Test
    void shouldHandleBoundaryConditionsInPagedResponse() {
        // Boundary 1: Empty content and 0 total elements
        PagedResponse<String> emptyResponse = PagedResponse.of(null, 0, 10, 0);
        assertTrue(emptyResponse.getContent().isEmpty());
        assertEquals(0, emptyResponse.getPage());
        assertEquals(10, emptyResponse.getSize());
        assertEquals(0, emptyResponse.getTotalElements());
        assertEquals(0, emptyResponse.getTotalPages());
        assertTrue(emptyResponse.isFirst());
        assertTrue(emptyResponse.isLast());

        // Boundary 2: Zero page size
        PagedResponse<String> zeroSizeResponse = PagedResponse.of(List.of("a"), 0, 0, 1);
        assertEquals(0, zeroSizeResponse.getTotalPages());
        assertTrue(zeroSizeResponse.isFirst());
        assertTrue(zeroSizeResponse.isLast());

        // Boundary 3: Last page
        PagedResponse<String> lastPageResponse = PagedResponse.of(List.of("item"), 3, 10, 35);
        assertEquals(4, lastPageResponse.getTotalPages());
        assertFalse(lastPageResponse.isFirst());
        assertTrue(lastPageResponse.isLast());

        // Boundary 4: Middle page
        PagedResponse<String> middlePageResponse = PagedResponse.of(List.of("item"), 1, 10, 35);
        assertEquals(4, middlePageResponse.getTotalPages());
        assertFalse(middlePageResponse.isFirst());
        assertFalse(middlePageResponse.isLast());
    }
}
