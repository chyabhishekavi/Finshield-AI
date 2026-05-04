package com.finshield.backend.common.api;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty
) {

    public PageResponse {
        content = List.copyOf(Objects.requireNonNull(content, "content must not be null"));
        if (page < 0 || size < 0 || totalElements < 0 || totalPages < 0) {
            throw new IllegalArgumentException("page metadata must not be negative");
        }
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        Objects.requireNonNull(page, "page must not be null");
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }

    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        return from(page.map(mapper));
    }
}
