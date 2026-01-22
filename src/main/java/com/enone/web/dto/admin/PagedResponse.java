package com.enone.web.dto.admin;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PagedResponse<T> {
    private List<T> content;
    private int number; 
    private int size; 
    private int totalPages;
    private long totalElements;
    private boolean first;
    private boolean last;
    private boolean empty;
}