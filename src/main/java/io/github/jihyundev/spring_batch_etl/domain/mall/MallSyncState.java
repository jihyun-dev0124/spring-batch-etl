package io.github.jihyundev.spring_batch_etl.domain.mall;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MallSyncState {
    private Long mallId;
    private MallSyncType syncType;
    private LocalDateTime lastSyncedAt;
    private MallSyncStatus lastStatus;
    private String lastErrorCode;
    private String lastErrorMsg;
    private int retryCount;
    private LocalDateTime nextRetryAt;
    private LocalDateTime updatedAt;
}
