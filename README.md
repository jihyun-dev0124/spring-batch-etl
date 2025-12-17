# Spring Batch 기반 쇼핑몰 데이터 통합 동기화 시스템
여러 쇼핑몰(Cafe24 기반)의 회원/주문/매출 데이터를 외부 API로부터 주기적으로 수집하여 하나의 통합 DB에서 관리하기 위한 Spring Batch 기반 배치 파이프라인입니다. <br>
단순 스케줄링을 넘어 대량 데이터 처리, 장애 대응, 재처리 전략, 실행 이력 관리까지 고려한 배치 아키텍처를 목표로 구현했습니다. <br>
(이 프로젝트에서는 회원 배치에 대한 내용만 담았습니다.)
<br><br>
## Running the Batch Jobs
이 프로젝트는 배치 작업의 수동 실행 및 예약 실행을 지원합니다. <br>
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

#### 실행 결과 table
* 배치 실행 이력 : tb_batch_execution_summary
* 실패/skip 이력 : tb_batch_error_log
* 재시도/retry 요청 내역 : tb_batch_retry_request
<br><br>
-----
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
-----
<br><br>
## 3. 동시성 및 안정성 관련 참고
* 배치는 쇼핑몰 단위로 병렬 실행됩니다.
* 동일한 mallId + 기간 + 도메인 파라미터는 동일 JobInstance로 인식되어 중복 실행이 제한됩니다.
* Job 실패 및 재실행 이력은 실행 요약 테이블(tb_batch_execution_summary)과 재실행 요청 테이블(tb_batch_retry_request)을 통해 관리됩니다.
* row 단위 실패, skip 이력은 에러 로그 테이블(tb_batch_error_log)에 기록됩니다.

이 내역을 통해 운영자는 관리자 화면에서 실패한 Job 목록과 파라미터를 확인하고 동일 파라미터로 특정 쇼핑몰의 배치 Job만 선택적으로 재실행할 수 있습니다.

-관리자 화면
<p>
<img width="640" height="245" alt="다운로드" src="https://github.com/user-attachments/assets/e164f138-d5c2-49f4-b3fa-c31f83729313" />
</p>

<br><br>




