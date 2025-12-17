# Spring Batch 기반 쇼핑몰 데이터 통합 동기화 시스템
여러 쇼핑몰(Cafe24 기반)의 회원/주문/매출 데이터를 외부 API로부터 주기적으로 수집하여 하나의 통합 DB에서 관리하기 위한 Spring Batch 기반 배치 파이프라인입니다. <br>
단순 스케줄링을 넘어 대량 데이터 처리, 외부 API 장애 대응, 재처리 전략, 실행 이력 기반 운영 관리까지 고려한 배치 아키텍처로<br>
실제 운영 환경에서 실패해도 다시 운영할 수 있는 배치를 만드는 것을 목표로 구현했습니다. <br>
(이 프로젝트에서는 대표적으로 회원 배치에 대한 내용만 담았습니다.)
<br><br>

## 아키텍처 요약
1. 쇼핑몰(mallId) 단위 Job 분리
2. Spring Batch Chunk 기반 ETL 구조
3. Reader / Processor / Writer 역할 분리
4. 실패 유형별 Retry / Skip / Fail-fast 전략 사용
5. 실행 이력 + 에러 로그 테이블 기반 운영 관리
6. 비동기 Job 실행과 동시성 제어
<br><br>

### 기술 스택
* java17
* Spring Boot 3.5.8
* Spring Batch 5.x
* Spring Scheduler
* MariaDB
* MyBatis
<br><br>

## 배치 구성 요소
<p><img width="670" height="461" alt="스크린샷 2025-12-13 오후 4 32 04" src="https://github.com/user-attachments/assets/058296ec-64a2-418a-84a0-8620e906c656" /></p>
<br><br>

### 1. ItemReader - 외부 API 기반 Reader
* 쇼핑몰별 API 설정(MallConfig) 기반 호출
* JobParameter 기반 실행 범위 제어 (mallId, startDate, endDate, pageSize, attemptCount)
* offset/limit 기반 페이징 처리
* count 기반 종료 조건
* RetryTemplate 기반 API 재시도
* 재시작(Restart)를 고려한 ExecutionContext 관리
* 재시도 실패 시 fail-fast 전략 적용

> Reader 단계의 실패는 데이터 정합성과 신뢰성을 잃게 되는 위험이 있기 때문에 재시도는 허용하되 실패 시 Job을 즉시 중단하는 전략을 사용합니다.
<br><br>

### 2. ItemProcessor - 데이터 검증 및 Skip 전략
* API 응답 DTO -> 내부 도메인 엔티티 변환
* 필수 필드 검증 (ex. memberId, email, phone)
* 데이터 정규화 (email, phone)
* 데이터 품질 오류 발생 시 InvalidDataException
* Processor 단계 오류는 row 단위 Skip 처리
<br><br>
### 3. ItemWriter - MyBatis Batch Upsert
* Chunk 단위 DB 반영
* MyBatis 기반 batch upsert
* 멱등성(Idempotency) 확보
* 유니크 키 기준 정렬을 통한 데드락 완화




<br><br><br>
## 배치 실행 방법
이 프로젝트는 배치 작업의 수동 실행과 스케줄러 기반 실행을 지원합니다. <br>
작업은 안전한 동시성 제어를 보장하기 위해 고정된 값의 JobParameters를 사용하여 쇼핑몰마다 실행됩니다. <br>
⚠️ 중복 실행을 방지하기 위해 이 프로젝트는 RunIdIncrementer 또는 timestamp 기반 매개변수를 사용하지 않습니다.
<br><br>
### 1. 수동 실행 (Command 기반)
운영자 또는 개발자가 특정 쇼핑몰의 특정 조건에 대해 배치를 직접 실행해야 할 경우 사용합니다. (ex. 재처리, 장애 복구)

#### 실행 예시
```JAVA
JobParameters params = new JobParametersBuilder()
        .addLong("mallId", 1L)
        .addString("domainType", "MEMBER")
        .addString("startDate", "2025-12-01T00:00:00")
        .addString("endDate", "2025-12-17T23:59:59")
        .addLong("pageSize", 1000L)
        .addLong("attemptCount", 1L) //job 실행 횟수
        .toJobParameters();

jobLauncher.run(memberSyncJob, params);
```

