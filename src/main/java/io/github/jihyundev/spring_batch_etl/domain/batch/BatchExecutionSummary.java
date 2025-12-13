package io.github.jihyundev.spring_batch_etl.domain.batch;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BatchExecutionSummary {
    private Long id;
    private String jobName;
    private String stepName;
    private BatchDomainType domainType;
    private Long mallId;

    //비즈니스 기준일
    LocalDateTime bizDateFrom; //시작일(매출,주문,가입일 등)
    LocalDateTime bizDateTo;   //종료일

    //Spring Batch 메타 정보 매핑
    Long jobInstanceId;
    Long jobExecutionId;

    //JobParameter JSON형태로 저장
    String parameterJson;

    //실행 결과 상태
    private String status; //COMPLETED, FAILED, STOPPED 등
    private String exitCode;
    private String exitMessage;

    //카운트 정보(Step 합산)
    private int readCount;
    private int writeCount;
    private int filterCount;
    private int readSkipCount;
    private int processSkipCount;
    private int writeSkipCount;
    private int errorLogCount;

    //시간 정보
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;

    //기타 플래그
    private int successFlag; //1:성공, 0:실패
    private int alertSentFlag; //실패 시 알림 발송 여부

    private LocalDateTime createdAt;
}
