
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `article_id` bigint DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `organization_id` bigint DEFAULT NULL,
  `registration_start` datetime NOT NULL,
  `registration_end` datetime NOT NULL,
  `event_start` datetime NOT NULL,
  `event_end` datetime NOT NULL,
  `capacity` int NOT NULL,
  `confirmed_count` int NOT NULL DEFAULT '0',
  `waitlist_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `lock_version` int NOT NULL DEFAULT '0',
  `created_by` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `article_id` (`article_id`),
  KEY `organization_id` (`organization_id`),
  CONSTRAINT `activity_ibfk_1` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `activity_ibfk_2` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`id`),
  CONSTRAINT `activity_chk_1` CHECK (((`capacity` >= 0) and (`confirmed_count` >= 0) and (`confirmed_count` <= `capacity`))),
  CONSTRAINT `activity_chk_2` CHECK ((`registration_end` > `registration_start`)),
  CONSTRAINT `activity_chk_3` CHECK ((`event_end` > `event_start`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_column` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `name` varchar(100) NOT NULL,
  `category_id` bigint NOT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (`id`),
  KEY `category_id` (`category_id`),
  CONSTRAINT `activity_column_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `content_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_download` (
  `user_id` bigint NOT NULL,
  `file_id` bigint NOT NULL,
  `activity_id` bigint NOT NULL,
  `day_key` date NOT NULL,
  `version_no` int NOT NULL,
  `issued_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`,`file_id`,`activity_id`,`day_key`),
  KEY `file_id` (`file_id`),
  KEY `activity_id` (`activity_id`),
  CONSTRAINT `activity_download_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `activity_download_ibfk_2` FOREIGN KEY (`file_id`) REFERENCES `global_file` (`id`),
  CONSTRAINT `activity_download_ibfk_3` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_favorite` (
  `user_id` bigint NOT NULL,
  `activity_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`,`activity_id`),
  KEY `activity_id` (`activity_id`),
  CONSTRAINT `activity_favorite_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `activity_favorite_ibfk_2` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_file` (
  `activity_id` bigint NOT NULL,
  `file_id` bigint NOT NULL,
  `show_history` tinyint(1) NOT NULL DEFAULT '0',
  `sort_order` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`activity_id`,`file_id`),
  KEY `file_id` (`file_id`),
  CONSTRAINT `activity_file_ibfk_1` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`),
  CONSTRAINT `activity_file_ibfk_2` FOREIGN KEY (`file_id`) REFERENCES `global_file` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_form_version` (
  `id` bigint NOT NULL,
  `activity_id` bigint NOT NULL,
  `version_no` int NOT NULL,
  `schema_json` json NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `activity_id` (`activity_id`,`version_no`),
  UNIQUE KEY `activity_id_2` (`activity_id`,`id`),
  CONSTRAINT `activity_form_version_ibfk_1` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_registration` (
  `id` bigint NOT NULL,
  `activity_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `form_version_id` bigint NOT NULL,
  `answer_snapshot` json NOT NULL,
  `status` varchar(20) NOT NULL,
  `queue_seq` bigint NOT NULL,
  `submitted_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `cancelled_at` datetime DEFAULT NULL,
  `lock_version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `activity_id` (`activity_id`,`user_id`),
  KEY `activity_id_2` (`activity_id`,`form_version_id`),
  KEY `user_id` (`user_id`),
  KEY `activity_id_3` (`activity_id`,`status`,`queue_seq`),
  CONSTRAINT `activity_registration_ibfk_1` FOREIGN KEY (`activity_id`, `form_version_id`) REFERENCES `activity_form_version` (`activity_id`, `id`),
  CONSTRAINT `activity_registration_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `activity_registration_chk_1` CHECK ((`status` in (_utf8mb4'CONFIRMED',_utf8mb4'WAITLISTED',_utf8mb4'CANCELLED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_review` (
  `activity_id` bigint NOT NULL,
  `campaign_id` bigint NOT NULL,
  PRIMARY KEY (`activity_id`,`campaign_id`),
  KEY `campaign_id` (`campaign_id`),
  CONSTRAINT `activity_review_ibfk_1` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`),
  CONSTRAINT `activity_review_ibfk_2` FOREIGN KEY (`campaign_id`) REFERENCES `review_campaign` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_topic` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `title` varchar(150) NOT NULL,
  `subtitle` varchar(300) DEFAULT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `description` text,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `sort_order` int NOT NULL DEFAULT '0',
  `lock_version` int NOT NULL DEFAULT '0',
  `created_by` bigint NOT NULL,
  `updated_by` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_topic_public` (`tenant_id`,`status`,`deleted_at`,`sort_order`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_topic_article` (
  `topic_id` bigint NOT NULL,
  `article_id` bigint NOT NULL,
  `sort_order` int NOT NULL,
  PRIMARY KEY (`topic_id`,`article_id`),
  KEY `idx_topic_article` (`article_id`),
  CONSTRAINT `fk_topic_article_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `fk_topic_article_topic` FOREIGN KEY (`topic_id`) REFERENCES `activity_topic` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_navigation` (
  `route` varchar(100) NOT NULL,
  `group_name` varchar(100) NOT NULL,
  `label` varchar(100) NOT NULL,
  `group_order` int NOT NULL,
  `sort_order` int NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`route`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `analytics_daily` (
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `metric_date` date NOT NULL,
  `metric_key` varchar(80) NOT NULL,
  `dimension_hash` char(64) NOT NULL,
  `value` bigint NOT NULL,
  PRIMARY KEY (`tenant_id`,`metric_date`,`metric_key`,`dimension_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `app_user` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `nickname` varchar(100) DEFAULT NULL,
  `avatar_url` varchar(500) DEFAULT NULL,
  `avatar_object_id` bigint DEFAULT NULL,
  `phone_masked` varchar(32) DEFAULT NULL,
  `phone_hash` char(64) DEFAULT NULL,
  `organization_id` bigint DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `last_login_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_app_user_org` (`organization_id`),
  KEY `idx_app_user_status` (`tenant_id`,`status`),
  UNIQUE KEY `uk_user_phone_hash` (`tenant_id`,`phone_hash`),
  CONSTRAINT `fk_app_user_organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='C端用户';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `app_user_identity` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `provider` varchar(30) NOT NULL,
  `provider_app_id` varchar(128) NOT NULL,
  `subject_hash` char(64) NOT NULL COMMENT '带版本化密钥的 HMAC，避免裸 openid 外泄',
  `encrypted_subject` text,
  `union_subject_hash` char(64) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `key_version` int NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_identity` (`tenant_id`,`provider`,`provider_app_id`,`subject_hash`),
  KEY `idx_identity_user` (`user_id`),
  CONSTRAINT `fk_identity_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='外部身份与内部用户分离';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `title` varchar(255) NOT NULL,
  `summary` varchar(1000) DEFAULT NULL,
  `cover_url` varchar(500) DEFAULT NULL,
  `source_type` varchar(20) NOT NULL DEFAULT 'PLATFORM',
  `source_name` varchar(150) DEFAULT NULL,
  `source_url` varchar(500) DEFAULT NULL,
  `content_type` varchar(20) NOT NULL DEFAULT 'BASIC',
  `status` varchar(30) NOT NULL DEFAULT 'DRAFT',
  `visibility` varchar(20) NOT NULL DEFAULT 'PUBLIC',
  `current_version_id` bigint DEFAULT NULL COMMENT '当前发布版本',
  `latest_version_id` bigint DEFAULT NULL COMMENT '兼容残留：不做版本追加，发布仍走主表 version_no=1',
  `effective_from` datetime DEFAULT NULL,
  `effective_to` datetime DEFAULT NULL,
  `pinned` tinyint NOT NULL DEFAULT '0',
  `content_weight` tinyint NOT NULL DEFAULT '1',
  `hot_weight` tinyint NOT NULL DEFAULT '0',
  `published_at` datetime DEFAULT NULL,
  `offline_at` datetime DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_dept_id` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `version_no` int NOT NULL DEFAULT '0',
  `deleted_at` datetime DEFAULT NULL,
  `lock_version` int NOT NULL DEFAULT '0',
  `submitted_version_id` bigint DEFAULT NULL COMMENT '兼容残留：不做版本追加，发布仍走主表 version_no=1',
  `carrier_type` varchar(20) NOT NULL DEFAULT 'TEXT_IMAGE',
  `abnormal` tinyint NOT NULL DEFAULT '0',
  `hot_until` date DEFAULT NULL,
  `urgent` tinyint(1) NOT NULL DEFAULT '0',
  `lifespan` varchar(20) NOT NULL DEFAULT 'LONG',
  `push_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `taxonomy_hash` char(64) DEFAULT NULL,
  `import_batch_id` bigint DEFAULT NULL,
  `hot_archived_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_article_feed` (`tenant_id`,`status`,`published_at`,`id`),
  KEY `idx_article_effective` (`tenant_id`,`effective_from`,`effective_to`),
  KEY `idx_article_creator` (`tenant_id`,`created_by`,`created_dept_id`),
  KEY `fk_article_current_version` (`id`,`current_version_id`),
  KEY `fk_article_latest_version` (`id`,`latest_version_id`),
  KEY `fk_article_submitted_version` (`id`,`submitted_version_id`),
  KEY `idx_article_hot_active` (`tenant_id`,`status`,`hot_until`,`effective_to`),
  KEY `idx_article_taxonomy` (`tenant_id`,`taxonomy_hash`,`status`,`effective_to`),
  KEY `idx_hot_archive_pending` (`status`,`hot_archived_at`,`hot_until`),
  FULLTEXT KEY `ft_article_search` (`title`,`summary`,`source_name`),
  CONSTRAINT `fk_article_current_version` FOREIGN KEY (`id`, `current_version_id`) REFERENCES `article_version` (`article_id`, `id`),
  CONSTRAINT `fk_article_latest_version` FOREIGN KEY (`id`, `latest_version_id`) REFERENCES `article_version` (`article_id`, `id`),
  CONSTRAINT `fk_article_submitted_version` FOREIGN KEY (`id`, `submitted_version_id`) REFERENCES `article_version` (`article_id`, `id`),
  CONSTRAINT `chk_article_abnormal` CHECK ((`abnormal` in (0,1))),
  CONSTRAINT `chk_article_carrier` CHECK ((`carrier_type` in (_utf8mb4'TEXT_IMAGE',_utf8mb4'LONG_IMAGE',_utf8mb4'FILE'))),
  CONSTRAINT `chk_article_effective` CHECK (((`effective_to` is null) or (`effective_from` is null) or (`effective_to` > `effective_from`))),
  CONSTRAINT `chk_article_status` CHECK ((`status` in (_utf8mb4'DRAFT',_utf8mb4'PENDING_REVIEW',_utf8mb4'REJECTED',_utf8mb4'PUBLISHED',_utf8mb4'OFFLINE'))),
  CONSTRAINT `chk_article_weights` CHECK (((`pinned` in (0,1)) and (`content_weight` between 1 and 5) and (`hot_weight` between 0 and 30)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资讯主表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_category_rel` (
  `article_id` bigint NOT NULL,
  `category_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`article_id`,`category_id`),
  KEY `idx_article_category_category` (`category_id`,`article_id`),
  CONSTRAINT `fk_ac_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `fk_ac_category` FOREIGN KEY (`category_id`) REFERENCES `content_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资讯-分类关系';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_feed_snapshot` (
  `token_hash` char(64) NOT NULL,
  `tenant_id` varchar(20) NOT NULL,
  `query_hash` char(64) NOT NULL,
  `issued_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `cards_json` longtext NOT NULL,
  PRIMARY KEY (`token_hash`),
  KEY `idx_feed_snapshot_expiry` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_import_batch` (
  `id` bigint NOT NULL,
  `staff_id` bigint NOT NULL,
  `request_key` varchar(128) NOT NULL,
  `payload_hash` char(64) NOT NULL,
  `payload` json NOT NULL,
  `total` int NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PROCESSING',
  `lease_token` varchar(64) DEFAULT NULL,
  `lease_until` datetime DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `completed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `staff_id` (`staff_id`,`request_key`),
  CONSTRAINT `article_import_batch_chk_1` CHECK ((`status` in (_utf8mb4'PROCESSING',_utf8mb4'PARTIAL',_utf8mb4'COMPLETED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_import_item` (
  `batch_id` bigint NOT NULL,
  `row_index` int NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `article_id` bigint DEFAULT NULL,
  `error_message` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`batch_id`,`row_index`),
  KEY `article_id` (`article_id`),
  CONSTRAINT `article_import_item_ibfk_1` FOREIGN KEY (`batch_id`) REFERENCES `article_import_batch` (`id`),
  CONSTRAINT `article_import_item_ibfk_2` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_keyword_rel` (
  `article_id` bigint NOT NULL,
  `keyword_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`article_id`,`keyword_id`),
  KEY `idx_article_keyword_keyword` (`keyword_id`,`article_id`),
  CONSTRAINT `fk_ak_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `fk_ak_keyword` FOREIGN KEY (`keyword_id`) REFERENCES `keyword` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资讯-关键词关系';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_label_rel` (
  `article_id` bigint NOT NULL,
  `label_id` bigint NOT NULL,
  PRIMARY KEY (`article_id`,`label_id`),
  KEY `label_id` (`label_id`),
  CONSTRAINT `article_label_rel_ibfk_1` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `article_label_rel_ibfk_2` FOREIGN KEY (`label_id`) REFERENCES `content_label` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_organization_rel` (
  `article_id` bigint NOT NULL,
  `organization_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`article_id`,`organization_id`),
  KEY `idx_article_org_org` (`organization_id`,`article_id`),
  CONSTRAINT `fk_ao_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `fk_ao_organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='云迹 2-09 已停用组织维度，仅为外键兼容保留，不做业务写入';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_replacement` (
  `id` bigint NOT NULL,
  `old_article_id` bigint NOT NULL,
  `new_article_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `relation_snapshot` json DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `resolved_by` bigint DEFAULT NULL,
  `resolved_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_replacement` (`old_article_id`,`new_article_id`),
  KEY `idx_replacement_status` (`status`,`created_at`),
  KEY `fk_replacement_new` (`new_article_id`),
  CONSTRAINT `fk_replacement_new` FOREIGN KEY (`new_article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `fk_replacement_old` FOREIGN KEY (`old_article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `chk_replacement_self` CHECK ((`old_article_id` <> `new_article_id`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资讯替换关系';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_replacement_candidate` (
  `id` bigint NOT NULL,
  `old_article_id` bigint NOT NULL,
  `new_article_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `resolved_by` bigint DEFAULT NULL,
  `resolved_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `old_article_id` (`old_article_id`,`new_article_id`),
  KEY `new_article_id` (`new_article_id`),
  CONSTRAINT `article_replacement_candidate_ibfk_1` FOREIGN KEY (`old_article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `article_replacement_candidate_ibfk_2` FOREIGN KEY (`new_article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `article_replacement_candidate_chk_1` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'CONFIRMED',_utf8mb4'IGNORED',_utf8mb4'OBSOLETE')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_replacement_guard` (
  `tenant_id` varchar(20) NOT NULL,
  PRIMARY KEY (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_review` (
  `id` bigint NOT NULL,
  `article_id` bigint NOT NULL,
  `version_id` bigint NOT NULL,
  `action` varchar(20) NOT NULL COMMENT 'SUBMIT/APPROVE/REJECT/PUBLISH/OFFLINE',
  `comment` varchar(1000) DEFAULT NULL,
  `operator_id` bigint NOT NULL,
  `operator_dept_id` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `request_id` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_review_article` (`article_id`,`created_at`),
  KEY `idx_review_operator` (`operator_id`,`created_at`),
  KEY `fk_review_version` (`article_id`,`version_id`),
  CONSTRAINT `fk_review_version` FOREIGN KEY (`article_id`, `version_id`) REFERENCES `article_version` (`article_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审核与发布记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_version` (
  `id` bigint NOT NULL,
  `article_id` bigint NOT NULL,
  `version_no` int NOT NULL,
  `title` varchar(255) NOT NULL,
  `summary` varchar(1000) DEFAULT NULL,
  `cover_url` varchar(500) DEFAULT NULL,
  `source_name` varchar(150) DEFAULT NULL,
  `source_url` varchar(500) DEFAULT NULL,
  `content_type` varchar(20) NOT NULL DEFAULT 'BASIC',
  `effective_from` datetime DEFAULT NULL,
  `effective_to` datetime DEFAULT NULL,
  `rich_text` longtext,
  `relation_snapshot` json NOT NULL COMMENT '该版本分类/组织/关键词快照',
  `change_summary` varchar(500) DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `review_state` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT '兼容残留：不做版本追加，发布仍走主表 version_no=1',
  `submitted_at` datetime DEFAULT NULL,
  `decided_at` datetime DEFAULT NULL,
  `pinned` tinyint NOT NULL DEFAULT '0',
  `content_weight` tinyint NOT NULL DEFAULT '1',
  `hot_weight` tinyint NOT NULL DEFAULT '0',
  `interpretation` longtext,
  `original_text` longtext,
  `carrier_type` varchar(20) NOT NULL DEFAULT 'TEXT_IMAGE',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_version` (`article_id`,`version_no`),
  UNIQUE KEY `uk_article_version_owner` (`article_id`,`id`),
  KEY `idx_article_version_article` (`article_id`,`created_at`),
  CONSTRAINT `fk_version_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `chk_article_version_weights` CHECK (((`pinned` in (0,1)) and (`content_weight` between 1 and 5) and (`hot_weight` between 0 and 30))),
  CONSTRAINT `chk_version_carrier` CHECK ((`carrier_type` in (_utf8mb4'TEXT_IMAGE',_utf8mb4'LONG_IMAGE',_utf8mb4'FILE'))),
  CONSTRAINT `chk_version_review` CHECK ((`review_state` in (_utf8mb4'DRAFT',_utf8mb4'SUBMITTED',_utf8mb4'APPROVED',_utf8mb4'REJECTED',_utf8mb4'WITHDRAWN')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='不可变版本，发布后投影到主表和关联索引';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_event` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `actor_id` bigint DEFAULT NULL,
  `actor_client` varchar(40) DEFAULT NULL,
  `action` varchar(50) NOT NULL,
  `object_type` varchar(50) NOT NULL,
  `object_id` bigint DEFAULT NULL,
  `before_json` json DEFAULT NULL,
  `after_json` json DEFAULT NULL,
  `request_id` varchar(64) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_object` (`object_type`,`object_id`,`created_at`),
  KEY `idx_audit_actor` (`actor_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审计事件';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `banner` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `title` varchar(150) NOT NULL,
  `image_url` varchar(500) NOT NULL,
  `link_type` varchar(20) NOT NULL DEFAULT 'ARTICLE',
  `link_value` varchar(500) DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `effective_from` datetime DEFAULT NULL,
  `effective_to` datetime DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_banner_feed` (`tenant_id`,`status`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='首页轮播';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `behavior_event` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `user_id` bigint DEFAULT NULL,
  `session_id` varchar(64) DEFAULT NULL,
  `event_type` varchar(40) NOT NULL,
  `object_type` varchar(40) DEFAULT NULL,
  `object_id` bigint DEFAULT NULL,
  `payload` json DEFAULT NULL,
  `occurred_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `client_event_id` varchar(64) DEFAULT NULL,
  `ingested_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_principal` (`user_id`,`client_event_id`),
  KEY `idx_behavior_user_time` (`user_id`,`occurred_at`),
  KEY `idx_behavior_object_time` (`object_type`,`object_id`,`occurred_at`),
  KEY `idx_behavior_type_time` (`event_type`,`occurred_at`),
  KEY `idx_behavior_content_type_user` (`object_id`,`event_type`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一行为事件';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `content_category` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `parent_id` bigint NOT NULL DEFAULT '0',
  `code` varchar(64) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  `lock_version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_code` (`tenant_id`,`code`),
  UNIQUE KEY `uk_category_parent_name` (`tenant_id`,`parent_id`,`name`),
  KEY `idx_category_parent` (`tenant_id`,`parent_id`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容分类树';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `content_label` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `category_id` bigint NOT NULL,
  `parent_id` bigint DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED',
  `sort_order` int NOT NULL DEFAULT '0',
  `lock_version` int NOT NULL DEFAULT '0',
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `category_id` (`category_id`),
  KEY `parent_id` (`parent_id`),
  CONSTRAINT `content_label_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `content_category` (`id`),
  CONSTRAINT `content_label_ibfk_2` FOREIGN KEY (`parent_id`) REFERENCES `content_label` (`id`),
  CONSTRAINT `chk_label_status` CHECK ((`status` in (_utf8mb4'ENABLED',_utf8mb4'DISABLED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `content_page` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `page_key` varchar(40) NOT NULL,
  `title` varchar(150) NOT NULL,
  `content` longtext NOT NULL,
  `version_no` int NOT NULL DEFAULT '1',
  `status` varchar(20) NOT NULL DEFAULT 'PUBLISHED',
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_content_page_key` (`tenant_id`,`page_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='协议和静态内容';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `content_page_version` (
  `id` bigint NOT NULL,
  `page_id` bigint NOT NULL,
  `version_no` int NOT NULL,
  `title` varchar(150) NOT NULL,
  `content` longtext NOT NULL,
  `published_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `page_id` (`page_id`,`version_no`),
  CONSTRAINT `content_page_version_ibfk_1` FOREIGN KEY (`page_id`) REFERENCES `content_page` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `content_type_definition` (
  `code` varchar(20) NOT NULL,
  `name` varchar(100) NOT NULL,
  `renderer` varchar(20) NOT NULL,
  `require_original` tinyint(1) NOT NULL,
  `require_body` tinyint(1) NOT NULL,
  `require_summary` tinyint(1) NOT NULL,
  `require_image` tinyint(1) NOT NULL,
  `require_dates` tinyint(1) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `lock_version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`code`),
  CONSTRAINT `content_type_definition_chk_1` CHECK ((`renderer` in (_utf8mb4'TEXT',_utf8mb4'NOTICE',_utf8mb4'LONG_IMAGE')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer_search_history` (
  `user_id` bigint NOT NULL,
  `query` varchar(100) NOT NULL,
  `searched_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`,`query`),
  CONSTRAINT `customer_search_history_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer_search_log` (
  `id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `query` varchar(100) NOT NULL,
  `searched_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `result_count` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_search_query` (`query`,`searched_at`),
  KEY `idx_search_user` (`user_id`,`searched_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer_session` (
  `token_hash` char(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `expires_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`token_hash`),
  KEY `user_id` (`user_id`,`expires_at`),
  CONSTRAINT `customer_session_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `download_event` (
  `id` bigint NOT NULL,
  `token_hash` char(64) NOT NULL,
  `event_type` varchar(30) NOT NULL,
  `received_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `token_hash` (`token_hash`),
  CONSTRAINT `download_event_ibfk_1` FOREIGN KEY (`token_hash`) REFERENCES `download_ticket` (`token_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `download_ticket` (
  `token_hash` char(64) NOT NULL,
  `file_id` bigint NOT NULL,
  `principal_realm` varchar(20) NOT NULL,
  `principal_id` bigint NOT NULL,
  `expires_at` datetime NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ISSUED',
  `issued_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `library_activity_id` bigint DEFAULT NULL,
  `library_file_id` bigint DEFAULT NULL,
  `library_version` int DEFAULT NULL,
  PRIMARY KEY (`token_hash`),
  KEY `file_id` (`file_id`,`expires_at`),
  CONSTRAINT `download_ticket_ibfk_1` FOREIGN KEY (`file_id`) REFERENCES `file_object` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `event_consumer_receipt` (
  `consumer_name` varchar(80) NOT NULL,
  `event_id` bigint NOT NULL,
  `processed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`consumer_name`,`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消费者去重收据';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exposure_receipt` (
  `event_id` char(36) NOT NULL,
  `user_id` bigint NOT NULL,
  `article_id` bigint NOT NULL,
  `received_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `request_hash` char(64) DEFAULT NULL,
  PRIMARY KEY (`event_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户端可见曝光去重';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `feedback` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `user_id` bigint NOT NULL,
  `type` varchar(30) NOT NULL DEFAULT 'GENERAL',
  `description` varchar(2000) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `handler_id` bigint DEFAULT NULL,
  `handled_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_feedback_status` (`tenant_id`,`status`,`created_at`),
  KEY `fk_feedback_user` (`user_id`),
  CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户反馈';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `file_object` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `owner_realm` varchar(20) NOT NULL,
  `owner_id` bigint NOT NULL,
  `object_key` varchar(100) NOT NULL,
  `sha256` char(64) NOT NULL,
  `size_bytes` bigint NOT NULL,
  `media_type` varchar(100) NOT NULL,
  `display_name` varchar(150) NOT NULL,
  `privacy` varchar(20) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'VERIFIED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  `purged_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `object_key` (`object_key`),
  UNIQUE KEY `owner_realm` (`owner_realm`,`owner_id`,`sha256`,`privacy`),
  KEY `idx_file_cleanup` (`status`,`deleted_at`,`purged_at`),
  CONSTRAINT `file_object_chk_1` CHECK ((`privacy` in (_utf8mb4'PUBLIC',_utf8mb4'PRIVATE'))),
  CONSTRAINT `file_object_chk_2` CHECK ((`size_bytes` between 1 and 10485760))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `file_reference` (
  `file_id` bigint NOT NULL,
  `owner_type` varchar(30) NOT NULL,
  `owner_id` bigint NOT NULL,
  `reference_version_id` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`file_id`,`owner_type`,`owner_id`,`reference_version_id`),
  CONSTRAINT `file_reference_ibfk_1` FOREIGN KEY (`file_id`) REFERENCES `file_object` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_table` (
  `table_id` bigint NOT NULL COMMENT '编号',
  `data_name` varchar(200) DEFAULT '' COMMENT '数据源名称',
  `table_name` varchar(200) DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) DEFAULT '' COMMENT '表描述',
  `sub_table_name` varchar(64) DEFAULT NULL COMMENT '关联子表的表名',
  `sub_table_fk_name` varchar(64) DEFAULT NULL COMMENT '子表关联的外键名',
  `class_name` varchar(100) DEFAULT '' COMMENT '实体类名称',
  `tpl_category` varchar(200) DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
  `package_name` varchar(100) DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(30) DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(30) DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(50) DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50) DEFAULT NULL COMMENT '生成功能作者',
  `gen_type` char(1) DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  `gen_path` varchar(200) DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
  `options` varchar(1000) DEFAULT NULL COMMENT '其它生成选项',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成业务表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `gen_table_column` (
  `column_id` bigint NOT NULL COMMENT '编号',
  `table_id` bigint DEFAULT NULL COMMENT '归属表编号',
  `column_name` varchar(200) DEFAULT NULL COMMENT '列名称',
  `column_comment` varchar(500) DEFAULT NULL COMMENT '列描述',
  `column_type` varchar(100) DEFAULT NULL COMMENT '列类型',
  `java_type` varchar(500) DEFAULT NULL COMMENT 'JAVA类型',
  `java_field` varchar(200) DEFAULT NULL COMMENT 'JAVA字段名',
  `is_pk` char(1) DEFAULT NULL COMMENT '是否主键（1是）',
  `is_increment` char(1) DEFAULT NULL COMMENT '是否自增（1是）',
  `is_required` char(1) DEFAULT NULL COMMENT '是否必填（1是）',
  `is_insert` char(1) DEFAULT NULL COMMENT '是否为插入字段（1是）',
  `is_edit` char(1) DEFAULT NULL COMMENT '是否编辑字段（1是）',
  `is_list` char(1) DEFAULT NULL COMMENT '是否列表字段（1是）',
  `is_query` char(1) DEFAULT NULL COMMENT '是否查询字段（1是）',
  `query_type` varchar(200) DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
  `html_type` varchar(200) DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  `dict_type` varchar(200) DEFAULT '' COMMENT '字典类型',
  `sort` int DEFAULT NULL COMMENT '排序',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`column_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='代码生成业务表字段';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `global_file` (
  `id` bigint NOT NULL,
  `name` varchar(150) NOT NULL,
  `description` varchar(2000) NOT NULL DEFAULT '',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `deleted_at` datetime DEFAULT NULL,
  `current_version` int NOT NULL DEFAULT '0',
  `lock_version` int NOT NULL DEFAULT '0',
  `created_by` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `global_file_version` (
  `file_id` bigint NOT NULL,
  `version_no` int NOT NULL,
  `object_id` bigint NOT NULL,
  `provider` varchar(20) NOT NULL,
  `cloud_file_id` varchar(1000) DEFAULT NULL,
  `note` varchar(500) NOT NULL DEFAULT '',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`file_id`,`version_no`),
  KEY `object_id` (`object_id`),
  CONSTRAINT `global_file_version_ibfk_1` FOREIGN KEY (`file_id`) REFERENCES `global_file` (`id`),
  CONSTRAINT `global_file_version_ibfk_2` FOREIGN KEY (`object_id`) REFERENCES `file_object` (`id`),
  CONSTRAINT `global_file_version_chk_1` CHECK ((`provider` in (_utf8mb4'LOCAL',_utf8mb4'WECHAT_CLOUD')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `home_recommendation` (
  `id` bigint NOT NULL,
  `article_id` bigint NOT NULL,
  `cover_url` varchar(500) DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `effective_from` datetime DEFAULT NULL,
  `effective_to` datetime DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_home_recommendation` (`status`,`sort_order`,`effective_from`,`effective_to`),
  KEY `fk_home_article` (`article_id`),
  CONSTRAINT `fk_home_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='首页推荐位';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `idempotency_record` (
  `principal_key` varchar(150) NOT NULL,
  `route_key` varchar(150) NOT NULL,
  `idempotency_key` varchar(128) NOT NULL,
  `request_hash` char(64) NOT NULL,
  `response_status` smallint DEFAULT NULL,
  `response_payload` json DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'IN_PROGRESS',
  `expires_at` datetime NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`principal_key`,`route_key`,`idempotency_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='有副作用请求的幂等记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `keyword` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `display_name` varchar(100) NOT NULL,
  `normalized_name` varchar(100) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED',
  `sort_order` int NOT NULL DEFAULT '0',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  `lock_version` int NOT NULL DEFAULT '0',
  `import_batch_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_keyword_normalized` (`tenant_id`,`normalized_name`),
  KEY `idx_keyword_name` (`tenant_id`,`display_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='可订阅关键词';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `keyword_alias` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `normalized_alias` varchar(100) NOT NULL,
  `keyword_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `tenant_id` (`tenant_id`,`normalized_alias`),
  KEY `keyword_id` (`keyword_id`),
  CONSTRAINT `keyword_alias_ibfk_1` FOREIGN KEY (`keyword_id`) REFERENCES `keyword` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `keyword_blacklist` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `normalized_name` varchar(100) NOT NULL,
  `reason` varchar(500) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `tenant_id` (`tenant_id`,`normalized_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `label_keyword_curation` (
  `label_id` bigint NOT NULL,
  `keyword_id` bigint NOT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`label_id`,`keyword_id`),
  KEY `keyword_id` (`keyword_id`),
  CONSTRAINT `label_keyword_curation_ibfk_1` FOREIGN KEY (`label_id`) REFERENCES `content_label` (`id`),
  CONSTRAINT `label_keyword_curation_ibfk_2` FOREIGN KEY (`keyword_id`) REFERENCES `keyword` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `local_login_code` (
  `code_hash` char(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `app_id` varchar(100) NOT NULL,
  `expires_at` datetime NOT NULL,
  `used_at` datetime DEFAULT NULL,
  PRIMARY KEY (`code_hash`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `local_login_code_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `local_notification_outcome` (
  `user_id` bigint NOT NULL,
  `outcome` varchar(40) NOT NULL,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `local_notification_outcome_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `migration_id_map` (
  `batch_key` varchar(80) NOT NULL,
  `source_table` varchar(100) NOT NULL,
  `source_id` varchar(128) NOT NULL,
  `target_table` varchar(100) NOT NULL,
  `target_id` bigint DEFAULT NULL,
  `resolution` varchar(30) NOT NULL DEFAULT 'PENDING',
  `reason` varchar(1000) DEFAULT NULL,
  `source_hash` char(64) DEFAULT NULL,
  PRIMARY KEY (`batch_key`,`source_table`,`source_id`,`target_table`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='迁移映射和隔离原因';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_attempt` (
  `id` bigint NOT NULL,
  `delivery_id` bigint NOT NULL,
  `attempt_no` int NOT NULL,
  `request_hash` char(64) NOT NULL,
  `outcome` varchar(40) NOT NULL,
  `provider_code` varchar(100) DEFAULT NULL,
  `started_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `finished_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `delivery_id` (`delivery_id`,`attempt_no`),
  CONSTRAINT `notification_attempt_ibfk_1` FOREIGN KEY (`delivery_id`) REFERENCES `notification_delivery` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_binding` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `provider` varchar(30) NOT NULL,
  `app_id` varchar(128) NOT NULL,
  `subject_hash` char(64) NOT NULL,
  `binding_state` varchar(20) NOT NULL,
  `key_version` int NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `provider` (`provider`,`app_id`,`subject_hash`),
  UNIQUE KEY `user_id` (`user_id`,`provider`,`app_id`),
  CONSTRAINT `notification_binding_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_callback_receipt` (
  `provider` varchar(30) NOT NULL,
  `callback_key` varchar(100) NOT NULL,
  `payload_hash` char(64) NOT NULL,
  `handled_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`provider`,`callback_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_delivery` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `article_id` bigint DEFAULT NULL,
  `channel` varchar(20) NOT NULL,
  `template_key` varchar(100) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `provider_message_id` varchar(150) DEFAULT NULL,
  `error_code` varchar(80) DEFAULT NULL,
  `sent_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `rule_id` bigint DEFAULT NULL,
  `rule_version` int NOT NULL DEFAULT '0',
  `dedupe_key` varchar(150) DEFAULT NULL,
  `article_version_id` bigint DEFAULT NULL,
  `attempts` int NOT NULL DEFAULT '0',
  `next_attempt_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lease_until` datetime DEFAULT NULL,
  `quota_reserved` tinyint(1) NOT NULL DEFAULT '0',
  `rule_snapshot` json DEFAULT NULL,
  `content_snapshot` json DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_delivery_dedupe` (`dedupe_key`),
  KEY `idx_delivery_user_time` (`user_id`,`created_at`),
  KEY `idx_delivery_status` (`status`,`created_at`),
  KEY `fk_delivery_article` (`article_id`),
  KEY `idx_delivery_due` (`status`,`next_attempt_at`),
  KEY `rule_id` (`rule_id`),
  CONSTRAINT `fk_delivery_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `fk_delivery_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `notification_delivery_ibfk_1` FOREIGN KEY (`rule_id`) REFERENCES `notification_rule` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='提醒发送记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_quota` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `app_id` varchar(128) NOT NULL,
  `channel` varchar(30) NOT NULL,
  `template_key` varchar(100) NOT NULL,
  `available` int NOT NULL DEFAULT '0',
  `reserved` int NOT NULL DEFAULT '0',
  `consumed` int NOT NULL DEFAULT '0',
  `lock_version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`,`app_id`,`channel`,`template_key`),
  CONSTRAINT `notification_quota_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `notification_quota_chk_1` CHECK (((`available` >= 0) and (`reserved` >= 0) and (`consumed` >= 0)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_quota_ledger` (
  `id` bigint NOT NULL,
  `quota_id` bigint NOT NULL,
  `delivery_id` bigint NOT NULL,
  `operation` varchar(20) NOT NULL,
  `amount` int NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `delivery_id` (`delivery_id`,`operation`),
  KEY `quota_id` (`quota_id`),
  CONSTRAINT `notification_quota_ledger_ibfk_1` FOREIGN KEY (`quota_id`) REFERENCES `notification_quota` (`id`),
  CONSTRAINT `notification_quota_ledger_ibfk_2` FOREIGN KEY (`delivery_id`) REFERENCES `notification_delivery` (`id`),
  CONSTRAINT `notification_quota_ledger_chk_1` CHECK ((`operation` in (_utf8mb4'RESERVE',_utf8mb4'COMMIT',_utf8mb4'RELEASE')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_rule` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `channel` varchar(20) NOT NULL COMMENT 'WECHAT_SUBSCRIBE/WECom',
  `enabled` tinyint NOT NULL DEFAULT '1',
  `quiet_start` time DEFAULT NULL,
  `quiet_end` time DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `target_type` varchar(20) NOT NULL DEFAULT 'ARTICLE',
  `target_id` bigint NOT NULL DEFAULT '0',
  `mode` varchar(20) NOT NULL DEFAULT 'INSTANT',
  `timezone` varchar(64) NOT NULL DEFAULT 'Asia/Shanghai',
  `scheduled_at` datetime DEFAULT NULL,
  `local_send_time` time DEFAULT NULL,
  `rule_hash` char(64) NOT NULL,
  `lock_version` int NOT NULL DEFAULT '0',
  `next_run_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_user_hash` (`user_id`,`rule_hash`),
  KEY `idx_rule_schedule` (`enabled`,`next_run_at`),
  CONSTRAINT `fk_notification_rule_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `chk_notification_mode` CHECK ((`mode` in (_utf8mb4'AT_TIME',_utf8mb4'DAILY',_utf8mb4'INSTANT')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='提醒规则扩展表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `organization` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `parent_id` bigint NOT NULL DEFAULT '0',
  `type` varchar(20) NOT NULL COMMENT 'SCHOOL/COLLEGE/DEPARTMENT/PLATFORM',
  `code` varchar(64) DEFAULT NULL,
  `name` varchar(150) NOT NULL,
  `short_name` varchar(80) DEFAULT NULL,
  `logo_url` varchar(500) DEFAULT NULL,
  `description` varchar(1000) DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  `lock_version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_org_parent_name` (`tenant_id`,`parent_id`,`name`),
  UNIQUE KEY `uk_org_code` (`tenant_id`,`code`),
  KEY `idx_org_parent_type` (`tenant_id`,`parent_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='云迹 2-09 已停用组织维度，仅为外键兼容保留，不做业务写入';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `outbox_event` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `aggregate_type` varchar(40) NOT NULL,
  `aggregate_id` bigint NOT NULL,
  `event_type` varchar(50) NOT NULL,
  `payload` json NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `attempts` int NOT NULL DEFAULT '0',
  `available_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lease_until` datetime DEFAULT NULL,
  `processed_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_outbox_pending` (`status`,`available_at`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='事务事件待处理队列';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reminder_attempt` (
  `id` bigint NOT NULL,
  `batch_id` bigint NOT NULL,
  `attempt_no` int NOT NULL,
  `outcome` varchar(30) NOT NULL,
  `evidence` varchar(1000) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `batch_id` (`batch_id`,`attempt_no`),
  CONSTRAINT `reminder_attempt_ibfk_1` FOREIGN KEY (`batch_id`) REFERENCES `reminder_batch` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reminder_batch` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `channel` varchar(30) NOT NULL,
  `state` varchar(30) NOT NULL DEFAULT 'PENDING',
  `snapshot` json NOT NULL,
  `attempts` int NOT NULL DEFAULT '0',
  `provider_task_id` varchar(150) DEFAULT NULL,
  `error_code` varchar(100) DEFAULT NULL,
  `lease_token` varchar(64) DEFAULT NULL,
  `lease_until` datetime DEFAULT NULL,
  `next_attempt_at` datetime NOT NULL DEFAULT (UTC_TIMESTAMP()),
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `completed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `idx_batch_state` (`state`,`next_attempt_at`),
  CONSTRAINT `reminder_batch_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `reminder_batch_chk_1` CHECK ((`channel` in (_utf8mb4'WECom',_utf8mb4'WECHAT_SUBSCRIBE'))),
  CONSTRAINT `reminder_batch_chk_2` CHECK ((`state` in (_utf8mb4'PENDING',_utf8mb4'WAITING_AUTH',_utf8mb4'SENDING',_utf8mb4'AWAITING_STAFF',_utf8mb4'PARTIAL',_utf8mb4'ACCEPTED',_utf8mb4'SENT',_utf8mb4'FAILED',_utf8mb4'UNKNOWN',_utf8mb4'CANCELLED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reminder_batch_item` (
  `batch_id` bigint NOT NULL,
  `change_id` bigint NOT NULL,
  PRIMARY KEY (`batch_id`,`change_id`),
  UNIQUE KEY `change_id` (`change_id`),
  CONSTRAINT `reminder_batch_item_ibfk_1` FOREIGN KEY (`batch_id`) REFERENCES `reminder_batch` (`id`),
  CONSTRAINT `reminder_batch_item_ibfk_2` FOREIGN KEY (`change_id`) REFERENCES `reminder_change` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reminder_change` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `article_id` bigint NOT NULL,
  `version_id` bigint NOT NULL,
  `event_type` varchar(30) NOT NULL,
  `state` varchar(30) NOT NULL DEFAULT 'PENDING',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`,`article_id`,`version_id`),
  KEY `idx_change_user_state` (`user_id`,`state`),
  KEY `article_id` (`article_id`),
  KEY `version_id` (`version_id`),
  CONSTRAINT `reminder_change_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `reminder_change_ibfk_2` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `reminder_change_ibfk_3` FOREIGN KEY (`version_id`) REFERENCES `article_version` (`id`),
  CONSTRAINT `reminder_change_chk_1` CHECK ((`state` in (_utf8mb4'PENDING',_utf8mb4'BATCHED',_utf8mb4'SENT',_utf8mb4'READ',_utf8mb4'DISMISSED',_utf8mb4'SUPERSEDED',_utf8mb4'CANCELLED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reminder_control` (
  `id` int NOT NULL,
  `daily_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `instant_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `timezone` varchar(64) NOT NULL DEFAULT 'Asia/Shanghai',
  `slots` json NOT NULL,
  `daily_template` varchar(128) NOT NULL DEFAULT '',
  `instant_template` varchar(128) NOT NULL DEFAULT '',
  `lock_version` int NOT NULL DEFAULT '0',
  `plan_after_user` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reminder_local_receipt` (
  `receipt_key` varchar(100) NOT NULL,
  `payload_hash` char(64) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`receipt_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reminder_match` (
  `change_id` bigint NOT NULL,
  `tracking_id` bigint NOT NULL,
  `tracking_version` int NOT NULL,
  PRIMARY KEY (`change_id`,`tracking_id`),
  KEY `tracking_id` (`tracking_id`),
  CONSTRAINT `reminder_match_ibfk_1` FOREIGN KEY (`change_id`) REFERENCES `reminder_change` (`id`),
  CONSTRAINT `reminder_match_ibfk_2` FOREIGN KEY (`tracking_id`) REFERENCES `reminder_tracking` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reminder_permission` (
  `user_id` bigint NOT NULL,
  `wecom_bound` tinyint(1) NOT NULL DEFAULT '0',
  `available` int NOT NULL DEFAULT '0',
  `wecom_external_userid` varchar(64) DEFAULT NULL,
  `simulation_outcome` varchar(30) NOT NULL DEFAULT 'SUCCESS',
  PRIMARY KEY (`user_id`),
  CONSTRAINT `reminder_permission_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reminder_preference` (
  `user_id` bigint NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `slot` time NOT NULL DEFAULT '08:00:00',
  `last_batch_date` date DEFAULT NULL,
  `lock_version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`user_id`),
  CONSTRAINT `reminder_preference_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reminder_tracking` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `target_type` varchar(20) NOT NULL,
  `target_id` bigint NOT NULL,
  `mode` varchar(20) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `lock_version` int NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`,`target_type`,`target_id`),
  CONSTRAINT `reminder_tracking_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `reminder_tracking_chk_1` CHECK ((`target_type` in (_utf8mb4'ARTICLE',_utf8mb4'KEYWORD'))),
  CONSTRAINT `reminder_tracking_chk_2` CHECK ((`mode` in (_utf8mb4'DAILY',_utf8mb4'INSTANT')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_application` (
  `id` bigint NOT NULL,
  `campaign_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `current_submission_id` bigint DEFAULT NULL,
  `latest_submission_id` bigint DEFAULT NULL,
  `lock_version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `campaign_id` (`campaign_id`,`user_id`),
  KEY `user_id` (`user_id`),
  KEY `id` (`id`,`current_submission_id`),
  KEY `id_2` (`id`,`latest_submission_id`),
  CONSTRAINT `review_application_ibfk_1` FOREIGN KEY (`campaign_id`) REFERENCES `review_campaign` (`id`),
  CONSTRAINT `review_application_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `review_application_ibfk_3` FOREIGN KEY (`id`, `current_submission_id`) REFERENCES `review_submission` (`application_id`, `id`),
  CONSTRAINT `review_application_ibfk_4` FOREIGN KEY (`id`, `latest_submission_id`) REFERENCES `review_submission` (`application_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_campaign` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `organization_id` bigint DEFAULT NULL,
  `article_id` bigint DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `open_at` datetime NOT NULL,
  `close_at` datetime NOT NULL,
  `rule_version` int NOT NULL DEFAULT '1',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `created_by` bigint NOT NULL,
  `lock_version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `organization_id` (`organization_id`),
  KEY `article_id` (`article_id`),
  CONSTRAINT `review_campaign_ibfk_1` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`id`),
  CONSTRAINT `review_campaign_ibfk_2` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `review_campaign_chk_1` CHECK ((`close_at` > `open_at`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_decision` (
  `id` bigint NOT NULL,
  `application_id` bigint NOT NULL,
  `submission_id` bigint NOT NULL,
  `decision` varchar(30) NOT NULL,
  `comment` varchar(1000) NOT NULL,
  `reviewer_id` bigint NOT NULL,
  `decided_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `application_id` (`application_id`,`submission_id`),
  CONSTRAINT `review_decision_ibfk_1` FOREIGN KEY (`application_id`, `submission_id`) REFERENCES `review_submission` (`application_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_requirement` (
  `campaign_id` bigint NOT NULL,
  `rule_version` int NOT NULL,
  `requirement_key` varchar(64) NOT NULL,
  `label` varchar(150) NOT NULL,
  `required` tinyint(1) NOT NULL,
  `file_type_allowlist` json NOT NULL,
  `max_size` bigint NOT NULL,
  `eligibility_json` json NOT NULL,
  PRIMARY KEY (`campaign_id`,`rule_version`,`requirement_key`),
  CONSTRAINT `review_requirement_ibfk_1` FOREIGN KEY (`campaign_id`) REFERENCES `review_campaign` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_submission` (
  `id` bigint NOT NULL,
  `application_id` bigint NOT NULL,
  `version_no` int NOT NULL,
  `requirement_snapshot` json NOT NULL,
  `eligibility_snapshot` json NOT NULL,
  `submitted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `application_id` (`application_id`,`version_no`),
  UNIQUE KEY `application_id_2` (`application_id`,`id`),
  CONSTRAINT `review_submission_ibfk_1` FOREIGN KEY (`application_id`) REFERENCES `review_application` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_submission_file` (
  `submission_id` bigint NOT NULL,
  `requirement_key` varchar(64) NOT NULL,
  `file_id` bigint NOT NULL,
  PRIMARY KEY (`submission_id`,`requirement_key`),
  KEY `file_id` (`file_id`),
  CONSTRAINT `review_submission_file_ibfk_1` FOREIGN KEY (`submission_id`) REFERENCES `review_submission` (`id`),
  CONSTRAINT `review_submission_file_ibfk_2` FOREIGN KEY (`file_id`) REFERENCES `file_object` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reviewer_scope` (
  `user_id` bigint NOT NULL,
  `organization_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`organization_id`),
  KEY `organization_id` (`organization_id`),
  CONSTRAINT `reviewer_scope_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`),
  CONSTRAINT `reviewer_scope_ibfk_2` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schema_history` (
  `version` varchar(80) NOT NULL,
  `sha256` char(64) NOT NULL,
  `installed_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `staff_scope` (
  `user_id` bigint NOT NULL,
  `department_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`department_id`),
  KEY `department_id` (`department_id`),
  CONSTRAINT `staff_scope_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`),
  CONSTRAINT `staff_scope_ibfk_2` FOREIGN KEY (`department_id`) REFERENCES `sys_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_dimension` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `kind` varchar(20) NOT NULL,
  `code` varchar(64) NOT NULL,
  `name` varchar(150) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED',
  `lock_version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `tenant_id` (`tenant_id`,`kind`,`code`),
  CONSTRAINT `study_dimension_chk_1` CHECK ((`kind` in (_utf8mb4'GRADE',_utf8mb4'MAJOR')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `study_subject` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000',
  `organization_id` bigint NOT NULL,
  `code` varchar(64) NOT NULL,
  `name` varchar(150) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED',
  `lock_version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `organization_id` (`organization_id`,`code`),
  CONSTRAINT `study_subject_ibfk_1` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subscription_box_preference` (
  `user_id` bigint NOT NULL,
  `follow_id` bigint NOT NULL,
  `hidden` tinyint(1) NOT NULL DEFAULT '0',
  `push_enabled` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`user_id`,`follow_id`),
  KEY `follow_id` (`follow_id`),
  CONSTRAINT `subscription_box_preference_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `subscription_box_preference_ibfk_2` FOREIGN KEY (`follow_id`) REFERENCES `user_follow` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_client` (
  `id` bigint NOT NULL COMMENT 'id',
  `client_id` varchar(64) DEFAULT NULL COMMENT '客户端id',
  `client_key` varchar(32) DEFAULT NULL COMMENT '客户端key',
  `client_secret` varchar(255) DEFAULT NULL COMMENT '客户端秘钥',
  `grant_type` varchar(255) DEFAULT NULL COMMENT '授权类型',
  `device_type` varchar(32) DEFAULT NULL COMMENT '设备类型',
  `active_timeout` int DEFAULT '1800' COMMENT 'token活跃超时时间',
  `timeout` int DEFAULT '604800' COMMENT 'token固定超时',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统授权表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_config` (
  `config_id` bigint NOT NULL COMMENT '参数主键',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `config_name` varchar(100) DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参数配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dept` (
  `dept_id` bigint NOT NULL COMMENT '部门id',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门id',
  `ancestors` varchar(500) DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(30) DEFAULT '' COMMENT '部门名称',
  `dept_category` varchar(100) DEFAULT NULL COMMENT '部门类别编码',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `leader` bigint DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) DEFAULT NULL COMMENT '邮箱',
  `status` char(1) DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data` (
  `dict_code` bigint NOT NULL COMMENT '字典编码',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `dict_sort` int DEFAULT '0' COMMENT '字典排序',
  `dict_label` varchar(100) DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type` (
  `dict_id` bigint NOT NULL COMMENT '字典主键',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `dict_name` varchar(100) DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `tenant_id` (`tenant_id`,`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_logininfor` (
  `info_id` bigint NOT NULL COMMENT '访问ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `user_name` varchar(50) DEFAULT '' COMMENT '用户账号',
  `client_key` varchar(32) DEFAULT '' COMMENT '客户端',
  `device_type` varchar(32) DEFAULT '' COMMENT '设备类型',
  `ipaddr` varchar(128) DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) DEFAULT '' COMMENT '操作系统',
  `status` char(1) DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) DEFAULT '' COMMENT '提示消息',
  `login_time` datetime DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`info_id`),
  KEY `idx_sys_logininfor_s` (`status`),
  KEY `idx_sys_logininfor_lt` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统访问记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `path` varchar(200) DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
  `query_param` varchar(255) DEFAULT NULL COMMENT '路由参数',
  `is_frame` int DEFAULT '1' COMMENT '是否为外链（0是 1否）',
  `is_cache` int DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) DEFAULT '0' COMMENT '显示状态（0显示 1隐藏）',
  `status` char(1) DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) DEFAULT '#' COMMENT '菜单图标',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单权限表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_notice` (
  `notice_id` bigint NOT NULL COMMENT '公告ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `notice_title` varchar(50) NOT NULL COMMENT '公告标题',
  `notice_type` char(1) NOT NULL COMMENT '公告类型（1通知 2公告）',
  `notice_content` longblob COMMENT '公告内容',
  `status` char(1) DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知公告表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oper_log` (
  `oper_id` bigint NOT NULL COMMENT '日志主键',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `title` varchar(50) DEFAULT '' COMMENT '模块标题',
  `business_type` int DEFAULT '0' COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(100) DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) DEFAULT '' COMMENT '请求方式',
  `operator_type` int DEFAULT '0' COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(4000) DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(4000) DEFAULT '' COMMENT '返回参数',
  `status` int DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(4000) DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint DEFAULT '0' COMMENT '消耗时间',
  PRIMARY KEY (`oper_id`),
  KEY `idx_sys_oper_log_bt` (`business_type`),
  KEY `idx_sys_oper_log_s` (`status`),
  KEY `idx_sys_oper_log_ot` (`oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oss` (
  `oss_id` bigint NOT NULL COMMENT '对象存储主键',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `file_name` varchar(255) NOT NULL DEFAULT '' COMMENT '文件名',
  `original_name` varchar(255) NOT NULL DEFAULT '' COMMENT '原名',
  `file_suffix` varchar(10) NOT NULL DEFAULT '' COMMENT '文件后缀名',
  `url` varchar(500) NOT NULL COMMENT 'URL地址',
  `ext1` text COMMENT '扩展字段',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` bigint DEFAULT NULL COMMENT '上传人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `service` varchar(20) NOT NULL DEFAULT 'minio' COMMENT '服务商',
  PRIMARY KEY (`oss_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='OSS对象存储表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oss_config` (
  `oss_config_id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `config_key` varchar(20) NOT NULL DEFAULT '' COMMENT '配置key',
  `access_key` varchar(255) DEFAULT '' COMMENT 'accessKey',
  `secret_key` varchar(255) DEFAULT '' COMMENT '秘钥',
  `bucket_name` varchar(255) DEFAULT '' COMMENT '桶名称',
  `prefix` varchar(255) DEFAULT '' COMMENT '前缀',
  `endpoint` varchar(255) DEFAULT '' COMMENT '访问站点',
  `domain` varchar(255) DEFAULT '' COMMENT '自定义域名',
  `is_https` char(1) DEFAULT 'N' COMMENT '是否https（Y=是,N=否）',
  `region` varchar(255) DEFAULT '' COMMENT '域',
  `access_policy` char(1) NOT NULL DEFAULT '1' COMMENT '桶权限类型(0=private 1=public 2=custom)',
  `status` char(1) DEFAULT '1' COMMENT '是否默认（0=是,1=否）',
  `ext1` varchar(255) DEFAULT '' COMMENT '扩展字段',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`oss_config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='对象存储配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_post` (
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `dept_id` bigint NOT NULL COMMENT '部门id',
  `post_code` varchar(64) NOT NULL COMMENT '岗位编码',
  `post_category` varchar(100) DEFAULT NULL COMMENT '岗位类别编码',
  `post_name` varchar(50) NOT NULL COMMENT '岗位名称',
  `post_sort` int NOT NULL COMMENT '显示顺序',
  `status` char(1) NOT NULL COMMENT '状态（0正常 1停用）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `role_name` varchar(30) NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) NOT NULL COMMENT '角色权限字符串',
  `role_sort` int NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限）',
  `menu_check_strictly` tinyint(1) DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(1) DEFAULT '1' COMMENT '部门树选择项是否关联显示',
  `status` char(1) NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_dept` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色和部门关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色和菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_social` (
  `id` bigint NOT NULL COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户id',
  `auth_id` varchar(255) NOT NULL COMMENT '平台+平台唯一id',
  `source` varchar(255) NOT NULL COMMENT '用户来源',
  `open_id` varchar(255) DEFAULT NULL COMMENT '平台编号唯一id',
  `user_name` varchar(30) NOT NULL COMMENT '登录账号',
  `nick_name` varchar(30) DEFAULT '' COMMENT '用户昵称',
  `email` varchar(255) DEFAULT '' COMMENT '用户邮箱',
  `avatar` varchar(500) DEFAULT '' COMMENT '头像地址',
  `access_token` varchar(255) NOT NULL COMMENT '用户的授权令牌',
  `expire_in` int DEFAULT NULL COMMENT '用户的授权令牌的有效期，部分平台可能没有',
  `refresh_token` varchar(255) DEFAULT NULL COMMENT '刷新令牌，部分平台可能没有',
  `access_code` varchar(255) DEFAULT NULL COMMENT '平台的授权信息，部分平台可能没有',
  `union_id` varchar(255) DEFAULT NULL COMMENT '用户的 unionid',
  `scope` varchar(255) DEFAULT NULL COMMENT '授予的权限，部分平台可能没有',
  `token_type` varchar(255) DEFAULT NULL COMMENT '个别平台的授权信息，部分平台可能没有',
  `id_token` varchar(2000) DEFAULT NULL COMMENT 'id token，部分平台可能没有',
  `mac_algorithm` varchar(255) DEFAULT NULL COMMENT '小米平台用户的附带属性，部分平台可能没有',
  `mac_key` varchar(255) DEFAULT NULL COMMENT '小米平台用户的附带属性，部分平台可能没有',
  `code` varchar(255) DEFAULT NULL COMMENT '用户的授权code，部分平台可能没有',
  `oauth_token` varchar(255) DEFAULT NULL COMMENT 'Twitter平台用户的附带属性，部分平台可能没有',
  `oauth_token_secret` varchar(255) DEFAULT NULL COMMENT 'Twitter平台用户的附带属性，部分平台可能没有',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='社会化关系表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_tenant` (
  `id` bigint NOT NULL COMMENT 'id',
  `tenant_id` varchar(20) NOT NULL COMMENT '租户编号',
  `contact_user_name` varchar(20) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `company_name` varchar(30) DEFAULT NULL COMMENT '企业名称',
  `license_number` varchar(30) DEFAULT NULL COMMENT '统一社会信用代码',
  `address` varchar(200) DEFAULT NULL COMMENT '地址',
  `intro` varchar(200) DEFAULT NULL COMMENT '企业简介',
  `domain` varchar(200) DEFAULT NULL COMMENT '域名',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `package_id` bigint DEFAULT NULL COMMENT '租户套餐编号',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `account_count` int DEFAULT '-1' COMMENT '用户数量（-1不限制）',
  `status` char(1) DEFAULT '0' COMMENT '租户状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_tenant_package` (
  `package_id` bigint NOT NULL COMMENT '租户套餐id',
  `package_name` varchar(20) DEFAULT NULL COMMENT '套餐名称',
  `menu_ids` varchar(3000) DEFAULT NULL COMMENT '关联菜单id',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `menu_check_strictly` tinyint(1) DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`package_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户套餐表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) NOT NULL COMMENT '用户昵称',
  `user_type` varchar(10) DEFAULT 'sys_user' COMMENT '用户类型（sys_user系统用户）',
  `email` varchar(50) DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) DEFAULT '' COMMENT '手机号码',
  `sex` char(1) DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` bigint DEFAULT NULL COMMENT '头像地址',
  `password` varchar(100) DEFAULT '' COMMENT '密码',
  `status` char(1) DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `login_ip` varchar(128) DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `yunji_lock_version` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_staff_username` (`tenant_id`,`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_post` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户与岗位关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户和角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_demo` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `dept_id` bigint DEFAULT NULL COMMENT '部门id',
  `user_id` bigint DEFAULT NULL COMMENT '用户id',
  `order_num` int DEFAULT '0' COMMENT '排序号',
  `test_key` varchar(255) DEFAULT NULL COMMENT 'key键',
  `value` varchar(255) DEFAULT NULL COMMENT '值',
  `version` int DEFAULT '0' COMMENT '版本',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` int DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='测试单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_tree` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `parent_id` bigint DEFAULT '0' COMMENT '父id',
  `dept_id` bigint DEFAULT NULL COMMENT '部门id',
  `user_id` bigint DEFAULT NULL COMMENT '用户id',
  `tree_name` varchar(255) DEFAULT NULL COMMENT '值',
  `version` int DEFAULT '0' COMMENT '版本',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` int DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='测试树表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_browse_history` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `article_id` bigint NOT NULL,
  `last_browsed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `browse_count` int NOT NULL DEFAULT '1',
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_browse` (`user_id`,`article_id`),
  KEY `idx_browse_user` (`user_id`,`last_browsed_at`),
  KEY `fk_browse_article` (`article_id`),
  CONSTRAINT `fk_browse_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `fk_browse_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户浏览历史';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_exposure` (
  `user_id` bigint NOT NULL,
  `article_id` bigint NOT NULL,
  `window_key` date NOT NULL,
  `visible_count` int NOT NULL DEFAULT '0',
  `last_visible_at` datetime NOT NULL,
  PRIMARY KEY (`user_id`,`article_id`,`window_key`),
  KEY `fk_exposure_article` (`article_id`),
  CONSTRAINT `fk_exposure_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `fk_exposure_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `chk_exposure_count` CHECK ((`visible_count` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='可见曝光计数；不等于已读';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_exposure_total` (
  `user_id` bigint NOT NULL,
  `article_id` bigint NOT NULL,
  `visible_count` int NOT NULL DEFAULT '0',
  `day_key` date NOT NULL,
  `day_count` int NOT NULL DEFAULT '0',
  `last_visible_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`user_id`,`article_id`),
  KEY `article_id` (`article_id`),
  CONSTRAINT `user_exposure_total_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `user_exposure_total_ibfk_2` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_favorite` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `article_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_favorite` (`user_id`,`article_id`),
  KEY `idx_favorite_article` (`article_id`,`created_at`),
  CONSTRAINT `fk_favorite_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收藏';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_follow` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `target_type` varchar(20) NOT NULL COMMENT 'KEYWORD/ARTICLE',
  `target_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_follow` (`user_id`,`target_type`,`target_id`),
  KEY `idx_follow_target` (`target_type`,`target_id`,`user_id`),
  CONSTRAINT `fk_follow_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `chk_follow_type` CHECK ((`target_type` in (_utf8mb4'KEYWORD',_utf8mb4'ARTICLE',_utf8mb4'CATEGORY',_utf8mb4'LABEL')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户订阅';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_read_state` (
  `user_id` bigint NOT NULL,
  `article_id` bigint NOT NULL,
  `read_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `source` varchar(30) DEFAULT NULL,
  `read_version_id` bigint DEFAULT NULL,
  PRIMARY KEY (`user_id`,`article_id`),
  KEY `idx_read_article` (`article_id`,`read_at`),
  KEY `fk_read_version` (`article_id`,`read_version_id`),
  CONSTRAINT `fk_read_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `fk_read_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `fk_read_version` FOREIGN KEY (`article_id`, `read_version_id`) REFERENCES `article_version` (`article_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户已读状态';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_replacement_notice` (
  `user_id` bigint NOT NULL,
  `follow_id` bigint NOT NULL,
  `old_article_id` bigint NOT NULL,
  `new_article_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`,`old_article_id`),
  UNIQUE KEY `uk_replacement_notice_follow` (`follow_id`),
  KEY `old_article_id` (`old_article_id`),
  KEY `new_article_id` (`new_article_id`),
  CONSTRAINT `user_replacement_notice_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`),
  CONSTRAINT `user_replacement_notice_ibfk_2` FOREIGN KEY (`old_article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `user_replacement_notice_ibfk_3` FOREIGN KEY (`new_article_id`) REFERENCES `article` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_report` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `article_id` bigint NOT NULL,
  `report_type` varchar(30) NOT NULL,
  `reason` varchar(1000) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `handled_by` bigint DEFAULT NULL,
  `handled_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_report_status` (`status`,`created_at`),
  KEY `idx_report_user` (`user_id`,`created_at`),
  KEY `fk_report_article` (`article_id`),
  CONSTRAINT `fk_report_article` FOREIGN KEY (`article_id`) REFERENCES `article` (`id`),
  CONSTRAINT `fk_report_user` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户举报';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