#### Parameters
| 파라미터       | 설명                     |
| ---------- | ---------------------- |
| mallId     | 대상 쇼핑몰 ID              |
| domainType | MEMBER / ORDER / SALES |
| startDate  | 데이터 수집 시작 시점           |
| endDate    | 데이터 수집 종료 시점           |
| pageSize   | 외부 API 조회 단위 (최대 1000) |
| attemptCount   | job 실행 횟수 (default 1, 재실행시 1씩 증가) |
* attemptCount는 운영자가 의도적으로 재실행했음을 구분하기 위한 파라미터로 자동 증가되는 RunIdIncrementer를 대체합니다.

#### 실행 결과 table
* 배치 실행 이력 : tb_batch_execution_summary
* 실패/skip 이력 : tb_batch_error_log
* 재시도/retry 요청 내역 : tb_batch_retry_request

<br><br>
### 2. 스케줄러 기반 실행 (@Scheduled/cron)
운영 환경에서는 @Scheduled 기반 스케줄러를 통해 쇼핑몰 단위로 Job을 자동 실행합니다. <br>
쇼핑몰별 Job은 병렬 실행되며 동일 쇼핑몰에 대한 중복 실행은 제한되도록 설계되어 있습니다.

#### 스케줄러 동작 흐름
1. 스케줄러가 cron 주기에 따라 실행
2. 활성화된 쇼핑몰 목록 조회
3. mallId별 JobParameters 생성
4. JobLauncher를 통해 Job 실행
5. 실행 결과 및 실패 이력 저장

#### 스케줄러 예시 코드 
```JAVA
@Component
@RequiredArgsConstructor
public class MemberJobScheduler {
    private final JobLauncher jobLauncher;
    private final Job memberSyncJob;
    private final MallConfigMapper mallConfigMapper;

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 전날 가입한 회원 정보 동기화 / 매일 자정 배치 실행 
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void runDailyMemberSyncJob() {
        LocalDate today = LocalDate.now(ZONE_SEOUL);
        LocalDateTime startDate = today.minusDays(1).atStartOfDay();
        LocalDateTime endDate = today.atStartOfDay();

        mallConfigMapper.findAllMallIds().forEach(mallId -> {
            runMemberSyncJob(mallId, startDate, endDate, 1000);
        });
    }

    public void runMemberSyncJob(Long mallId, LocalDateTime startDate, LocalDateTime endDate, Integer pageSize) {
        String startDateStr = startDate.format(FORMATTER);
        String endDateStr = endDate.format(FORMATTER);

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("mallId", mallId)
                .addString("startDate", startDateStr)
                .addString("endDate", endDateStr)
                .addString("domainType", BatchDomainType.MEMBER.toString())
                .addLong("pageSize", pageSize != null ? pageSize.longValue() : 1000L)
                .addLong("attemptCount", 1L) //job 실행 횟수
                .toJobParameters();

        try {
            JobExecution execution = jobLauncher.run(memberSyncJob, jobParameters);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException e) {
            log.warn("[MemberJobScheduler] Job not started. reason={}", e.getClass().getSimpleName(), e);
        } catch (Exception e) {
            log.error("[MemberJobScheduler] Unexpected error while running memberSyncJob", e);
        }
    }
}

```

<br><br>
## 동시성 및 안정성 관련 참고
* 배치는 쇼핑몰 단위로 병렬 실행됩니다.
* 동일한 mallId + 기간 + 도메인 파라미터는 동일 JobInstance로 인식되어 중복 실행이 제한됩니다. <br>
이를 통해 스케줄러 재실행이나 운영자 수동 실행으로 인해 동일 쇼핑몰 배치가 동시에 실행되는 상황을 방지합니다.


<br><br>
## 배치 실행 중 실패 발생 시 운영 흐름
* Job 실패 및 재실행 이력은 실행 요약 테이블(tb_batch_execution_summary)과 재실행 요청 테이블(tb_batch_retry_request)을 통해 관리됩니다.
* row 단위 실패, skip 이력은 에러 로그 테이블(tb_batch_error_log)에 기록됩니다.

이 내역을 통해 운영자는 관리자 화면에서 실패한 Job 목록과 파라미터를 확인하고 동일 파라미터로 특정 쇼핑몰의 배치 Job만 선택적으로 재실행할 수 있습니다.

-관리자 화면
<p>
<img width="640" height="245" alt="다운로드" src="https://github.com/user-attachments/assets/e164f138-d5c2-49f4-b3fa-c31f83729313" />
</p>

<br><br>




