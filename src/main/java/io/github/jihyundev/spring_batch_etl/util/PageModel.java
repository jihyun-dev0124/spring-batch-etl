package io.github.jihyundev.spring_batch_etl.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PageModel {
    private final int pageNo;
    private final int size;
    private final long totalElements;

    public int getTotalPages() {
        if(size == 0) return 0;
        return (int) Math.ceil((double) totalElements / (double) size);
    }
}
