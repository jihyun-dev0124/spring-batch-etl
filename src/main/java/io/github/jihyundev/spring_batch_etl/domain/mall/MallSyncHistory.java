package io.github.jihyundev.spring_batch_etl.domain.mall;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MallSyncHistory {
    private Long id;
    private Long mallId;
    private MallSyncType syncType;
    private Long JobExecutionId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private MallSyncStatus status;
    private int totalRead;
    private int totalWritten;
    private int totalSkipped;
    private String errorCode;
    private String errorMsg;
}
