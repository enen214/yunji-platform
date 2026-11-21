
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











INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('activities','活动与功能','活动与报名配置',1,1,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('analytics/behavior','数据','行为统计',4,2,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('analytics/content','数据','内容统计',4,1,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('analytics/dashboard','数据','数据看板',4,0,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('analytics/profile','数据','用户概况',4,3,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('articles','内容','资讯管理',0,0,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('audit','数据','操作审计',4,4,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('banners','内容','轮播管理',0,8,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('catalog-tree','内容','分类树',0,5,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('categories','内容','板块管理',0,2,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('content-types','内容','信息类型',0,6,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('content/replacements','内容','更替判断列表',0,1,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('customers','用户与互动','小程序用户',3,0,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('feedback','用户与互动','反馈处理',3,1,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('file-library','活动与功能','文件管理',1,2,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('keywords','内容','关键词管理',0,4,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('labels','内容','标签管理',0,3,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('pages','内容','协议内容',0,9,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('recommendations','内容','首页强加',0,7,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('reminders/monitor','提醒与推送','监控与告警',2,5,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('reminders/records','提醒与推送','投递记录',2,2,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('reminders/rules','提醒与推送','强提醒规则',2,0,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('reminders/settings','提醒与推送','日常提醒设置',2,1,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('reminders/wecom-config','提醒与推送','企微配置',2,4,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('reminders/wecom-operations','提醒与推送','企微任务处理',2,3,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('reports','用户与互动','举报处理',3,2,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('review-applications','活动与功能','材料评审',1,4,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('review-campaigns','活动与功能','评审项目',1,3,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('system','系统','管理员账号',5,0,1);
INSERT INTO `admin_navigation` (`route`, `group_name`, `label`, `group_order`, `sort_order`, `enabled`) VALUES ('topics','活动与功能','活动页面专题',1,0,1);














INSERT INTO `article_replacement_guard` (`tenant_id`) VALUES ('000000');








INSERT INTO `content_category` (`id`, `tenant_id`, `parent_id`, `code`, `name`, `description`, `sort_order`, `status`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted_at`, `lock_version`) VALUES (1001,'000000',0,'COMPETITION','竞赛双创',NULL,10,'ENABLED',NULL,'2026-10-28 16:04:39',NULL,'2026-10-28 16:04:39',NULL,0);
INSERT INTO `content_category` (`id`, `tenant_id`, `parent_id`, `code`, `name`, `description`, `sort_order`, `status`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted_at`, `lock_version`) VALUES (1002,'000000',0,'ACADEMIC','学业修读',NULL,20,'ENABLED',NULL,'2026-10-28 16:04:39',NULL,'2026-10-28 16:04:39',NULL,0);
INSERT INTO `content_category` (`id`, `tenant_id`, `parent_id`, `code`, `name`, `description`, `sort_order`, `status`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted_at`, `lock_version`) VALUES (1003,'000000',0,'LIFE','生活服务',NULL,30,'ENABLED',NULL,'2026-10-28 16:04:39',NULL,'2026-10-28 16:04:39',NULL,0);
INSERT INTO `content_category` (`id`, `tenant_id`, `parent_id`, `code`, `name`, `description`, `sort_order`, `status`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted_at`, `lock_version`) VALUES (1004,'000000',0,'NOTICE','通知公告',NULL,40,'ENABLED',NULL,'2026-10-28 16:04:39',NULL,'2026-10-28 16:04:39',NULL,0);
INSERT INTO `content_category` (`id`, `tenant_id`, `parent_id`, `code`, `name`, `description`, `sort_order`, `status`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted_at`, `lock_version`) VALUES (1005,'000000',0,'FURTHER_EDU','升学发展',NULL,50,'ENABLED',NULL,'2026-10-28 16:04:39',NULL,'2026-10-28 16:04:39',NULL,0);
INSERT INTO `content_category` (`id`, `tenant_id`, `parent_id`, `code`, `name`, `description`, `sort_order`, `status`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted_at`, `lock_version`) VALUES (1006,'000000',0,'CAREER','就业求职',NULL,60,'ENABLED',NULL,'2026-10-28 16:04:39',NULL,'2026-10-28 16:04:39',NULL,0);
INSERT INTO `content_category` (`id`, `tenant_id`, `parent_id`, `code`, `name`, `description`, `sort_order`, `status`, `created_by`, `created_at`, `updated_by`, `updated_at`, `deleted_at`, `lock_version`) VALUES (1007,'000000',0,'CAMPUS','校园事务',NULL,70,'ENABLED',NULL,'2026-10-28 16:04:39',NULL,'2026-10-28 16:04:39',NULL,0);


INSERT INTO `content_page` (`id`, `tenant_id`, `page_key`, `title`, `content`, `version_no`, `status`, `updated_by`, `updated_at`) VALUES (2001,'000000','user_agreement','用户协议','<p>请在上线前替换为经确认的协议正文。</p>',1,'PUBLISHED',NULL,'2026-10-28 16:04:39');
INSERT INTO `content_page` (`id`, `tenant_id`, `page_key`, `title`, `content`, `version_no`, `status`, `updated_by`, `updated_at`) VALUES (2002,'000000','privacy_policy','隐私政策','<p>请在上线前替换为经确认的隐私政策。</p>',1,'PUBLISHED',NULL,'2026-10-28 16:04:39');
INSERT INTO `content_page` (`id`, `tenant_id`, `page_key`, `title`, `content`, `version_no`, `status`, `updated_by`, `updated_at`) VALUES (2003,'000000','about_us','关于云迹','<p>请在上线前替换为正式介绍。</p>',1,'PUBLISHED',NULL,'2026-10-28 16:04:39');

INSERT INTO `content_page_version` (`id`, `page_id`, `version_no`, `title`, `content`, `published_at`) VALUES (2001,2001,1,'用户协议','<p>请在上线前替换为经确认的协议正文。</p>','2026-10-28 16:04:42');
INSERT INTO `content_page_version` (`id`, `page_id`, `version_no`, `title`, `content`, `published_at`) VALUES (2002,2002,1,'隐私政策','<p>请在上线前替换为经确认的隐私政策。</p>','2026-10-28 16:04:42');
INSERT INTO `content_page_version` (`id`, `page_id`, `version_no`, `title`, `content`, `published_at`) VALUES (2003,2003,1,'关于云迹','<p>请在上线前替换为正式介绍。</p>','2026-10-28 16:04:42');

INSERT INTO `content_type_definition` (`code`, `name`, `renderer`, `require_original`, `require_body`, `require_summary`, `require_image`, `require_dates`, `enabled`, `lock_version`) VALUES ('BASIC','普通信息','TEXT',1,1,1,0,0,1,0);
INSERT INTO `content_type_definition` (`code`, `name`, `renderer`, `require_original`, `require_body`, `require_summary`, `require_image`, `require_dates`, `enabled`, `lock_version`) VALUES ('GUIDE','攻略','LONG_IMAGE',0,0,0,1,0,1,0);
INSERT INTO `content_type_definition` (`code`, `name`, `renderer`, `require_original`, `require_body`, `require_summary`, `require_image`, `require_dates`, `enabled`, `lock_version`) VALUES ('NOTICE','通知','NOTICE',0,1,0,0,1,1,0);






































INSERT INTO `reminder_control` (`id`, `daily_enabled`, `instant_enabled`, `timezone`, `slots`, `daily_template`, `instant_template`, `lock_version`, `plan_after_user`) VALUES (1,1,1,'Asia/Shanghai','[\"08:00\", \"13:00\", \"17:00\"]','','',0,0);















INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('framework-schema.sql','cb7cd3bae9757eaf85cb5139fb1cc76c2022623f53e670aa379cab2493032e36','2026-10-28 16:04:38');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V1__core_schema.sql','0a6d5c2778950950709225f37a80a0de2986a24c0dc3dcb3188d21626a217af1','2026-10-28 16:04:39');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V10__notification_ledger.sql','539e4ca17c66435c43d1302e158b987e2e858edc6d60bdc96d1b9576111f8209','2026-10-28 16:04:44');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V11__operation_and_rule_snapshots.sql','c0bbab25d4bcdcacb9e9329f6844d7d70f9fb2f615be0e5a4d0d54aaae2b9e14','2026-10-28 16:04:45');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V12__business_permissions.sql','33447c13830cb52273b3f27752abd77d445c14aaa84f92575b8f78c28437eb0d','2026-10-28 16:04:45');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V13__article_feed_snapshot.sql','c193373f52a179c53aa95c9754b642259027c6c4bbed12e13c090a984116e818','2026-10-28 16:04:45');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V14__resource_lifecycle.sql','870d6de2a9661903e870aa80f37e91fc35d3119f32f1a2423e14f0bdfb97fcfe','2026-10-28 16:04:46');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V15__file_cleanup_state.sql','bce44c5bda3849b10f303bc5819b33e5a1ea2762e49af34f8e39fba529c9264b','2026-10-28 16:04:46');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V16__replacement_serialization.sql','2b99caefb195ab26f6f0efd9b091c8db5a24b9bfab27e7fae886254e0f156a43','2026-10-28 16:04:46');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V17__export_permission.sql','a9c776c35ccab7c4b4b485dea82e54729d104b91a2e2777d9bec506bad3843b2','2026-10-28 16:04:46');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V18__filtered_requirements_alignment.sql','dda16fb35564d3ab3238ca30985a095d669a672f39a944c7a8373a2a53b9d35b','2026-10-28 16:04:46');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V19__content_lifecycle_and_preferences.sql','1d3e25a0a92e582982f1f8b285f37ba91e2a3def1bc65ff7d5deca95d62e655b','2026-10-28 16:04:47');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V2__seed_catalog.sql','baf6fa2b4603c5d7ed23aee47d872dca8a5335b7ecfd683276cbdb646aed361c','2026-10-28 16:04:39');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V20__discovery_labels_and_search.sql','c8389b44cf34da7fccd1d0d030910b15594de6d8379a740cdb4ef8d2653d9f17','2026-10-28 16:04:47');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V21__reminder_batches_and_catalog_curation.sql','23de9fe2003155d2d104ad9583fb16905e1d0fca643c3eadbeaef02902172c78','2026-10-28 16:04:48');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V22__reminder_planner_cursor.sql','15aeb6f7228c8c2d77a9d1ce24ad8dd367f0d85f4016eb14a3efb4173459a919','2026-10-28 16:04:48');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V23__activity_topics.sql','4228804c5820c5600c2c7018fd3780b960002f5f4091c6dd9661e8454da8e840','2026-10-28 16:04:48');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V24__product_baseline.sql','f7ef53f25c8de8164250e068b3d7eff6c9565aefeeedf93b701875390bb0e8df','2026-10-28 16:04:49');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V25__global_function_library.sql','d5f5b9b2dbba50ebf4a2459e5903ef9accb9f426ea3c5a93f5978650a0896987','2026-10-28 16:04:50');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V26__hot_archive.sql','291677b0242bc7d101b607af097ab6f2f29ee6d50914802810c6e3c5d494fa04','2026-10-28 16:04:50');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V3__identity_outbox_extensions.sql','e466fde78350e4f6cd6968677b96949f73b57c52afb80a8b6d43624a0993459d','2026-10-28 16:04:39');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V4__core_integrity.sql','70df53957dfca43d76724c961c553229bfcb650fe9a8120a2077d6c50e32fbd0','2026-10-28 16:04:41');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V5__identity_and_version_workflow.sql','bfc2afeb1c8cccb274ac4c2af640bef2923f0eb0b9b7a7f0d62c6afbb7a2bf64','2026-10-28 16:04:42');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V6__catalog_versions.sql','bb3be9652a36ab55d0d22c2814e9cdf94f5678efc0bf19ae34858c9a1972ded6','2026-10-28 16:04:43');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V7__engagement_events.sql','587401e1fc419e13a43890bcd63c6c8985867243e3ee4305adecf970f160def3','2026-10-28 16:04:43');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V8__files_activity_review_resource.sql','cacae23543e25868bd1d4404223b19c519932b973704bc540f565e6ee06676c2','2026-10-28 16:04:44');
INSERT INTO `schema_history` (`version`, `sha256`, `installed_at`) VALUES ('V9__reviewer_scope.sql','50ad5e677829cd9af259bec503192a283a791dd916dbcbc2ed1eebf2c40bb33a','2026-10-28 16:04:44');





INSERT INTO `sys_client` (`id`, `client_id`, `client_key`, `client_secret`, `grant_type`, `device_type`, `active_timeout`, `timeout`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1,'e5cd7e4891bf95d1d19206ce24a7b32e','pc','pc123','password,social','pc',1800,604800,'0','0',103,1,'2026-10-28 16:04:38',1,'2026-10-28 16:04:38');
INSERT INTO `sys_client` (`id`, `client_id`, `client_key`, `client_secret`, `grant_type`, `device_type`, `active_timeout`, `timeout`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (2,'428a8310cd442757ae699df5d894f051','app','app123','password,sms,social','android',1800,604800,'0','0',103,1,'2026-10-28 16:04:38',1,'2026-10-28 16:04:38');

INSERT INTO `sys_config` (`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1,'000000','主框架页-默认皮肤样式名称','sys.index.skinName','skin-blue','Y',103,1,'2026-10-28 16:04:38',NULL,NULL,'蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow');
INSERT INTO `sys_config` (`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2,'000000','用户管理-账号初始密码','sys.user.initPassword','123456','Y',103,1,'2026-10-28 16:04:38',NULL,NULL,'初始化密码 123456');
INSERT INTO `sys_config` (`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (3,'000000','主框架页-侧边栏主题','sys.index.sideTheme','theme-dark','Y',103,1,'2026-10-28 16:04:38',NULL,NULL,'深色主题theme-dark，浅色主题theme-light');
INSERT INTO `sys_config` (`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (5,'000000','账号自助-是否开启用户注册功能','sys.account.registerUser','false','Y',103,1,'2026-10-28 16:04:38',NULL,NULL,'是否开启注册用户功能（true开启，false关闭）');
INSERT INTO `sys_config` (`config_id`, `tenant_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (11,'000000','OSS预览列表资源开关','sys.oss.previewListResource','true','Y',103,1,'2026-10-28 16:04:38',NULL,NULL,'true:开启, false:关闭');

INSERT INTO `sys_dept` (`dept_id`, `tenant_id`, `parent_id`, `ancestors`, `dept_name`, `dept_category`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (100,'000000',0,'0','XXX科技',NULL,0,NULL,'15888888888','xxx@qq.com','0','0',103,1,'2026-10-28 16:04:37',NULL,NULL);
INSERT INTO `sys_dept` (`dept_id`, `tenant_id`, `parent_id`, `ancestors`, `dept_name`, `dept_category`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (101,'000000',100,'0,100','深圳总公司',NULL,1,NULL,'15888888888','xxx@qq.com','0','0',103,1,'2026-10-28 16:04:37',NULL,NULL);
INSERT INTO `sys_dept` (`dept_id`, `tenant_id`, `parent_id`, `ancestors`, `dept_name`, `dept_category`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (102,'000000',100,'0,100','长沙分公司',NULL,2,NULL,'15888888888','xxx@qq.com','0','0',103,1,'2026-10-28 16:04:37',NULL,NULL);
INSERT INTO `sys_dept` (`dept_id`, `tenant_id`, `parent_id`, `ancestors`, `dept_name`, `dept_category`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (103,'000000',101,'0,100,101','研发部门',NULL,1,1,'15888888888','xxx@qq.com','0','0',103,1,'2026-10-28 16:04:37',NULL,NULL);
INSERT INTO `sys_dept` (`dept_id`, `tenant_id`, `parent_id`, `ancestors`, `dept_name`, `dept_category`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (104,'000000',101,'0,100,101','市场部门',NULL,2,NULL,'15888888888','xxx@qq.com','0','0',103,1,'2026-10-28 16:04:37',NULL,NULL);
INSERT INTO `sys_dept` (`dept_id`, `tenant_id`, `parent_id`, `ancestors`, `dept_name`, `dept_category`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (105,'000000',101,'0,100,101','测试部门',NULL,3,NULL,'15888888888','xxx@qq.com','0','0',103,1,'2026-10-28 16:04:37',NULL,NULL);
INSERT INTO `sys_dept` (`dept_id`, `tenant_id`, `parent_id`, `ancestors`, `dept_name`, `dept_category`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (106,'000000',101,'0,100,101','财务部门',NULL,4,NULL,'15888888888','xxx@qq.com','0','0',103,1,'2026-10-28 16:04:37',NULL,NULL);
INSERT INTO `sys_dept` (`dept_id`, `tenant_id`, `parent_id`, `ancestors`, `dept_name`, `dept_category`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (107,'000000',101,'0,100,101','运维部门',NULL,5,NULL,'15888888888','xxx@qq.com','0','0',103,1,'2026-10-28 16:04:37',NULL,NULL);
INSERT INTO `sys_dept` (`dept_id`, `tenant_id`, `parent_id`, `ancestors`, `dept_name`, `dept_category`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (108,'000000',102,'0,100,102','市场部门',NULL,1,NULL,'15888888888','xxx@qq.com','0','0',103,1,'2026-10-28 16:04:37',NULL,NULL);
INSERT INTO `sys_dept` (`dept_id`, `tenant_id`, `parent_id`, `ancestors`, `dept_name`, `dept_category`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (109,'000000',102,'0,100,102','财务部门',NULL,2,NULL,'15888888888','xxx@qq.com','0','0',103,1,'2026-10-28 16:04:37',NULL,NULL);

INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1,'000000',1,'男','0','sys_user_sex','','','Y',103,1,'2026-10-28 16:04:38',NULL,NULL,'性别男');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2,'000000',2,'女','1','sys_user_sex','','','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'性别女');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (3,'000000',3,'未知','2','sys_user_sex','','','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'性别未知');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (4,'000000',1,'显示','0','sys_show_hide','','primary','Y',103,1,'2026-10-28 16:04:38',NULL,NULL,'显示菜单');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (5,'000000',2,'隐藏','1','sys_show_hide','','danger','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'隐藏菜单');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (6,'000000',1,'正常','0','sys_normal_disable','','primary','Y',103,1,'2026-10-28 16:04:38',NULL,NULL,'正常状态');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (7,'000000',2,'停用','1','sys_normal_disable','','danger','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'停用状态');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (12,'000000',1,'是','Y','sys_yes_no','','primary','Y',103,1,'2026-10-28 16:04:38',NULL,NULL,'系统默认是');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (13,'000000',2,'否','N','sys_yes_no','','danger','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'系统默认否');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (14,'000000',1,'通知','1','sys_notice_type','','warning','Y',103,1,'2026-10-28 16:04:38',NULL,NULL,'通知');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (15,'000000',2,'公告','2','sys_notice_type','','success','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'公告');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (16,'000000',1,'正常','0','sys_notice_status','','primary','Y',103,1,'2026-10-28 16:04:38',NULL,NULL,'正常状态');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (17,'000000',2,'关闭','1','sys_notice_status','','danger','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'关闭状态');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (18,'000000',1,'新增','1','sys_oper_type','','info','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'新增操作');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (19,'000000',2,'修改','2','sys_oper_type','','info','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'修改操作');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (20,'000000',3,'删除','3','sys_oper_type','','danger','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'删除操作');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (21,'000000',4,'授权','4','sys_oper_type','','primary','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'授权操作');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (22,'000000',5,'导出','5','sys_oper_type','','warning','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'导出操作');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (23,'000000',6,'导入','6','sys_oper_type','','warning','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'导入操作');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (24,'000000',7,'强退','7','sys_oper_type','','danger','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'强退操作');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (25,'000000',8,'生成代码','8','sys_oper_type','','warning','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'生成操作');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (26,'000000',9,'清空数据','9','sys_oper_type','','danger','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'清空操作');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (27,'000000',1,'成功','0','sys_common_status','','primary','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'正常状态');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (28,'000000',2,'失败','1','sys_common_status','','danger','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'停用状态');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (29,'000000',99,'其他','0','sys_oper_type','','info','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'其他操作');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (30,'000000',0,'密码认证','password','sys_grant_type','el-check-tag','default','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'密码认证');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (31,'000000',0,'短信认证','sms','sys_grant_type','el-check-tag','default','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'短信认证');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (32,'000000',0,'邮件认证','email','sys_grant_type','el-check-tag','default','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'邮件认证');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (33,'000000',0,'小程序认证','xcx','sys_grant_type','el-check-tag','default','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'小程序认证');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (34,'000000',0,'三方登录认证','social','sys_grant_type','el-check-tag','default','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'三方登录认证');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (35,'000000',0,'PC','pc','sys_device_type','','default','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'PC');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (36,'000000',0,'安卓','android','sys_device_type','','default','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'安卓');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (37,'000000',0,'iOS','ios','sys_device_type','','default','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'iOS');
INSERT INTO `sys_dict_data` (`dict_code`, `tenant_id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (38,'000000',0,'小程序','xcx','sys_device_type','','default','N',103,1,'2026-10-28 16:04:38',NULL,NULL,'小程序');

INSERT INTO `sys_dict_type` (`dict_id`, `tenant_id`, `dict_name`, `dict_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1,'000000','用户性别','sys_user_sex',103,1,'2026-10-28 16:04:38',NULL,NULL,'用户性别列表');
INSERT INTO `sys_dict_type` (`dict_id`, `tenant_id`, `dict_name`, `dict_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2,'000000','菜单状态','sys_show_hide',103,1,'2026-10-28 16:04:38',NULL,NULL,'菜单状态列表');
INSERT INTO `sys_dict_type` (`dict_id`, `tenant_id`, `dict_name`, `dict_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (3,'000000','系统开关','sys_normal_disable',103,1,'2026-10-28 16:04:38',NULL,NULL,'系统开关列表');
INSERT INTO `sys_dict_type` (`dict_id`, `tenant_id`, `dict_name`, `dict_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (6,'000000','系统是否','sys_yes_no',103,1,'2026-10-28 16:04:38',NULL,NULL,'系统是否列表');
INSERT INTO `sys_dict_type` (`dict_id`, `tenant_id`, `dict_name`, `dict_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (7,'000000','通知类型','sys_notice_type',103,1,'2026-10-28 16:04:38',NULL,NULL,'通知类型列表');
INSERT INTO `sys_dict_type` (`dict_id`, `tenant_id`, `dict_name`, `dict_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (8,'000000','通知状态','sys_notice_status',103,1,'2026-10-28 16:04:38',NULL,NULL,'通知状态列表');
INSERT INTO `sys_dict_type` (`dict_id`, `tenant_id`, `dict_name`, `dict_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9,'000000','操作类型','sys_oper_type',103,1,'2026-10-28 16:04:38',NULL,NULL,'操作类型列表');
INSERT INTO `sys_dict_type` (`dict_id`, `tenant_id`, `dict_name`, `dict_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (10,'000000','系统状态','sys_common_status',103,1,'2026-10-28 16:04:38',NULL,NULL,'登录状态列表');
INSERT INTO `sys_dict_type` (`dict_id`, `tenant_id`, `dict_name`, `dict_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (11,'000000','授权类型','sys_grant_type',103,1,'2026-10-28 16:04:38',NULL,NULL,'认证授权类型');
INSERT INTO `sys_dict_type` (`dict_id`, `tenant_id`, `dict_name`, `dict_type`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (12,'000000','设备类型','sys_device_type',103,1,'2026-10-28 16:04:38',NULL,NULL,'客户端设备类型');


INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1,'系统管理',0,1,'system',NULL,'',1,0,'M','0','0','','system',103,1,'2026-10-28 16:04:37',NULL,NULL,'系统管理目录');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2,'系统监控',0,3,'monitor',NULL,'',1,0,'M','0','0','','monitor',103,1,'2026-10-28 16:04:37',NULL,NULL,'系统监控目录');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (3,'系统工具',0,4,'tool',NULL,'',1,0,'M','0','0','','tool',103,1,'2026-10-28 16:04:37',NULL,NULL,'系统工具目录');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (4,'PLUS官网',0,5,'https://gitee.com/dromara/RuoYi-Vue-Plus',NULL,'',0,0,'M','0','0','','guide',103,1,'2026-10-28 16:04:37',NULL,NULL,'RuoYi-Vue-Plus官网地址');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (5,'测试菜单',0,5,'demo',NULL,'',1,0,'M','0','0','','star',103,1,'2026-10-28 16:04:37',NULL,NULL,'测试菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (6,'租户管理',0,2,'tenant',NULL,'',1,0,'M','0','0','','chart',103,1,'2026-10-28 16:04:37',NULL,NULL,'租户管理目录');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (100,'用户管理',1,1,'user','system/user/index','',1,0,'C','0','0','system:user:list','user',103,1,'2026-10-28 16:04:37',NULL,NULL,'用户管理菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (101,'角色管理',1,2,'role','system/role/index','',1,0,'C','0','0','system:role:list','peoples',103,1,'2026-10-28 16:04:37',NULL,NULL,'角色管理菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (102,'菜单管理',1,3,'menu','system/menu/index','',1,0,'C','0','0','system:menu:list','tree-table',103,1,'2026-10-28 16:04:37',NULL,NULL,'菜单管理菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (103,'部门管理',1,4,'dept','system/dept/index','',1,0,'C','0','0','system:dept:list','tree',103,1,'2026-10-28 16:04:37',NULL,NULL,'部门管理菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (104,'岗位管理',1,5,'post','system/post/index','',1,0,'C','0','0','system:post:list','post',103,1,'2026-10-28 16:04:37',NULL,NULL,'岗位管理菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (105,'字典管理',1,6,'dict','system/dict/index','',1,0,'C','0','0','system:dict:list','dict',103,1,'2026-10-28 16:04:37',NULL,NULL,'字典管理菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (106,'参数设置',1,7,'config','system/config/index','',1,0,'C','0','0','system:config:list','edit',103,1,'2026-10-28 16:04:37',NULL,NULL,'参数设置菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (107,'通知公告',1,8,'notice','system/notice/index','',1,0,'C','0','0','system:notice:list','message',103,1,'2026-10-28 16:04:37',NULL,NULL,'通知公告菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (108,'日志管理',1,9,'log','','',1,0,'M','0','0','','log',103,1,'2026-10-28 16:04:37',NULL,NULL,'日志管理菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (109,'在线用户',2,1,'online','monitor/online/index','',1,0,'C','0','0','monitor:online:list','online',103,1,'2026-10-28 16:04:37',NULL,NULL,'在线用户菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (113,'缓存监控',2,5,'cache','monitor/cache/index','',1,0,'C','0','0','monitor:cache:list','redis',103,1,'2026-10-28 16:04:37',NULL,NULL,'缓存监控菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (115,'代码生成',3,2,'gen','tool/gen/index','',1,0,'C','0','0','tool:gen:list','code',103,1,'2026-10-28 16:04:37',NULL,NULL,'代码生成菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (117,'Admin监控',2,5,'Admin','monitor/admin/index','',1,0,'C','0','0','monitor:admin:list','dashboard',103,1,'2026-10-28 16:04:37',NULL,NULL,'Admin监控菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (118,'文件管理',1,10,'oss','system/oss/index','',1,0,'C','0','0','system:oss:list','upload',103,1,'2026-10-28 16:04:37',NULL,NULL,'文件管理菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (120,'任务调度中心',2,6,'snailjob','monitor/snailjob/index','',1,0,'C','0','0','monitor:snailjob:list','job',103,1,'2026-10-28 16:04:37',NULL,NULL,'SnailJob控制台菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (121,'租户管理',6,1,'tenant','system/tenant/index','',1,0,'C','0','0','system:tenant:list','list',103,1,'2026-10-28 16:04:37',NULL,NULL,'租户管理菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (122,'租户套餐管理',6,2,'tenantPackage','system/tenantPackage/index','',1,0,'C','0','0','system:tenantPackage:list','form',103,1,'2026-10-28 16:04:37',NULL,NULL,'租户套餐管理菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (123,'客户端管理',1,11,'client','system/client/index','',1,0,'C','0','0','system:client:list','international',103,1,'2026-10-28 16:04:37',NULL,NULL,'客户端管理菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (500,'操作日志',108,1,'operlog','monitor/operlog/index','',1,0,'C','0','0','monitor:operlog:list','form',103,1,'2026-10-28 16:04:37',NULL,NULL,'操作日志菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (501,'登录日志',108,2,'logininfor','monitor/logininfor/index','',1,0,'C','0','0','monitor:logininfor:list','logininfor',103,1,'2026-10-28 16:04:37',NULL,NULL,'登录日志菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1001,'用户查询',100,1,'','','',1,0,'F','0','0','system:user:query','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1002,'用户新增',100,2,'','','',1,0,'F','0','0','system:user:add','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1003,'用户修改',100,3,'','','',1,0,'F','0','0','system:user:edit','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1004,'用户删除',100,4,'','','',1,0,'F','0','0','system:user:remove','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1005,'用户导出',100,5,'','','',1,0,'F','0','0','system:user:export','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1006,'用户导入',100,6,'','','',1,0,'F','0','0','system:user:import','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1007,'重置密码',100,7,'','','',1,0,'F','0','0','system:user:resetPwd','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1008,'角色查询',101,1,'','','',1,0,'F','0','0','system:role:query','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1009,'角色新增',101,2,'','','',1,0,'F','0','0','system:role:add','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1010,'角色修改',101,3,'','','',1,0,'F','0','0','system:role:edit','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1011,'角色删除',101,4,'','','',1,0,'F','0','0','system:role:remove','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1012,'角色导出',101,5,'','','',1,0,'F','0','0','system:role:export','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1013,'菜单查询',102,1,'','','',1,0,'F','0','0','system:menu:query','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1014,'菜单新增',102,2,'','','',1,0,'F','0','0','system:menu:add','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1015,'菜单修改',102,3,'','','',1,0,'F','0','0','system:menu:edit','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1016,'菜单删除',102,4,'','','',1,0,'F','0','0','system:menu:remove','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1017,'部门查询',103,1,'','','',1,0,'F','0','0','system:dept:query','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1018,'部门新增',103,2,'','','',1,0,'F','0','0','system:dept:add','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1019,'部门修改',103,3,'','','',1,0,'F','0','0','system:dept:edit','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1020,'部门删除',103,4,'','','',1,0,'F','0','0','system:dept:remove','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1021,'岗位查询',104,1,'','','',1,0,'F','0','0','system:post:query','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1022,'岗位新增',104,2,'','','',1,0,'F','0','0','system:post:add','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1023,'岗位修改',104,3,'','','',1,0,'F','0','0','system:post:edit','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1024,'岗位删除',104,4,'','','',1,0,'F','0','0','system:post:remove','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1025,'岗位导出',104,5,'','','',1,0,'F','0','0','system:post:export','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1026,'字典查询',105,1,'#','','',1,0,'F','0','0','system:dict:query','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1027,'字典新增',105,2,'#','','',1,0,'F','0','0','system:dict:add','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1028,'字典修改',105,3,'#','','',1,0,'F','0','0','system:dict:edit','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1029,'字典删除',105,4,'#','','',1,0,'F','0','0','system:dict:remove','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1030,'字典导出',105,5,'#','','',1,0,'F','0','0','system:dict:export','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1031,'参数查询',106,1,'#','','',1,0,'F','0','0','system:config:query','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1032,'参数新增',106,2,'#','','',1,0,'F','0','0','system:config:add','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1033,'参数修改',106,3,'#','','',1,0,'F','0','0','system:config:edit','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1034,'参数删除',106,4,'#','','',1,0,'F','0','0','system:config:remove','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1035,'参数导出',106,5,'#','','',1,0,'F','0','0','system:config:export','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1036,'公告查询',107,1,'#','','',1,0,'F','0','0','system:notice:query','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1037,'公告新增',107,2,'#','','',1,0,'F','0','0','system:notice:add','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1038,'公告修改',107,3,'#','','',1,0,'F','0','0','system:notice:edit','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1039,'公告删除',107,4,'#','','',1,0,'F','0','0','system:notice:remove','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1040,'操作查询',500,1,'#','','',1,0,'F','0','0','monitor:operlog:query','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1041,'操作删除',500,2,'#','','',1,0,'F','0','0','monitor:operlog:remove','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1042,'日志导出',500,4,'#','','',1,0,'F','0','0','monitor:operlog:export','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1043,'登录查询',501,1,'#','','',1,0,'F','0','0','monitor:logininfor:query','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1044,'登录删除',501,2,'#','','',1,0,'F','0','0','monitor:logininfor:remove','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1045,'日志导出',501,3,'#','','',1,0,'F','0','0','monitor:logininfor:export','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1046,'在线查询',109,1,'#','','',1,0,'F','0','0','monitor:online:query','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1047,'批量强退',109,2,'#','','',1,0,'F','0','0','monitor:online:batchLogout','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1048,'单条强退',109,3,'#','','',1,0,'F','0','0','monitor:online:forceLogout','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1050,'账户解锁',501,4,'#','','',1,0,'F','0','0','monitor:logininfor:unlock','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1055,'生成查询',115,1,'#','','',1,0,'F','0','0','tool:gen:query','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1056,'生成修改',115,2,'#','','',1,0,'F','0','0','tool:gen:edit','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1057,'生成删除',115,3,'#','','',1,0,'F','0','0','tool:gen:remove','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1058,'导入代码',115,2,'#','','',1,0,'F','0','0','tool:gen:import','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1059,'预览代码',115,4,'#','','',1,0,'F','0','0','tool:gen:preview','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1060,'生成代码',115,5,'#','','',1,0,'F','0','0','tool:gen:code','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1061,'客户端管理查询',123,1,'#','','',1,0,'F','0','0','system:client:query','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1062,'客户端管理新增',123,2,'#','','',1,0,'F','0','0','system:client:add','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1063,'客户端管理修改',123,3,'#','','',1,0,'F','0','0','system:client:edit','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1064,'客户端管理删除',123,4,'#','','',1,0,'F','0','0','system:client:remove','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1065,'客户端管理导出',123,5,'#','','',1,0,'F','0','0','system:client:export','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1500,'测试单表',5,1,'demo','demo/demo/index','',1,0,'C','0','0','demo:demo:list','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'测试单表菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1501,'测试单表查询',1500,1,'#','','',1,0,'F','0','0','demo:demo:query','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1502,'测试单表新增',1500,2,'#','','',1,0,'F','0','0','demo:demo:add','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1503,'测试单表修改',1500,3,'#','','',1,0,'F','0','0','demo:demo:edit','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1504,'测试单表删除',1500,4,'#','','',1,0,'F','0','0','demo:demo:remove','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1505,'测试单表导出',1500,5,'#','','',1,0,'F','0','0','demo:demo:export','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1506,'测试树表',5,1,'tree','demo/tree/index','',1,0,'C','0','0','demo:tree:list','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'测试树表菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1507,'测试树表查询',1506,1,'#','','',1,0,'F','0','0','demo:tree:query','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1508,'测试树表新增',1506,2,'#','','',1,0,'F','0','0','demo:tree:add','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1509,'测试树表修改',1506,3,'#','','',1,0,'F','0','0','demo:tree:edit','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1510,'测试树表删除',1506,4,'#','','',1,0,'F','0','0','demo:tree:remove','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1511,'测试树表导出',1506,5,'#','','',1,0,'F','0','0','demo:tree:export','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1600,'文件查询',118,1,'#','','',1,0,'F','0','0','system:oss:query','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1601,'文件上传',118,2,'#','','',1,0,'F','0','0','system:oss:upload','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1602,'文件下载',118,3,'#','','',1,0,'F','0','0','system:oss:download','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1603,'文件删除',118,4,'#','','',1,0,'F','0','0','system:oss:remove','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1606,'租户查询',121,1,'#','','',1,0,'F','0','0','system:tenant:query','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1607,'租户新增',121,2,'#','','',1,0,'F','0','0','system:tenant:add','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1608,'租户修改',121,3,'#','','',1,0,'F','0','0','system:tenant:edit','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1609,'租户删除',121,4,'#','','',1,0,'F','0','0','system:tenant:remove','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1610,'租户导出',121,5,'#','','',1,0,'F','0','0','system:tenant:export','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1611,'租户套餐查询',122,1,'#','','',1,0,'F','0','0','system:tenantPackage:query','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1612,'租户套餐新增',122,2,'#','','',1,0,'F','0','0','system:tenantPackage:add','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1613,'租户套餐修改',122,3,'#','','',1,0,'F','0','0','system:tenantPackage:edit','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1614,'租户套餐删除',122,4,'#','','',1,0,'F','0','0','system:tenantPackage:remove','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1615,'租户套餐导出',122,5,'#','','',1,0,'F','0','0','system:tenantPackage:export','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1620,'配置列表',118,5,'#','','',1,0,'F','0','0','system:ossConfig:list','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1621,'配置添加',118,6,'#','','',1,0,'F','0','0','system:ossConfig:add','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1622,'配置编辑',118,6,'#','','',1,0,'F','0','0','system:ossConfig:edit','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1623,'配置删除',118,6,'#','','',1,0,'F','0','0','system:ossConfig:remove','#',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9500,'内容读取',0,0,'yunji-0',NULL,NULL,1,0,'F','1','0','yunji:article:read','#',NULL,NULL,NULL,NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9501,'内容编辑',0,1,'yunji-1',NULL,NULL,1,0,'F','1','0','yunji:article:write','#',NULL,NULL,NULL,NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9502,'内容审核',0,2,'yunji-2',NULL,NULL,1,0,'F','1','0','yunji:article:review','#',NULL,NULL,NULL,NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9503,'内容发布',0,3,'yunji-3',NULL,NULL,1,0,'F','1','0','yunji:article:publish','#',NULL,NULL,NULL,NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9504,'目录治理',0,4,'yunji-4',NULL,NULL,1,0,'F','1','0','yunji:catalog:manage','#',NULL,NULL,NULL,NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9505,'活动配置',0,5,'yunji-5',NULL,NULL,1,0,'F','1','0','yunji:activity:manage','#',NULL,NULL,NULL,NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9506,'评审配置',0,6,'yunji-6',NULL,NULL,1,0,'F','1','0','yunji:review:manage','#',NULL,NULL,NULL,NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9507,'材料评审',0,7,'yunji-7',NULL,NULL,1,0,'F','1','0','yunji:review:decide','#',NULL,NULL,NULL,NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9508,'资料工具',0,8,'yunji-8',NULL,NULL,1,0,'F','1','0','yunji:resource:manage','#',NULL,NULL,NULL,NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9509,'文件治理',0,9,'yunji-9',NULL,NULL,1,0,'F','1','0','yunji:file:manage','#',NULL,NULL,NULL,NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9510,'提醒账本',0,10,'yunji-10',NULL,NULL,1,0,'F','1','0','yunji:notification:manage','#',NULL,NULL,NULL,NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9511,'运营配置',0,11,'yunji-11',NULL,NULL,1,0,'F','1','0','yunji:operation:manage','#',NULL,NULL,NULL,NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9512,'举报反馈',0,12,'yunji-12',NULL,NULL,1,0,'F','1','0','yunji:engagement:manage','#',NULL,NULL,NULL,NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9513,'效果分析',0,13,'yunji-13',NULL,NULL,1,0,'F','1','0','yunji:analytics:read','#',NULL,NULL,NULL,NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9514,'业务审计',0,14,'yunji-14',NULL,NULL,1,0,'F','1','0','yunji:audit:read','#',NULL,NULL,NULL,NULL,NULL,'');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9518,'云迹数据导出',9500,18,'export',NULL,NULL,1,0,'F','0','0','yunji:export','#',103,1,'2026-10-28 08:04:46',NULL,NULL,'');

INSERT INTO `sys_notice` (`notice_id`, `tenant_id`, `notice_title`, `notice_type`, `notice_content`, `status`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1,'000000','温馨提醒：2018-07-01 新版本发布啦','2',0xE696B0E78988E69CACE58685E5AEB9,'0',103,1,'2026-10-28 16:04:38',NULL,NULL,'管理员');
INSERT INTO `sys_notice` (`notice_id`, `tenant_id`, `notice_title`, `notice_type`, `notice_content`, `status`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2,'000000','维护通知：2018-07-01 系统凌晨维护','1',0xE7BBB4E68AA4E58685E5AEB9,'0',103,1,'2026-10-28 16:04:38',NULL,NULL,'管理员');



INSERT INTO `sys_oss_config` (`oss_config_id`, `tenant_id`, `config_key`, `access_key`, `secret_key`, `bucket_name`, `prefix`, `endpoint`, `domain`, `is_https`, `region`, `access_policy`, `status`, `ext1`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1,'000000','minio','ruoyi','ruoyi123','ruoyi','','127.0.0.1:9000','','N','','1','0','',103,1,'2026-10-28 16:04:38',1,'2026-10-28 16:04:38',NULL);
INSERT INTO `sys_oss_config` (`oss_config_id`, `tenant_id`, `config_key`, `access_key`, `secret_key`, `bucket_name`, `prefix`, `endpoint`, `domain`, `is_https`, `region`, `access_policy`, `status`, `ext1`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2,'000000','qiniu','XXXXXXXXXXXXXXX','XXXXXXXXXXXXXXX','ruoyi','','s3-cn-north-1.qiniucs.com','','N','','1','1','',103,1,'2026-10-28 16:04:38',1,'2026-10-28 16:04:38',NULL);
INSERT INTO `sys_oss_config` (`oss_config_id`, `tenant_id`, `config_key`, `access_key`, `secret_key`, `bucket_name`, `prefix`, `endpoint`, `domain`, `is_https`, `region`, `access_policy`, `status`, `ext1`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (3,'000000','aliyun','XXXXXXXXXXXXXXX','XXXXXXXXXXXXXXX','ruoyi','','oss-cn-beijing.aliyuncs.com','','N','','1','1','',103,1,'2026-10-28 16:04:38',1,'2026-10-28 16:04:38',NULL);
INSERT INTO `sys_oss_config` (`oss_config_id`, `tenant_id`, `config_key`, `access_key`, `secret_key`, `bucket_name`, `prefix`, `endpoint`, `domain`, `is_https`, `region`, `access_policy`, `status`, `ext1`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (4,'000000','qcloud','XXXXXXXXXXXXXXX','XXXXXXXXXXXXXXX','ruoyi-1240000000','','cos.ap-beijing.myqcloud.com','','N','ap-beijing','1','1','',103,1,'2026-10-28 16:04:38',1,'2026-10-28 16:04:38',NULL);
INSERT INTO `sys_oss_config` (`oss_config_id`, `tenant_id`, `config_key`, `access_key`, `secret_key`, `bucket_name`, `prefix`, `endpoint`, `domain`, `is_https`, `region`, `access_policy`, `status`, `ext1`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (5,'000000','image','ruoyi','ruoyi123','ruoyi','image','127.0.0.1:9000','','N','','1','1','',103,1,'2026-10-28 16:04:38',1,'2026-10-28 16:04:38',NULL);

INSERT INTO `sys_post` (`post_id`, `tenant_id`, `dept_id`, `post_code`, `post_category`, `post_name`, `post_sort`, `status`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1,'000000',103,'ceo',NULL,'董事长',1,'0',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_post` (`post_id`, `tenant_id`, `dept_id`, `post_code`, `post_category`, `post_name`, `post_sort`, `status`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2,'000000',100,'se',NULL,'项目经理',2,'0',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_post` (`post_id`, `tenant_id`, `dept_id`, `post_code`, `post_category`, `post_name`, `post_sort`, `status`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (3,'000000',100,'hr',NULL,'人力资源',3,'0',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_post` (`post_id`, `tenant_id`, `dept_id`, `post_code`, `post_category`, `post_name`, `post_sort`, `status`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (4,'000000',100,'user',NULL,'普通员工',4,'0',103,1,'2026-10-28 16:04:37',NULL,NULL,'');

INSERT INTO `sys_role` (`role_id`, `tenant_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1,'000000','超级管理员','superadmin',1,'1',1,1,'0','0',103,1,'2026-10-28 16:04:37',NULL,NULL,'超级管理员');
INSERT INTO `sys_role` (`role_id`, `tenant_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (3,'000000','本部门及以下','test1',3,'4',1,1,'0','0',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_role` (`role_id`, `tenant_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (4,'000000','仅本人','test2',4,'5',1,1,'0','0',103,1,'2026-10-28 16:04:37',NULL,NULL,'');
INSERT INTO `sys_role` (`role_id`, `tenant_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9700,'000000','云迹运营','yunji_operator',10,'1',1,1,'0','0',NULL,NULL,NULL,NULL,NULL,NULL);
INSERT INTO `sys_role` (`role_id`, `tenant_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9701,'000000','云迹创作者','yunji_creator',10,'5',1,1,'0','0',NULL,NULL,NULL,NULL,NULL,NULL);
INSERT INTO `sys_role` (`role_id`, `tenant_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9702,'000000','云迹审核员','yunji_reviewer',10,'5',1,1,'0','0',NULL,NULL,NULL,NULL,NULL,NULL);


INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,5);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,100);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,101);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,102);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,103);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,104);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,105);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,106);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,107);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,108);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,118);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,123);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,500);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,501);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1001);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1002);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1003);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1004);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1005);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1006);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1007);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1008);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1009);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1010);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1011);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1012);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1013);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1014);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1015);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1016);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1017);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1018);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1019);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1020);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1021);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1022);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1023);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1024);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1025);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1026);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1027);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1028);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1029);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1030);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1031);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1032);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1033);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1034);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1035);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1036);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1037);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1038);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1039);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1040);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1041);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1042);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1043);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1044);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1045);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1050);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1061);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1062);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1063);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1064);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1065);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1500);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1501);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1502);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1503);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1504);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1505);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1506);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1507);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1508);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1509);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1510);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1511);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1600);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1601);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1602);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1603);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1620);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1621);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1622);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,1623);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,11618);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,11619);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,11629);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,11632);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,11633);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,11638);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,11639);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,11640);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,11641);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,11642);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (3,11643);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4,5);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4,1500);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4,1501);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4,1502);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4,1503);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4,1504);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4,1505);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4,1506);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4,1507);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4,1508);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4,1509);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4,1510);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (4,1511);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9700,9500);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9700,9501);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9700,9502);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9700,9503);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9700,9504);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9700,9505);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9700,9506);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9700,9507);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9700,9508);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9700,9509);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9700,9510);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9700,9511);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9700,9512);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9700,9513);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9700,9514);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9701,9500);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9701,9501);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9702,9500);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9702,9502);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9702,9503);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9702,9507);


INSERT INTO `sys_tenant` (`id`, `tenant_id`, `contact_user_name`, `contact_phone`, `company_name`, `license_number`, `address`, `intro`, `domain`, `remark`, `package_id`, `expire_time`, `account_count`, `status`, `del_flag`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1,'000000','管理组','15888888888','XXX有限公司',NULL,NULL,'多租户通用后台管理管理系统',NULL,NULL,NULL,NULL,-1,'0','0',103,1,'2026-10-28 16:04:37',NULL,NULL);


INSERT INTO `sys_user` (`user_id`, `tenant_id`, `dept_id`, `user_name`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `avatar`, `password`, `status`, `del_flag`, `login_ip`, `login_date`, `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `yunji_lock_version`) VALUES (1,'000000',103,'admin','疯狂的狮子Li','sys_user','','','1',NULL,'!UNINITIALIZED!','1','0','127.0.0.1','2026-10-28 16:04:37',103,1,'2026-10-28 16:04:37',NULL,NULL,'管理员',0);

INSERT INTO `sys_user_post` (`user_id`, `post_id`) VALUES (1,1);

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1,1);

INSERT INTO `test_demo` (`id`, `tenant_id`, `dept_id`, `user_id`, `order_num`, `test_key`, `value`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (1,'000000',102,4,1,'测试数据权限','测试',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_demo` (`id`, `tenant_id`, `dept_id`, `user_id`, `order_num`, `test_key`, `value`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (2,'000000',102,3,2,'子节点1','111',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_demo` (`id`, `tenant_id`, `dept_id`, `user_id`, `order_num`, `test_key`, `value`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (3,'000000',102,3,3,'子节点2','222',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_demo` (`id`, `tenant_id`, `dept_id`, `user_id`, `order_num`, `test_key`, `value`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (4,'000000',108,4,4,'测试数据','demo',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_demo` (`id`, `tenant_id`, `dept_id`, `user_id`, `order_num`, `test_key`, `value`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (5,'000000',108,3,13,'子节点11','1111',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_demo` (`id`, `tenant_id`, `dept_id`, `user_id`, `order_num`, `test_key`, `value`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (6,'000000',108,3,12,'子节点22','2222',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_demo` (`id`, `tenant_id`, `dept_id`, `user_id`, `order_num`, `test_key`, `value`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (7,'000000',108,3,11,'子节点33','3333',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_demo` (`id`, `tenant_id`, `dept_id`, `user_id`, `order_num`, `test_key`, `value`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (8,'000000',108,3,10,'子节点44','4444',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_demo` (`id`, `tenant_id`, `dept_id`, `user_id`, `order_num`, `test_key`, `value`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (9,'000000',108,3,9,'子节点55','5555',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_demo` (`id`, `tenant_id`, `dept_id`, `user_id`, `order_num`, `test_key`, `value`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (10,'000000',108,3,8,'子节点66','6666',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_demo` (`id`, `tenant_id`, `dept_id`, `user_id`, `order_num`, `test_key`, `value`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (11,'000000',108,3,7,'子节点77','7777',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_demo` (`id`, `tenant_id`, `dept_id`, `user_id`, `order_num`, `test_key`, `value`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (12,'000000',108,3,6,'子节点88','8888',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_demo` (`id`, `tenant_id`, `dept_id`, `user_id`, `order_num`, `test_key`, `value`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (13,'000000',108,3,5,'子节点99','9999',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);

INSERT INTO `test_tree` (`id`, `tenant_id`, `parent_id`, `dept_id`, `user_id`, `tree_name`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (1,'000000',0,102,4,'测试数据权限',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_tree` (`id`, `tenant_id`, `parent_id`, `dept_id`, `user_id`, `tree_name`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (2,'000000',1,102,3,'子节点1',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_tree` (`id`, `tenant_id`, `parent_id`, `dept_id`, `user_id`, `tree_name`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (3,'000000',2,102,3,'子节点2',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_tree` (`id`, `tenant_id`, `parent_id`, `dept_id`, `user_id`, `tree_name`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (4,'000000',0,108,4,'测试树1',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_tree` (`id`, `tenant_id`, `parent_id`, `dept_id`, `user_id`, `tree_name`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (5,'000000',4,108,3,'子节点11',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_tree` (`id`, `tenant_id`, `parent_id`, `dept_id`, `user_id`, `tree_name`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (6,'000000',4,108,3,'子节点22',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_tree` (`id`, `tenant_id`, `parent_id`, `dept_id`, `user_id`, `tree_name`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (7,'000000',4,108,3,'子节点33',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_tree` (`id`, `tenant_id`, `parent_id`, `dept_id`, `user_id`, `tree_name`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (8,'000000',5,108,3,'子节点44',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_tree` (`id`, `tenant_id`, `parent_id`, `dept_id`, `user_id`, `tree_name`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (9,'000000',6,108,3,'子节点55',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_tree` (`id`, `tenant_id`, `parent_id`, `dept_id`, `user_id`, `tree_name`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (10,'000000',7,108,3,'子节点66',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_tree` (`id`, `tenant_id`, `parent_id`, `dept_id`, `user_id`, `tree_name`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (11,'000000',7,108,3,'子节点77',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_tree` (`id`, `tenant_id`, `parent_id`, `dept_id`, `user_id`, `tree_name`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (12,'000000',10,108,3,'子节点88',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);
INSERT INTO `test_tree` (`id`, `tenant_id`, `parent_id`, `dept_id`, `user_id`, `tree_name`, `version`, `create_dept`, `create_time`, `create_by`, `update_time`, `update_by`, `del_flag`) VALUES (13,'000000',10,108,3,'子节点99',0,103,'2026-10-28 16:04:38',1,NULL,NULL,0);









/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
