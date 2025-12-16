package io.github.jihyundev.spring_batch_etl.dto.response;

import lombok.Data;

@Data
public class BatchExecutionKpi {
    private int todayTotal; //오늘 실행 수
    private int todayFailed; //오늘 실패 수
    private int pendingRetryRows; // 재처리 대기(에러 로그)
    private Double avgDurationSec; // 평균 소요(초)
}
