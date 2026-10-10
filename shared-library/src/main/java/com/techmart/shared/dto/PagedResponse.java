package com.techmart.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * Standard paginated response envelope matching api-spec section 2.1.
 *
 * @param <T> Element type of the content list
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> implements Serializable {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int calculatedPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return PagedResponse.<T>builder()
                .content(content != null ? content : List.of())
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(calculatedPages)
                .first(page == 0)
                .last(page >= calculatedPages - 1 || calculatedPages == 0)
                .build();
    }
}
