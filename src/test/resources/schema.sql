CREATE TABLE `tb_mall_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `mall_id` BIGINT NOT NULL,
  `member_id` VARCHAR(50) NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `phone` VARCHAR(20) NOT NULL,
  `email` VARCHAR(100) DEFAULT NULL,
  `status` enum('ACTIVE','DELETED') NOT NULL,
  `join_path` VARCHAR(10) DEFAULT NULL,
  `joined_at` datetime NOT NULL,
  `created_at` datetime NOT NULL,
  `last_modified_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `member_id` (`member_id`),
  UNIQUE KEY `phone` (`phone`)
);

CREATE TABLE `tb_mall_config` (
  `mall_id` BIGINT NOT NULL,
  `mall_name` VARCHAR(100) DEFAULT NULL,
  `api_url` VARCHAR(300) DEFAULT NULL COMMENT 'api_url',
  `api_id` VARCHAR(100) DEFAULT NULL COMMENT 'api_아이디',
  `api_pw` VARCHAR(200) DEFAULT NULL COMMENT 'api_비밀번호',
  `api_token_value` VARCHAR(300) DEFAULT NULL COMMENT 'api_토큰_값',
  `api_refresh_token_value` VARCHAR(300) DEFAULT NULL COMMENT 'api_리프레시_토큰_값(토큰 재발급시 사용)',
  `api_token_expire` datetime DEFAULT NULL COMMENT 'api_토큰_만료일자',
  `api_refresh_token_expire` datetime DEFAULT NULL COMMENT 'api_리프레시_토큰_만료일자',
  `api_version` VARCHAR(300) DEFAULT NULL COMMENT 'api_version',
  `is_usable` enum('T','F') NOT NULL DEFAULT 'F' COMMENT '사용여부',
  `created_at` datetime NOT NULL,
  `last_modified_at` datetime DEFAULT NULL,
  PRIMARY KEY (`mall_id`)
);

CREATE TABLE `tb_mall_sync_state` (
  `mall_id` BIGINT NOT NULL,
  `sync_type` VARCHAR(20) NOT NULL COMMENT 'MEMBER, ORDER, SALES',
  `last_synced_at` datetime DEFAULT NULL COMMENT '마지막 성공 동기화 기준 시각',
  `last_status` VARCHAR(20) NOT NULL COMMENT 'IDLE, RUNNING, SUCCESS, FAILED',
  `last_error_code` VARCHAR(100) DEFAULT NULL,
  `last_error_msg` VARCHAR(500) DEFAULT NULL,
  `retry_count` INT(11) NOT NULL DEFAULT 0,
  `next_retry_at` datetime DEFAULT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`mall_id`,`sync_type`)
);


CREATE TABLE tb_batch_error_log (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  job_name     VARCHAR(100) NOT NULL,
  step_name    VARCHAR(100) NOT NULL,
  domain_type  VARCHAR(20)  NOT NULL,
  mall_id      BIGINT,
  entity_key   VARCHAR(100),
  biz_date     DATE,
  error_type   VARCHAR(30)  NOT NULL,
  error_message VARCHAR(1000),
  raw_payload  CLOB, -- 또는 VARCHAR(4000) 등, H2에서 되는 타입으로
  retry_flag   TINYINT NOT NULL DEFAULT 0,
  retry_count  INT NOT NULL DEFAULT 0,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `tb_batch_retry_request` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `job_name` VARCHAR(100) NOT NULL,
  `job_instance_id` BIGINT NOT NULL,
  `job_execution_id` BIGINT NOT NULL,
  `mall_id` BIGINT NOT NULL,
  `domain_type` VARCHAR(20) DEFAULT NULL COMMENT 'MEMBER,ORDER,SALES',
  `biz_date_from` datetime DEFAULT NULL COMMENT '비즈니스 기준 시작일(매출일/주문일/가입일 등)',
  `biz_date_to` datetime DEFAULT NULL COMMENT '비즈니스 기준 종료일(매출일/주문일/가입일 등)',
  `parameter_json` CLOB,
  `status` VARCHAR(20) NOT NULL COMMENT 'REQUESTED,RUNNING,FAILED,DONE',
  `attempt_count` INT(11) NOT NULL DEFAULT 1 COMMENT 'job 실행횟수',
  `requested_by` VARCHAR(100) NOT NULL,
  `requested_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `tb_batch_execution_summary` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `job_name` VARCHAR(100) NOT NULL,
  `step_name` VARCHAR(100) DEFAULT NULL,
  `domain_type` VARCHAR(20) DEFAULT NULL COMMENT 'MEMBER,ORDER,SALES',
  `mall_id` BIGINT DEFAULT NULL,
  `biz_date_from` datetime DEFAULT NULL COMMENT '비즈니스 기준 시작일(매출일/주문일/가입일 등)',
  `biz_date_to` datetime DEFAULT NULL COMMENT '비즈니스 기준 종료일(매출일/주문일/가입일 등)',
  `job_instance_id` BIGINT DEFAULT NULL,
  `job_execution_id` BIGINT DEFAULT NULL,
  `parameter_json` CLOB,
  `status` VARCHAR(20) NOT NULL COMMENT 'COMPLETED, FAILED, STOPPED 등',
  `exit_code` VARCHAR(250) DEFAULT NULL,
  `exit_message` VARCHAR(1000) DEFAULT NULL,
  `read_count` int(11) NOT NULL DEFAULT 0,
  `write_count` int(11) NOT NULL DEFAULT 0,
  `filter_count` int(11) NOT NULL DEFAULT 0,
  `read_skip_count` int(11) NOT NULL DEFAULT 0,
  `process_skip_count` int(11) NOT NULL DEFAULT 0,
  `write_skip_count` int(11) NOT NULL DEFAULT 0,
  `error_log_count` int(11) NOT NULL DEFAULT 0 COMMENT 'tb_batch_error_log에 남은 건수',
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `duration_ms` BIGINT NOT NULL DEFAULT 0,
  `success_flag` tinyint(1) NOT NULL DEFAULT 0,
  `alert_sent_flag` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);