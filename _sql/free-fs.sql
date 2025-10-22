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

 Date: 20/10/2025 16:30:50
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for file_chunk_info
-- ----------------------------
DROP TABLE IF EXISTS `file_chunk_info`;
CREATE TABLE `file_chunk_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `chunk_number` int NULL DEFAULT NULL COMMENT '文件块编号',
  `chunk_size` bigint NULL DEFAULT NULL COMMENT '分块大小',
  `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `current_chunk_size` bigint NULL DEFAULT NULL,
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `identifier` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `total_chunks` int NULL DEFAULT NULL,
  `total_size` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of file_chunk_info
-- ----------------------------

-- ----------------------------
-- Table structure for file_info
-- ----------------------------
DROP TABLE IF EXISTS `file_info`;
CREATE TABLE `file_info`  (
  `id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `object_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '资源名称',
  `original_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '资源原始名称',
  `display_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '资源别名',
  `suffix` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '后缀名',
  `size` bigint NULL DEFAULT NULL COMMENT '大小',
  `mime_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '存储标准MIME类型',
  `is_dir` tinyint(1) NULL DEFAULT NULL COMMENT '是否目录',
  `parent_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户id',
  `content_md5` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '用于秒传和文件校验',
  `storage_platform_identifier` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '存储平台标识符',
  `upload_time` datetime NULL DEFAULT NULL COMMENT '上传时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `is_deleted` tinyint(1) NULL DEFAULT NULL COMMENT '软删除标记，回收站标识0：未删除 1：已删除',
  `deleted_time` datetime NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文件资源表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of file_info
-- ----------------------------
INSERT INTO `file_info` VALUES ('0f7f3bd455b44acc8da2911ef6829da7', NULL, '测试(2)', '测试(2)', NULL, NULL, NULL, 1, '', '01jrvgs943q0f43h0aa5mjde0y', NULL, 'Local', '2025-05-13 08:55:32', '2025-05-13 08:55:32', 0, NULL);
INSERT INTO `file_info` VALUES ('4ac585753b144a5ca36d5242fd0d3bd6', NULL, '测试', '测试', NULL, NULL, NULL, 1, '807f3f93219a4b1899c2359f2efc9314', '01jrvgs943q0f43h0aa5mjde0y', NULL, 'Local', '2025-05-13 08:56:01', '2025-05-13 08:56:01', 0, NULL);
INSERT INTO `file_info` VALUES ('538b63f877fa4e078af97c7fc98d57bc', 'user/01jrvgs943q0f43h0aa5mjde0y/538b63f877fa4e078af97c7fc98d57bc.docx', '申请材料.docx', '申请材料.docx', 'docx', 29428, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 0, NULL, '01jrvgs943q0f43h0aa5mjde0y', '43c78fb14a1a1e3758e46f6190c0891d', 'Local', '2025-05-12 13:28:02', '2025-05-12 13:28:02', 0, NULL);
INSERT INTO `file_info` VALUES ('807f3f93219a4b1899c2359f2efc9314', NULL, '测试(1)', '测试(1)', NULL, NULL, NULL, 1, '', '01jrvgs943q0f43h0aa5mjde0y', NULL, 'Local', '2025-05-13 08:55:25', '2025-05-13 08:55:25', 0, NULL);
INSERT INTO `file_info` VALUES ('fc87e62e51d94b14b3bdb1f70f4b074c', NULL, '测试', '测试', NULL, NULL, NULL, 1, '', '01jrvgs943q0f43h0aa5mjde0y', NULL, 'Local', '2025-05-13 08:55:16', '2025-05-13 08:55:16', 0, NULL);

-- ----------------------------
-- Table structure for file_shares
-- ----------------------------
DROP TABLE IF EXISTS `file_shares`;
CREATE TABLE `file_shares`  (
  `id` int NOT NULL,
  `file_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `share_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `share_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `expire_time` datetime NULL DEFAULT NULL,
  `access_count` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `max_access_count` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `allow_download` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `allow_preview` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '文件分享表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of file_shares
-- ----------------------------

-- ----------------------------
-- Table structure for file_user_favorites
-- ----------------------------
DROP TABLE IF EXISTS `file_user_favorites`;
CREATE TABLE `file_user_favorites`  (
  `user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
  `file_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件ID',
  `favorite_time` datetime NOT NULL COMMENT '收藏时间',
  PRIMARY KEY (`user_id`, `file_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '收藏夹' ROW_FORMAT = Dynamic;

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
INSERT INTO `storage_platform` VALUES (1, '阿里云OSS', 'AliyunOSS', '[{\"label\": \"Access-Key\", \"dataType\": \"string\", \"identifier\": \"accessKey\", \"validation\": {\"required\": true}}, {\"label\": \"Secret-key\", \"dataType\": \"string\", \"identifier\": \"secretKey\", \"validation\": {\"required\": true}}, {\"label\": \"服务器端点\", \"dataType\": \"string\", \"identifier\": \"endpoint\", \"validation\": {\"required\": true}}, {\"label\": \"存储桶名\", \"dataType\": \"string\", \"identifier\": \"bucket\", \"validation\": {\"required\": true}}]', 'icon-aliyun1', 'https://www.aliyun.com/product/oss?utm_content=se_1020894540', 0, '阿里云对象存储 OSS（Object Storage Service）是一款海量、安全、低成本、高可靠的云存储服务');
INSERT INTO `storage_platform` VALUES (2, 'Minio', 'Minio', '[{\"label\": \"Access-Key\", \"dataType\": \"string\", \"identifier\": \"accessKey\", \"validation\": {\"required\": true}}, {\"label\": \"Secret-key\", \"dataType\": \"string\", \"identifier\": \"secretKey\", \"validation\": {\"required\": true}}, {\"label\": \"服务器端点\", \"dataType\": \"string\", \"identifier\": \"endpoint\", \"validation\": {\"required\": true}}, {\"label\": \"存储桶名\", \"dataType\": \"string\", \"identifier\": \"bucket\", \"validation\": {\"required\": true}}]', 'icon-Minio1', 'https://www.minio.org.cn/?bd_vid=10111900197314796808', 0, 'MinIO 是一种高性能、S3 兼容的对象存储。');
INSERT INTO `storage_platform` VALUES (3, '七牛云', 'Kodo', '[{\"label\": \"Access-Key\", \"dataType\": \"string\", \"identifier\": \"accessKey\", \"validation\": {\"required\": true}}, {\"label\": \"Secret-key\", \"dataType\": \"string\", \"identifier\": \"secretKey\", \"validation\": {\"required\": true}}, {\"label\": \"服务器端点\", \"dataType\": \"string\", \"identifier\": \"endpoint\", \"validation\": {\"required\": true}}, {\"label\": \"存储桶名\", \"dataType\": \"string\", \"identifier\": \"bucket\", \"validation\": {\"required\": true}}]', 'icon-normal-logo-blue', 'https://www.qiniu.com/products/kodo', 0, '七牛云海量存储系统（Kodo）是自主研发的非结构化数据存储管理平台，支持中心和边缘存储。');

-- ----------------------------
-- Table structure for storage_settings
-- ----------------------------
DROP TABLE IF EXISTS `storage_settings`;
CREATE TABLE `storage_settings`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `platform_identifier` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '存储平台标识符',
  `config_data` json NOT NULL COMMENT '存储配置',
  `enabled` tinyint(1) NOT NULL COMMENT '是否启用 0：否 1：是',
  `user_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '所属用户',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '存储平台配置' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of storage_settings
-- ----------------------------
INSERT INTO `storage_settings` VALUES (1, 'Minio', '{\"bucket\": \"test222\", \"endpoint\": \"http://116.62.112.146:9090\", \"accessKey\": \"kNKaJVWM36fP2xewIC3G\", \"secretKey\": \"n3BTy9ntg4nmlJa6Y81dZkh7r9kyoL75VVdTZFMH\", \"identifier\": \"Minio\"}', 1, '01jrvgs943q0f43h0aa5mjde0y', '2024-02-02 13:32:15', '2025-10-20 16:23:09');
INSERT INTO `storage_settings` VALUES (2, 'AliyunOSS', '{}', 1, '01jrvgs943q0f43h0aa5mjde0y', '2025-04-27 09:02:13', '2025-10-20 16:21:18');
INSERT INTO `storage_settings` VALUES (5, 'AliyunOSS', '{\"bucket\": \"12\", \"endpoint\": \"12\", \"accessKey\": \"12\", \"secretKey\": \"12\", \"identifier\": \"AliyunOSS\"}', 1, '01k8031gfr5adevd5tc3n3pamv', '2025-10-20 14:39:38', '2025-10-20 14:40:06');
INSERT INTO `storage_settings` VALUES (6, 'Kodo', '{\"bucket\": \"2\", \"endpoint\": \"2\", \"accessKey\": \"2\", \"secretKey\": \"2\", \"identifier\": \"Kodo\"}', 1, '01jrvgs943q0f43h0aa5mjde0y', '2025-10-20 15:18:42', '2025-10-20 16:23:10');

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
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户编号',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '用户账号',
  `login_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录IP',
  `login_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '登录地址',
  `browser` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '浏览器类型',
  `os` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作系统',
  `status` tinyint NOT NULL COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提示消息',
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3550 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统访问记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_login_log
-- ----------------------------
INSERT INTO `sys_login_log` VALUES (3445, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-25 15:29:03');
INSERT INTO `sys_login_log` VALUES (3446, NULL, 'admin', '192.168.199.173', '', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 1, '密码不正确', '2025-09-25 15:29:38');
INSERT INTO `sys_login_log` VALUES (3447, NULL, 'admin', '192.168.199.173', '', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 1, '密码不正确', '2025-09-25 15:33:39');
INSERT INTO `sys_login_log` VALUES (3448, NULL, 'admin12', '192.168.199.173', '', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 1, '用户不存在', '2025-09-25 15:34:52');
INSERT INTO `sys_login_log` VALUES (3449, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-25 15:48:31');
INSERT INTO `sys_login_log` VALUES (3450, NULL, 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 1, '密码不正确', '2025-09-25 15:57:17');
INSERT INTO `sys_login_log` VALUES (3451, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-25 16:18:51');
INSERT INTO `sys_login_log` VALUES (3452, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 08:39:27');
INSERT INTO `sys_login_log` VALUES (3453, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 08:54:39');
INSERT INTO `sys_login_log` VALUES (3454, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 09:13:57');
INSERT INTO `sys_login_log` VALUES (3455, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 09:18:41');
INSERT INTO `sys_login_log` VALUES (3456, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 09:43:10');
INSERT INTO `sys_login_log` VALUES (3457, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 09:43:17');
INSERT INTO `sys_login_log` VALUES (3458, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 09:47:01');
INSERT INTO `sys_login_log` VALUES (3459, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 09:47:31');
INSERT INTO `sys_login_log` VALUES (3460, NULL, '12', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 1, '用户不存在', '2025-09-28 09:47:41');
INSERT INTO `sys_login_log` VALUES (3461, NULL, '12', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 1, '用户不存在', '2025-09-28 09:47:41');
INSERT INTO `sys_login_log` VALUES (3462, NULL, '12', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 1, '用户不存在', '2025-09-28 09:47:42');
INSERT INTO `sys_login_log` VALUES (3463, NULL, '12', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 1, '用户不存在', '2025-09-28 09:47:42');
INSERT INTO `sys_login_log` VALUES (3464, NULL, '12', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 1, '用户不存在', '2025-09-28 09:47:42');
INSERT INTO `sys_login_log` VALUES (3465, NULL, 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 1, '密码不正确', '2025-09-28 09:47:45');
INSERT INTO `sys_login_log` VALUES (3466, NULL, 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 1, '密码不正确', '2025-09-28 09:47:45');
INSERT INTO `sys_login_log` VALUES (3467, NULL, 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 1, '密码不正确', '2025-09-28 09:47:45');
INSERT INTO `sys_login_log` VALUES (3468, NULL, 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 1, '密码不正确', '2025-09-28 09:47:46');
INSERT INTO `sys_login_log` VALUES (3469, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 09:48:07');
INSERT INTO `sys_login_log` VALUES (3470, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 09:55:40');
INSERT INTO `sys_login_log` VALUES (3471, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 09:56:24');
INSERT INTO `sys_login_log` VALUES (3472, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 09:57:23');
INSERT INTO `sys_login_log` VALUES (3473, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 10:13:09');
INSERT INTO `sys_login_log` VALUES (3474, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 10:25:22');
INSERT INTO `sys_login_log` VALUES (3475, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 10:32:12');
INSERT INTO `sys_login_log` VALUES (3476, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 10:40:20');
INSERT INTO `sys_login_log` VALUES (3477, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 11:02:01');
INSERT INTO `sys_login_log` VALUES (3478, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 11:11:17');
INSERT INTO `sys_login_log` VALUES (3479, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 11:11:33');
INSERT INTO `sys_login_log` VALUES (3480, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 11:12:15');
INSERT INTO `sys_login_log` VALUES (3481, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 11:14:29');
INSERT INTO `sys_login_log` VALUES (3482, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 14:38:26');
INSERT INTO `sys_login_log` VALUES (3483, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 15:14:07');
INSERT INTO `sys_login_log` VALUES (3484, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 15:14:13');
INSERT INTO `sys_login_log` VALUES (3485, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.173', '0', 'Chrome 140.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-09-28 15:18:05');
INSERT INTO `sys_login_log` VALUES (3486, NULL, 'root', '192.168.199.20', '0', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 1, '用户不存在', '2025-10-11 08:47:23');
INSERT INTO `sys_login_log` VALUES (3487, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-11 08:47:26');
INSERT INTO `sys_login_log` VALUES (3488, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-11 08:50:21');
INSERT INTO `sys_login_log` VALUES (3489, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-14 08:37:34');
INSERT INTO `sys_login_log` VALUES (3490, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-14 08:37:57');
INSERT INTO `sys_login_log` VALUES (3491, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-14 08:57:37');
INSERT INTO `sys_login_log` VALUES (3492, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-14 08:58:23');
INSERT INTO `sys_login_log` VALUES (3493, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-14 08:59:21');
INSERT INTO `sys_login_log` VALUES (3494, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-16 15:23:29');
INSERT INTO `sys_login_log` VALUES (3495, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-16 15:23:35');
INSERT INTO `sys_login_log` VALUES (3496, NULL, 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 1, '密码不正确', '2025-10-20 08:58:31');
INSERT INTO `sys_login_log` VALUES (3497, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 08:58:34');
INSERT INTO `sys_login_log` VALUES (3498, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 08:59:15');
INSERT INTO `sys_login_log` VALUES (3499, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 09:00:12');
INSERT INTO `sys_login_log` VALUES (3500, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 09:17:37');
INSERT INTO `sys_login_log` VALUES (3501, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 09:27:11');
INSERT INTO `sys_login_log` VALUES (3502, NULL, 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 1, '密码不正确', '2025-10-20 10:01:37');
INSERT INTO `sys_login_log` VALUES (3503, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 10:01:41');
INSERT INTO `sys_login_log` VALUES (3504, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 10:05:53');
INSERT INTO `sys_login_log` VALUES (3505, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 10:44:04');
INSERT INTO `sys_login_log` VALUES (3506, '01k5xawhp9mas36qywkgqdk8nj', 'test2', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 10:51:15');
INSERT INTO `sys_login_log` VALUES (3507, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 10:59:06');
INSERT INTO `sys_login_log` VALUES (3508, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 11:00:44');
INSERT INTO `sys_login_log` VALUES (3509, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 11:00:59');
INSERT INTO `sys_login_log` VALUES (3510, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 11:04:38');
INSERT INTO `sys_login_log` VALUES (3511, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 11:06:34');
INSERT INTO `sys_login_log` VALUES (3512, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 11:06:51');
INSERT INTO `sys_login_log` VALUES (3513, NULL, 'demo_', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 1, '用户不存在', '2025-10-20 11:13:21');
INSERT INTO `sys_login_log` VALUES (3514, NULL, 'demo_user', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 1, '用户不存在', '2025-10-20 11:13:30');
INSERT INTO `sys_login_log` VALUES (3515, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 11:13:50');
INSERT INTO `sys_login_log` VALUES (3516, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 11:13:55');
INSERT INTO `sys_login_log` VALUES (3517, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 11:13:58');
INSERT INTO `sys_login_log` VALUES (3518, NULL, 'as', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 1, '用户不存在', '2025-10-20 13:49:12');
INSERT INTO `sys_login_log` VALUES (3519, NULL, 'as', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 1, '用户不存在', '2025-10-20 13:49:13');
INSERT INTO `sys_login_log` VALUES (3520, NULL, 'as', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 1, '用户不存在', '2025-10-20 13:49:25');
INSERT INTO `sys_login_log` VALUES (3521, NULL, 'as', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 1, '用户不存在', '2025-10-20 13:50:05');
INSERT INTO `sys_login_log` VALUES (3522, NULL, 'as', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 1, '用户不存在', '2025-10-20 13:50:35');
INSERT INTO `sys_login_log` VALUES (3523, NULL, 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 1, '密码不正确', '2025-10-20 13:50:39');
INSERT INTO `sys_login_log` VALUES (3524, NULL, 'sd', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 1, '用户不存在', '2025-10-20 13:51:39');
INSERT INTO `sys_login_log` VALUES (3525, NULL, 'sd', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 1, '用户不存在', '2025-10-20 13:51:39');
INSERT INTO `sys_login_log` VALUES (3526, NULL, 'sd', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 1, '用户不存在', '2025-10-20 13:51:40');
INSERT INTO `sys_login_log` VALUES (3527, NULL, 'sdf', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 1, '用户不存在', '2025-10-20 13:51:43');
INSERT INTO `sys_login_log` VALUES (3528, NULL, 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 1, '密码不正确', '2025-10-20 13:51:46');
INSERT INTO `sys_login_log` VALUES (3529, NULL, 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 1, '密码不正确', '2025-10-20 13:51:48');
INSERT INTO `sys_login_log` VALUES (3530, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 13:51:50');
INSERT INTO `sys_login_log` VALUES (3531, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 13:52:23');
INSERT INTO `sys_login_log` VALUES (3532, '01k802znjvk56eg99rjaqyk6ac', 'dinghao', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 13:55:33');
INSERT INTO `sys_login_log` VALUES (3533, '01k8031gfr5adevd5tc3n3pamv', 'xddcodec', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 13:56:37');
INSERT INTO `sys_login_log` VALUES (3534, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 14:30:23');
INSERT INTO `sys_login_log` VALUES (3535, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 14:34:10');
INSERT INTO `sys_login_log` VALUES (3536, '01k8031gfr5adevd5tc3n3pamv', 'xddcodec', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 14:39:33');
INSERT INTO `sys_login_log` VALUES (3537, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 14:54:53');
INSERT INTO `sys_login_log` VALUES (3538, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 14:55:06');
INSERT INTO `sys_login_log` VALUES (3539, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 14:55:33');
INSERT INTO `sys_login_log` VALUES (3540, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 14:55:44');
INSERT INTO `sys_login_log` VALUES (3541, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 14:59:54');
INSERT INTO `sys_login_log` VALUES (3542, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 15:00:58');
INSERT INTO `sys_login_log` VALUES (3543, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 15:02:29');
INSERT INTO `sys_login_log` VALUES (3544, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 15:02:57');
INSERT INTO `sys_login_log` VALUES (3545, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 15:04:31');
INSERT INTO `sys_login_log` VALUES (3546, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 15:56:55');
INSERT INTO `sys_login_log` VALUES (3547, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 16:15:11');
INSERT INTO `sys_login_log` VALUES (3548, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 16:16:01');
INSERT INTO `sys_login_log` VALUES (3549, '01jrvgs943q0f43h0aa5mjde0y', 'admin', '192.168.199.20', '0|0|内网IP|内网IP', 'Chrome 141.0.0.0', 'Windows 10 or Windows Server 2016', 0, '登录成功', '2025-10-20 16:21:10');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户ID',
  `username` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '邮箱',
  `nickname` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '昵称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像',
  `status` int NOT NULL DEFAULT 0 COMMENT '用户状态 0正常 1禁用',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  `last_login_at` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES ('01jrvgs943q0f43h0aa5mjde0y', 'admin', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', '459102951@qq.com', '超级管理员', NULL, 0, '2025-04-15 09:25:22', '2025-10-20 16:21:10', '2025-10-20 16:21:10');
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
