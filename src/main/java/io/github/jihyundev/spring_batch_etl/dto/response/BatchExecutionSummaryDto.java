package io.github.jihyundev.spring_batch_etl.dto.response;

import io.github.jihyundev.spring_batch_etl.domain.batch.BatchDomainType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BatchExecutionSummaryDto {
    private Long id;

    private String jobName;
    private String stepName;
    private BatchDomainType domainType;
    private Long mallId;

    //실행 결과 상태
    private String status; //COMPLETED, FAILED, STOPPED 등
    //배치 실행 시간 정보
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double durationSec;

    //비즈니스 기준일
    LocalDateTime bizDateFrom; //시작일(매출,주문,가입일 등)
    LocalDateTime bizDateTo;   //종료일

    //Spring Batch 메타 정보 매핑
    Long jobInstanceId;
    Long jobExecutionId; // 재실행 요청시 필요

    private String exitMessage;

    //카운트 정보(Step 합산)
    private int readCount;
    private int writeCount;
    private int skipCount;
    private int errorLogCount;

    private int retryFlag; //재시도 가능 여부 1:가능, 0:불가능

    public void setRetryFlag(int retryFlag) {
        this.retryFlag = retryFlag;
    }
}
