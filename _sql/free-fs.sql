/*
 Navicat Premium Dump SQL

 Source Server         : mysql8
 Source Server Type    : MySQL
 Source Server Version : 80300 (8.3.0)
 Source Host           : localhost:3306
 Source Schema         : free-fs

 Target Server Type    : MySQL
 Target Server Version : 80300 (8.3.0)
 File Encoding         : 65001

 Date: 10/11/2025 14:03:56
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for file_info
-- ----------------------------
DROP TABLE IF EXISTS `file_info`;
CREATE TABLE `file_info`  (
  `id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `object_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '资源名称',
  `original_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '资源原始名称',
  `display_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '资源别名',
  `suffix` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '后缀名',
  `size` bigint NULL DEFAULT NULL COMMENT '大小',
  `mime_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '存储标准MIME类型',
  `is_dir` tinyint(1) NULL DEFAULT NULL COMMENT '是否目录',
  `parent_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户id',
  `content_md5` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '用于秒传和文件校验',
  `storage_platform_setting_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '存储平台标识符',
  `upload_time` datetime NULL DEFAULT NULL COMMENT '上传时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `is_deleted` tinyint(1) NULL DEFAULT NULL COMMENT '软删除标记，回收站标识0：未删除 1：已删除',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '文件资源表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of file_info
-- ----------------------------

-- ----------------------------
-- Table structure for file_share_access_record
-- ----------------------------
DROP TABLE IF EXISTS `file_share_access_record`;
CREATE TABLE `file_share_access_record`  (
  `id` bigint NOT NULL,
  `share_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分享ID',
  `access_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '访问IP',
  `access_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '访问地址',
  `browser` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '浏览器类型',
  `os` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '操作系统',
  `access_time` datetime NOT NULL COMMENT '访问时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '分享页面访问记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of file_share_access_record
-- ----------------------------

-- ----------------------------
-- Table structure for file_share_items
-- ----------------------------
DROP TABLE IF EXISTS `file_share_items`;
CREATE TABLE `file_share_items`  (
  `share_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分享ID',
  `file_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件/文件夹ID',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`share_id`, `file_id` DESC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '分享文件关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of file_share_items
-- ----------------------------

-- ----------------------------
-- Table structure for file_shares
-- ----------------------------
DROP TABLE IF EXISTS `file_shares`;
CREATE TABLE `file_shares`  (
  `id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分享ID',
  `user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分享人ID',
  `share_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分享名称',
  `share_code` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '提取码（可为空）',
  `expire_time` datetime NULL DEFAULT NULL COMMENT '过期时间（null表示永久有效）',
  `view_count` int NULL DEFAULT 0 COMMENT '查看次数统计',
  `max_view_count` int NULL DEFAULT NULL COMMENT '最大查看次数（NULL表示无限制）',
  `download_count` int NULL DEFAULT 0 COMMENT '下载次数统计',
  `max_download_count` int NULL DEFAULT NULL COMMENT '最大下载次数（NULL表示无限制）',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '文件分享表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of file_shares
-- ----------------------------

-- ----------------------------
-- Table structure for file_upload_task
-- ----------------------------
DROP TABLE IF EXISTS `file_upload_task`;
CREATE TABLE `file_upload_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务ID(UUID)',
  `upload_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '上传唯一ID',
  `parent_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '父ID',
  `user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `storage_platform_setting_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '存储平台配置ID',
  `object_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '对象key',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件名',
  `file_size` bigint NOT NULL COMMENT '文件大小(字节)',
  `file_md5` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件MD5值',
  `suffix` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件类型(扩展名)',
  `mime_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '存储标准MIME类型',
  `total_chunks` int NOT NULL COMMENT '总分片数',
  `uploaded_chunks` int NULL DEFAULT 0 COMMENT '已上传分片数',
  `chunk_size` bigint NULL DEFAULT 5242880 COMMENT '分片大小(默认5MB)',
  `uploaded_size` bigint NULL DEFAULT 0 COMMENT '已上传大小(字节)',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'uploading' COMMENT '状态: uploading-上传中, paused-已暂停, completed-已完成, failed-失败, canceled-已取消',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '错误信息',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `complete_time` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_task_id`(`task_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_file_md5`(`file_md5` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 131 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '上传任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of file_upload_task
-- ----------------------------

-- ----------------------------
-- Table structure for file_user_favorites
-- ----------------------------
DROP TABLE IF EXISTS `file_user_favorites`;
CREATE TABLE `file_user_favorites`  (
  `user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
  `file_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件ID',
  `favorite_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`user_id`, `file_id`) USING BTREE,
  INDEX `idx_file_time`(`file_id` ASC, `favorite_time` DESC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '文件收藏表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of file_user_favorites
-- ----------------------------

-- ----------------------------
-- Table structure for storage_platform
-- ----------------------------
DROP TABLE IF EXISTS `storage_platform`;
CREATE TABLE `storage_platform`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '存储平台',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '存储平台名称',
  `identifier` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '存储平台标识符',
  `config_scheme` json NOT NULL COMMENT '存储平台配置描述schema',
  `icon` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '存储平台图标',
  `link` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '存储平台链接',
  `is_default` tinyint NOT NULL DEFAULT 1 COMMENT '是否默认存储平台 0-否 1-是',
  `desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '存储平台描述',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '存储平台' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of storage_platform
-- ----------------------------
INSERT INTO `storage_platform` VALUES (1, '阿里云OSS', 'AliyunOSS', '[{\"label\": \"Access-Key\", \"dataType\": \"string\", \"identifier\": \"accessKey\", \"validation\": {\"required\": true}}, {\"label\": \"Secret-key\", \"dataType\": \"string\", \"identifier\": \"secretKey\", \"validation\": {\"required\": true}}, {\"label\": \"服务器端点\", \"dataType\": \"string\", \"identifier\": \"endpoint\", \"validation\": {\"required\": true}}, {\"label\": \"存储桶名\", \"dataType\": \"string\", \"identifier\": \"bucket\", \"validation\": {\"required\": true}}, {\"label\": \"区域\", \"dataType\": \"string\", \"identifier\": \"region\", \"validation\": {\"required\": true}}]', 'icon-aliyun1', 'https://www.aliyun.com/product/oss?utm_content=se_1020894540', 0, '阿里云对象存储 OSS（Object Storage Service）是一款海量、安全、低成本、高可靠的云存储服务');
INSERT INTO `storage_platform` VALUES (2, 'Minio', 'Minio', '[{\"label\": \"Access-Key\", \"dataType\": \"string\", \"identifier\": \"accessKey\", \"validation\": {\"required\": true}}, {\"label\": \"Secret-key\", \"dataType\": \"string\", \"identifier\": \"secretKey\", \"validation\": {\"required\": true}}, {\"label\": \"服务器端点\", \"dataType\": \"string\", \"identifier\": \"endpoint\", \"validation\": {\"required\": true}}, {\"label\": \"存储桶名\", \"dataType\": \"string\", \"identifier\": \"bucket\", \"validation\": {\"required\": true}}]', 'icon-Minio1', 'https://www.minio.org.cn/?bd_vid=10111900197314796808', 0, 'MinIO 是一种高性能、S3 兼容的对象存储。');
INSERT INTO `storage_platform` VALUES (3, '七牛云', 'Kodo', '[{\"label\": \"Access-Key\", \"dataType\": \"string\", \"identifier\": \"accessKey\", \"validation\": {\"required\": true}}, {\"label\": \"Secret-key\", \"dataType\": \"string\", \"identifier\": \"secretKey\", \"validation\": {\"required\": true}}, {\"label\": \"服务器端点\", \"dataType\": \"string\", \"identifier\": \"endpoint\", \"validation\": {\"required\": true}}, {\"label\": \"存储桶名\", \"dataType\": \"string\", \"identifier\": \"bucket\", \"validation\": {\"required\": true}}]', 'icon-normal-logo-blue', 'https://www.qiniu.com/products/kodo', 0, '七牛云海量存储系统（Kodo）是自主研发的非结构化数据存储管理平台，支持中心和边缘存储。');
INSERT INTO `storage_platform` VALUES (4, 'RustFS', 'Rustfs', '[]', NULL, 'https://docs.rustfs.com/', 1, 'RustFS 用热门安全的 Rust 语言开发，兼容 S3 协议。适用于 AI/ML 及海量数据存储、大数据、互联网、工业和保密存储等全部场景。');

-- ----------------------------
-- Table structure for storage_settings
-- ----------------------------
DROP TABLE IF EXISTS `storage_settings`;
CREATE TABLE `storage_settings`  (
  `id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'id',
  `platform_identifier` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '存储平台标识符',
  `config_data` json NOT NULL COMMENT '存储配置',
  `enabled` tinyint(1) NOT NULL COMMENT '是否启用 0：否 1：是',
  `user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '所属用户',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '存储平台配置' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of storage_settings
-- ----------------------------

-- ----------------------------
-- Table structure for subscription_plan
-- ----------------------------
DROP TABLE IF EXISTS `subscription_plan`;
CREATE TABLE `subscription_plan`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '套餐代码',
  `plan_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '套餐名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '套餐描述',
  `storage_quota_gb` int NOT NULL COMMENT '存储配额(GB)',
  `max_files` int NOT NULL COMMENT '最大文件数',
  `max_file_size` bigint NOT NULL COMMENT '单个文件最大大小(字节)',
  `bandwidth_quota` bigint NOT NULL COMMENT '每月带宽配额(字节)',
  `price` double(8, 2) NOT NULL COMMENT '价格/月',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用0否1是',
  `is_default` tinyint NOT NULL COMMENT '是否为默认套餐 0否1是',
  `sort_order` int NOT NULL COMMENT '排序',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除 0否1是',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '套餐表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of subscription_plan
-- ----------------------------
INSERT INTO `subscription_plan` VALUES (1, 'Free', '免费套餐', '', 20, 50, 1024, 1, 0.00, 1, 1, 1, '2025-09-23 14:54:29', '2025-09-23 14:54:32', 0);
INSERT INTO `subscription_plan` VALUES (2, 'Basic', '基础套餐', NULL, 100, 50, 1, 1, 2999.00, 1, 0, 2, '2025-09-23 14:57:32', '2025-09-23 14:57:34', 0);

-- ----------------------------
-- Table structure for sys_login_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户编号',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '用户账号',
  `login_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录IP',
  `login_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '登录地址',
  `browser` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '浏览器类型',
  `os` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作系统',
  `status` tinyint NOT NULL COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '提示消息',
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3769 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统访问记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_login_log
-- ----------------------------

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
  `username` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '邮箱',
  `nickname` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '昵称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像',
  `status` int NOT NULL DEFAULT 0 COMMENT '用户状态 0正常 1禁用',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  `last_login_at` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES ('01jrvgs943q0f43h0aa5mjde0y', 'admin', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', '459102951@qq.com', '超级管理员', 'https://csdn-665-inscode.s3.cn-north-1.jdcloud-oss.com/inscode/202303/628c9f991a7e4862742d8a2f/1680072908255-49035150-ttVQUH7YUEaCdHRZenaoQrUQPxtaBUay/large', 0, '2025-04-15 09:25:22', '2025-11-10 13:40:10', '2025-11-10 13:40:10');
INSERT INTO `sys_user` VALUES ('01k5xawhp9mas36qywkgqdk8nj', 'test2', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'test2@qq.com', '测试用户2', NULL, 0, '2025-09-24 15:44:26', '2025-10-20 10:51:15', '2025-10-20 10:51:15');
INSERT INTO `sys_user` VALUES ('01k5zrbyzgqxvdtxehe6k99fvp', 'test3', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'xx@qq.com', '测试3', NULL, 0, '2025-09-25 14:18:31', '2025-09-28 10:25:44', NULL);
INSERT INTO `sys_user` VALUES ('01k8031gfr5adevd5tc3n3pamv', 'xddcodec', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '459102951@qq.com', '小叮咚', 'https://api.dicebear.com/7.x/avataaars/svg?seed=xddcodec', 0, '2025-10-20 13:56:27', '2025-10-20 14:39:33', '2025-10-20 14:39:33');

-- ----------------------------
-- Table structure for user_quota_usage
-- ----------------------------
DROP TABLE IF EXISTS `user_quota_usage`;
CREATE TABLE `user_quota_usage`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
  `storage_used` int NOT NULL COMMENT '已使用存储(GB)',
  `files_count` int NOT NULL COMMENT '文件数量',
  `bandwidth_used_month` bigint NOT NULL COMMENT '带宽使用情况(按月统计)',
  `bandwidth_reset_date` date NULL DEFAULT NULL COMMENT '带宽重置日期',
  `last_calculated_at` datetime NOT NULL COMMENT '最后统计时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户配额使用情况表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_quota_usage
-- ----------------------------
INSERT INTO `user_quota_usage` VALUES (1, '01jrvgs943q0f43h0aa5mjd333', 0, 0, 0, NULL, '2025-09-24 11:03:39', '2025-09-24 11:03:42');

-- ----------------------------
-- Table structure for user_subscription
-- ----------------------------
DROP TABLE IF EXISTS `user_subscription`;
CREATE TABLE `user_subscription`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户id',
  `plan_id` bigint NOT NULL COMMENT '套餐id',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '订阅状态 0-生效中，1-已过期',
  `subscription_date` datetime NOT NULL COMMENT '订阅日期',
  `expire_date` datetime NOT NULL COMMENT '到期日期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户订阅表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_subscription
-- ----------------------------
INSERT INTO `user_subscription` VALUES (1, '01jrvgs943q0f43h0aa5mjd333', 1, 0, '2025-09-24 11:04:21', '2025-10-24 00:00:00');

SET FOREIGN_KEY_CHECKS = 1;
