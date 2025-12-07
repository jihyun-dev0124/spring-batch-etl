CREATE TABLE `tb_mall_member` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `mall_id` bigint(20) NOT NULL,
  `member_id` varchar(50) NOT NULL,
  `name` varchar(50) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `status` enum('ACTIVE','DELETED') NOT NULL,
  `join_path` varchar(10) DEFAULT NULL,
  `joined_at` datetime NOT NULL,
  `created_at` datetime NOT NULL,
  `last_modified_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `member_id` (`member_id`),
  UNIQUE KEY `phone` (`phone`)
);

CREATE TABLE `tb_mall_config` (
  `mall_id` bigint(20) NOT NULL,
  `mall_name` varchar(100) DEFAULT NULL,
  `api_url` varchar(300) DEFAULT NULL COMMENT 'api_url',
  `api_id` varchar(100) DEFAULT NULL COMMENT 'api_아이디',
  `api_pw` varchar(200) DEFAULT NULL COMMENT 'api_비밀번호',
  `api_token_value` varchar(300) DEFAULT NULL COMMENT 'api_토큰_값',
  `api_refresh_token_value` varchar(300) DEFAULT NULL COMMENT 'api_리프레시_토큰_값(토큰 재발급시 사용)',
  `api_token_expire` datetime DEFAULT NULL COMMENT 'api_토큰_만료일자',
  `api_refresh_token_expire` datetime DEFAULT NULL COMMENT 'api_리프레시_토큰_만료일자',
  `api_version` varchar(300) DEFAULT NULL COMMENT 'api_version',
  `is_usable` enum('T','F') NOT NULL DEFAULT 'F' COMMENT '사용여부',
  `created_at` datetime NOT NULL,
  `last_modified_at` datetime DEFAULT NULL,
  PRIMARY KEY (`mall_id`)
);

CREATE TABLE `tb_mall_sync_state` (
  `mall_id` bigint(20) NOT NULL,
  `sync_type` varchar(20) NOT NULL COMMENT 'MEMBER, ORDER, SALES',
  `last_synced_at` datetime DEFAULT NULL COMMENT '마지막 성공 동기화 기준 시각',
  `last_status` varchar(20) NOT NULL COMMENT 'IDLE, RUNNING, SUCCESS, FAILED',
  `last_error_code` varchar(100) DEFAULT NULL,
  `last_error_msg` varchar(500) DEFAULT NULL,
  `retry_count` int(11) NOT NULL DEFAULT 0,
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