package org.example.dto.request;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.example.dto.RequestPayload;

@JsonTypeName(value = "pageRequest")
public class PageRequest implements RequestPayload {
    private int pageNumber;
    private int pageSize;

    private PageRequest(int pageNumber, int pageSize) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber must be >= 0");
        }
        if (pageSize <= 0 || pageSize > 1000) {
            throw new IllegalArgumentException("pageSize must be between 1 and 1000");
        }
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public static PageRequest of(int pageNumber, int pageSize) {
        return new PageRequest(pageNumber, pageSize);
    }
}
