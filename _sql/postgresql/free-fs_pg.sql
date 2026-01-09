-- free-fs_pg.sql
-- Date: 17/11/2025 16:57:34

-- ----------------------------
-- Table structure for file_info
-- ----------------------------
DROP TABLE IF EXISTS "file_info";
CREATE TABLE "file_info" (
                             "id" VARCHAR(128) NOT NULL,
                             "object_key" VARCHAR(128) DEFAULT NULL,
                             "original_name" VARCHAR(128) NOT NULL,
                             "display_name" VARCHAR(128) NOT NULL,
                             "suffix" VARCHAR(20) DEFAULT NULL,
                             "size" BIGINT DEFAULT NULL,
                             "mime_type" VARCHAR(128) DEFAULT NULL,
                             "is_dir" BOOLEAN NOT NULL,
                             "parent_id" VARCHAR(128) DEFAULT NULL,
                             "user_id" VARCHAR(128) NOT NULL,
                             "content_md5" TEXT DEFAULT NULL,
                             "storage_platform_setting_id" VARCHAR(128) DEFAULT NULL,
                             "upload_time" TIMESTAMP NOT NULL,
                             "update_time" TIMESTAMP DEFAULT NULL,
                             "last_access_time" TIMESTAMP DEFAULT NULL,
                             "is_deleted" BOOLEAN DEFAULT NULL,
                             "deleted_time" TIMESTAMP DEFAULT NULL,
                             PRIMARY KEY ("id")
);

COMMENT ON TABLE "file_info" IS '文件资源表';
COMMENT ON COLUMN "file_info"."object_key" IS '资源名称';
COMMENT ON COLUMN "file_info"."original_name" IS '资源原始名称';
COMMENT ON COLUMN "file_info"."display_name" IS '资源别名';
COMMENT ON COLUMN "file_info"."suffix" IS '后缀名';
COMMENT ON COLUMN "file_info"."size" IS '大小';
COMMENT ON COLUMN "file_info"."mime_type" IS '存储标准MIME类型';
COMMENT ON COLUMN "file_info"."is_dir" IS '是否目录';
COMMENT ON COLUMN "file_info"."parent_id" IS '父节点ID';
COMMENT ON COLUMN "file_info"."user_id" IS '用户id';
COMMENT ON COLUMN "file_info"."content_md5" IS '用于秒传和文件校验';
COMMENT ON COLUMN "file_info"."storage_platform_setting_id" IS '存储平台标识符';
COMMENT ON COLUMN "file_info"."upload_time" IS '上传时间';
COMMENT ON COLUMN "file_info"."update_time" IS '修改时间';
COMMENT ON COLUMN "file_info"."last_access_time" IS '最后访问时间';
COMMENT ON COLUMN "file_info"."is_deleted" IS '软删除标记，回收站标识0：未删除 1：已删除';
COMMENT ON COLUMN "file_info"."deleted_time" IS '删除时间';

-- ----------------------------
-- Table structure for file_share_access_record
-- ----------------------------
DROP TABLE IF EXISTS "file_share_access_record";
CREATE TABLE "file_share_access_record" (
                                            "id" BIGSERIAL NOT NULL,
                                            "share_id" VARCHAR(128) NOT NULL,
                                            "access_ip" VARCHAR(50) DEFAULT NULL,
                                            "access_address" VARCHAR(255) DEFAULT NULL,
                                            "browser" VARCHAR(255) DEFAULT NULL,
                                            "os" VARCHAR(512) DEFAULT NULL,
                                            "access_time" TIMESTAMP NOT NULL,
                                            PRIMARY KEY ("id")
);

COMMENT ON TABLE "file_share_access_record" IS '分享页面访问记录表';
COMMENT ON COLUMN "file_share_access_record"."share_id" IS '分享ID';
COMMENT ON COLUMN "file_share_access_record"."access_ip" IS '访问IP';
COMMENT ON COLUMN "file_share_access_record"."access_address" IS '访问地址';
COMMENT ON COLUMN "file_share_access_record"."browser" IS '浏览器类型';
COMMENT ON COLUMN "file_share_access_record"."os" IS '操作系统';
COMMENT ON COLUMN "file_share_access_record"."access_time" IS '访问时间';

-- ----------------------------
-- Table structure for file_share_items
-- ----------------------------
DROP TABLE IF EXISTS "file_share_items";
CREATE TABLE "file_share_items" (
                                    "share_id" VARCHAR(128) NOT NULL,
                                    "file_id" VARCHAR(128) NOT NULL,
                                    "created_at" TIMESTAMP NOT NULL,
                                    PRIMARY KEY ("share_id", "file_id")
);

COMMENT ON TABLE "file_share_items" IS '分享文件关联表';
COMMENT ON COLUMN "file_share_items"."share_id" IS '分享ID';
COMMENT ON COLUMN "file_share_items"."file_id" IS '文件/文件夹ID';
COMMENT ON COLUMN "file_share_items"."created_at" IS '创建时间';

-- ----------------------------
-- Table structure for file_shares
-- ----------------------------
DROP TABLE IF EXISTS "file_shares";
CREATE TABLE "file_shares" (
                               "id" VARCHAR(128) NOT NULL,
                               "user_id" VARCHAR(128) NOT NULL,
                               "share_name" VARCHAR(255) NOT NULL,
                               "share_code" VARCHAR(6) DEFAULT NULL,
                               "expire_time" TIMESTAMP DEFAULT NULL,
                               "scope" VARCHAR(255) NOT NULL,
                               "view_count" INTEGER DEFAULT 0,
                               "max_view_count" INTEGER DEFAULT NULL,
                               "download_count" INTEGER DEFAULT 0,
                               "max_download_count" INTEGER DEFAULT NULL,
                               "created_at" TIMESTAMP NOT NULL,
                               "updated_at" TIMESTAMP NOT NULL,
                               PRIMARY KEY ("id")
);

COMMENT ON TABLE "file_shares" IS '文件分享表';
COMMENT ON COLUMN "file_shares"."id" IS '分享ID';
COMMENT ON COLUMN "file_shares"."user_id" IS '分享人ID';
COMMENT ON COLUMN "file_shares"."share_name" IS '分享名称';
COMMENT ON COLUMN "file_shares"."share_code" IS '提取码（可为空）';
COMMENT ON COLUMN "file_shares"."expire_time" IS '过期时间（null表示永久有效）';
COMMENT ON COLUMN "file_shares"."scope" IS '权限范围: preview,download  (逗号分隔)';
COMMENT ON COLUMN "file_shares"."view_count" IS '查看次数统计';
COMMENT ON COLUMN "file_shares"."max_view_count" IS '最大查看次数（NULL表示无限制）';
COMMENT ON COLUMN "file_shares"."download_count" IS '下载次数统计';
COMMENT ON COLUMN "file_shares"."max_download_count" IS '最大下载次数（NULL表示无限制）';

-- ----------------------------
-- Table structure for file_transfer_task
-- ----------------------------
DROP TABLE IF EXISTS "file_transfer_task";
CREATE TABLE "file_transfer_task" (
                                      "id" BIGSERIAL NOT NULL,
                                      "task_id" VARCHAR(64) NOT NULL,
                                      "upload_id" VARCHAR(255) DEFAULT NULL,
                                      "parent_id" VARCHAR(128) DEFAULT NULL,
                                      "user_id" VARCHAR(128) NOT NULL,
                                      "storage_platform_setting_id" VARCHAR(255) DEFAULT NULL,
                                      "object_key" VARCHAR(255) NOT NULL,
                                      "file_id" VARCHAR(128) DEFAULT NULL,
                                      "file_name" VARCHAR(255) NOT NULL,
                                      "file_size" BIGINT NOT NULL,
                                      "file_md5" VARCHAR(64) DEFAULT NULL,
                                      "suffix" VARCHAR(50) NOT NULL,
                                      "mime_type" VARCHAR(255) NOT NULL,
                                      "total_chunks" INTEGER NOT NULL,
                                      "task_type" VARCHAR(32) DEFAULT NULL,
                                      "uploaded_chunks" INTEGER DEFAULT 0,
                                      "chunk_size" BIGINT DEFAULT 5242880,
                                      "uploaded_size" BIGINT DEFAULT 0,
                                      "status" VARCHAR(20) NOT NULL DEFAULT 'uploading',
                                      "error_msg" VARCHAR(500) DEFAULT NULL,
                                      "start_time" TIMESTAMP NOT NULL,
                                      "complete_time" TIMESTAMP DEFAULT NULL,
                                      "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "uk_task_id" ON "file_transfer_task" ("task_id");
CREATE INDEX "idx_user_id" ON "file_transfer_task" ("user_id");
CREATE INDEX "idx_file_md5" ON "file_transfer_task" ("file_md5");
CREATE INDEX "idx_status" ON "file_transfer_task" ("status");
CREATE INDEX "idx_create_time" ON "file_transfer_task" ("created_at");

COMMENT ON TABLE "file_transfer_task" IS '传输任务表';
COMMENT ON COLUMN "file_transfer_task"."id" IS '主键ID';
COMMENT ON COLUMN "file_transfer_task"."task_id" IS '任务ID(UUID)';
COMMENT ON COLUMN "file_transfer_task"."upload_id" IS '上传唯一ID';
COMMENT ON COLUMN "file_transfer_task"."parent_id" IS '父ID';
COMMENT ON COLUMN "file_transfer_task"."user_id" IS '用户ID';
COMMENT ON COLUMN "file_transfer_task"."storage_platform_setting_id" IS '存储平台配置ID';
COMMENT ON COLUMN "file_transfer_task"."object_key" IS '对象key';
COMMENT ON COLUMN "file_transfer_task"."file_id" IS '下载时关联的文件ID';
COMMENT ON COLUMN "file_transfer_task"."file_name" IS '文件名';
COMMENT ON COLUMN "file_transfer_task"."file_size" IS '文件大小(字节)';
COMMENT ON COLUMN "file_transfer_task"."file_md5" IS '文件MD5值';
COMMENT ON COLUMN "file_transfer_task"."suffix" IS '文件类型(扩展名)';
COMMENT ON COLUMN "file_transfer_task"."mime_type" IS '存储标准MIME类型';
COMMENT ON COLUMN "file_transfer_task"."total_chunks" IS '总分片数';
COMMENT ON COLUMN "file_transfer_task"."task_type" IS '任务类型';
COMMENT ON COLUMN "file_transfer_task"."uploaded_chunks" IS '已上传分片数';
COMMENT ON COLUMN "file_transfer_task"."chunk_size" IS '分片大小(默认5MB)';
COMMENT ON COLUMN "file_transfer_task"."uploaded_size" IS '已上传大小(字节)';
COMMENT ON COLUMN "file_transfer_task"."status" IS '状态';
COMMENT ON COLUMN "file_transfer_task"."error_msg" IS '错误信息';
COMMENT ON COLUMN "file_transfer_task"."start_time" IS '开始时间';
COMMENT ON COLUMN "file_transfer_task"."complete_time" IS '完成时间';
COMMENT ON COLUMN "file_transfer_task"."created_at" IS '创建时间';
COMMENT ON COLUMN "file_transfer_task"."updated_at" IS '更新时间';

-- 创建触发器函数来更新 updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_file_transfer_task_updated_at BEFORE UPDATE
    ON "file_transfer_task" FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ----------------------------
-- Table structure for file_user_favorites
-- ----------------------------
DROP TABLE IF EXISTS "file_user_favorites";
CREATE TABLE "file_user_favorites" (
                                       "user_id" VARCHAR(128) NOT NULL,
                                       "file_id" VARCHAR(128) NOT NULL,
                                       "favorite_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       PRIMARY KEY ("user_id", "file_id")
);

CREATE INDEX "idx_file_time" ON "file_user_favorites" ("file_id", "favorite_time" DESC);

COMMENT ON TABLE "file_user_favorites" IS '文件收藏表';
COMMENT ON COLUMN "file_user_favorites"."user_id" IS '用户ID';
COMMENT ON COLUMN "file_user_favorites"."file_id" IS '文件ID';
COMMENT ON COLUMN "file_user_favorites"."favorite_time" IS '收藏时间';

-- ----------------------------
-- Table structure for storage_platform
-- ----------------------------
DROP TABLE IF EXISTS "storage_platform";
CREATE TABLE "storage_platform" (
                                    "id" SERIAL NOT NULL,
                                    "name" VARCHAR(255) NOT NULL,
                                    "identifier" VARCHAR(128) NOT NULL,
                                    "config_scheme" JSONB NOT NULL,
                                    "icon" VARCHAR(128) DEFAULT NULL,
                                    "link" VARCHAR(255) DEFAULT NULL,
                                    "is_default" BOOLEAN NOT NULL DEFAULT true,
                                    "desc" VARCHAR(255) DEFAULT NULL,
                                    PRIMARY KEY ("id")
);

COMMENT ON TABLE "storage_platform" IS '存储平台';
COMMENT ON COLUMN "storage_platform"."id" IS '存储平台';
COMMENT ON COLUMN "storage_platform"."name" IS '存储平台名称';
COMMENT ON COLUMN "storage_platform"."identifier" IS '存储平台标识符';
COMMENT ON COLUMN "storage_platform"."config_scheme" IS '存储平台配置描述schema';
COMMENT ON COLUMN "storage_platform"."icon" IS '存储平台图标';
COMMENT ON COLUMN "storage_platform"."link" IS '存储平台链接';
COMMENT ON COLUMN "storage_platform"."is_default" IS '是否默认存储平台 0-否 1-是';
COMMENT ON COLUMN "storage_platform"."desc" IS '存储平台描述';

-- ----------------------------
-- Records of storage_platform
-- ----------------------------
INSERT INTO "storage_platform" VALUES
                                   (1, '阿里云OSS', 'AliyunOSS', '[{"label": "Access-Key", "dataType": "string", "identifier": "accessKey", "validation": {"required": true}}, {"label": "Secret-key", "dataType": "string", "identifier": "secretKey", "validation": {"required": true}}, {"label": "服务器端点", "dataType": "string", "identifier": "endpoint", "validation": {"required": true}}, {"label": "存储桶名", "dataType": "string", "identifier": "bucket", "validation": {"required": true}}, {"label": "区域", "dataType": "string", "identifier": "region", "validation": {"required": true}}]', 'icon-aliyun1', 'https://www.aliyun.com/product/oss?utm_content=se_1020894540', false, '阿里云对象存储 OSS（Object Storage Service）是一款海量、安全、低成本、高可靠的云存储服务'),
                                   (2, 'RustFS', 'RustFS', '[{"label": "Access-Key", "dataType": "string", "identifier": "accessKey", "validation": {"required": true}}, {"label": "Secret-key", "dataType": "string", "identifier": "secretKey", "validation": {"required": true}}, {"label": "服务器端点", "dataType": "string", "identifier": "endpoint", "validation": {"required": true}}, {"label": "存储桶名", "dataType": "string", "identifier": "bucket", "validation": {"required": true}}]', 'icon-bendicunchu1', 'https://rustfs.com.cn', false, 'RustFS 是一个基于 Rust 构建的高性能分布式对象存储系统。Rust 是全球最受开发者喜爱的编程语言之一，RustFS 完美结合了 MinIO 的简洁性与 Rust 的内存安全及高性能优势。它提供完整的 S3 兼容性，完全开源，并专为数据湖、人工智能（AI）和大数据负载进行了优化。'),
                                   (3, '七牛云', 'Kodo', '[{"label": "Access-Key", "dataType": "string", "identifier": "accessKey", "validation": {"required": true}}, {"label": "Secret-key", "dataType": "string", "identifier": "secretKey", "validation": {"required": true}}, {"label": "服务器端点", "dataType": "string", "identifier": "endpoint", "validation": {"required": true}}, {"label": "存储桶名", "dataType": "string", "identifier": "bucket", "validation": {"required": true}}]', 'icon-normal-logo-blue', 'https://www.qiniu.com/products/kodo', false, '七牛云海量存储系统（Kodo）是自主研发的非结构化数据存储管理平台，支持中心和边缘存储。');

-- 重置序列
SELECT setval('storage_platform_id_seq', 3, true);

-- ----------------------------
-- Table structure for storage_settings
-- ----------------------------
DROP TABLE IF EXISTS "storage_settings";
CREATE TABLE "storage_settings" (
                                    "id" VARCHAR(128) NOT NULL,
                                    "platform_identifier" VARCHAR(128) NOT NULL,
                                    "config_data" JSONB NOT NULL,
                                    "enabled" BOOLEAN NOT NULL DEFAULT false,
                                    "user_id" VARCHAR(128) NOT NULL,
                                    "created_at" TIMESTAMP DEFAULT NULL,
                                    "updated_at" TIMESTAMP DEFAULT NULL,
                                    "remark" VARCHAR(255) DEFAULT NULL,
                                    "deleted" BOOLEAN DEFAULT false,
                                    PRIMARY KEY ("id")
);

COMMENT ON TABLE "storage_settings" IS '存储平台配置';
COMMENT ON COLUMN "storage_settings"."id" IS 'id';
COMMENT ON COLUMN "storage_settings"."platform_identifier" IS '存储平台标识符';
COMMENT ON COLUMN "storage_settings"."config_data" IS '存储配置';
COMMENT ON COLUMN "storage_settings"."enabled" IS '是否启用 0：否 1：是';
COMMENT ON COLUMN "storage_settings"."user_id" IS '所属用户';
COMMENT ON COLUMN "storage_settings"."created_at" IS '创建时间';
COMMENT ON COLUMN "storage_settings"."updated_at" IS '更新时间';
COMMENT ON COLUMN "storage_settings"."remark" IS '备注';
COMMENT ON COLUMN "storage_settings"."deleted" IS '逻辑删除 0未删除 1已删除';

-- ----------------------------
-- Table structure for subscription_plan
-- ----------------------------
DROP TABLE IF EXISTS "subscription_plan";
CREATE TABLE "subscription_plan" (
                                     "id" BIGSERIAL NOT NULL,
                                     "plan_code" VARCHAR(50) NOT NULL,
                                     "plan_name" VARCHAR(100) NOT NULL,
                                     "description" TEXT DEFAULT NULL,
                                     "storage_quota_gb" INTEGER NOT NULL,
                                     "max_files" INTEGER NOT NULL,
                                     "max_file_size" BIGINT NOT NULL,
                                     "bandwidth_quota" BIGINT NOT NULL,
                                     "price" NUMERIC(8, 2) NOT NULL,
                                     "is_active" BOOLEAN NOT NULL DEFAULT true,
                                     "is_default" BOOLEAN NOT NULL,
                                     "sort_order" INTEGER NOT NULL,
                                     "created_at" TIMESTAMP NOT NULL,
                                     "updated_at" TIMESTAMP NOT NULL,
                                     "del_flag" BOOLEAN NOT NULL DEFAULT false,
                                     PRIMARY KEY ("id")
);

COMMENT ON TABLE "subscription_plan" IS '套餐表';
COMMENT ON COLUMN "subscription_plan"."plan_code" IS '套餐代码';
COMMENT ON COLUMN "subscription_plan"."plan_name" IS '套餐名称';
COMMENT ON COLUMN "subscription_plan"."description" IS '套餐描述';
COMMENT ON COLUMN "subscription_plan"."storage_quota_gb" IS '存储配额(GB)';
COMMENT ON COLUMN "subscription_plan"."max_files" IS '最大文件数';
COMMENT ON COLUMN "subscription_plan"."max_file_size" IS '单个文件最大大小(字节)';
COMMENT ON COLUMN "subscription_plan"."bandwidth_quota" IS '每月带宽配额(字节)';
COMMENT ON COLUMN "subscription_plan"."price" IS '价格/月';
COMMENT ON COLUMN "subscription_plan"."is_active" IS '是否启用0否1是';
COMMENT ON COLUMN "subscription_plan"."is_default" IS '是否为默认套餐 0否1是';
COMMENT ON COLUMN "subscription_plan"."sort_order" IS '排序';
COMMENT ON COLUMN "subscription_plan"."created_at" IS '创建时间';
COMMENT ON COLUMN "subscription_plan"."updated_at" IS '更新时间';
COMMENT ON COLUMN "subscription_plan"."del_flag" IS '是否删除 0否1是';

-- ----------------------------
-- Table structure for sys_login_log
-- ----------------------------
DROP TABLE IF EXISTS "sys_login_log";
CREATE TABLE "sys_login_log" (
                                 "id" BIGSERIAL NOT NULL,
                                 "user_id" VARCHAR(100) DEFAULT NULL,
                                 "username" VARCHAR(50) NOT NULL DEFAULT '',
                                 "login_ip" VARCHAR(50) NOT NULL,
                                 "login_address" VARCHAR(255) DEFAULT NULL,
                                 "browser" VARCHAR(255) DEFAULT NULL,
                                 "os" VARCHAR(512) NOT NULL,
                                 "status" SMALLINT NOT NULL,
                                 "msg" VARCHAR(255) NOT NULL,
                                 "login_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 PRIMARY KEY ("id")
);

COMMENT ON TABLE "sys_login_log" IS '系统访问记录';
COMMENT ON COLUMN "sys_login_log"."id" IS '访问ID';
COMMENT ON COLUMN "sys_login_log"."user_id" IS '用户编号';
COMMENT ON COLUMN "sys_login_log"."username" IS '用户账号';
COMMENT ON COLUMN "sys_login_log"."login_ip" IS '登录IP';
COMMENT ON COLUMN "sys_login_log"."login_address" IS '登录地址';
COMMENT ON COLUMN "sys_login_log"."browser" IS '浏览器类型';
COMMENT ON COLUMN "sys_login_log"."os" IS '操作系统';
COMMENT ON COLUMN "sys_login_log"."status" IS '登录状态（0成功 1失败）';
COMMENT ON COLUMN "sys_login_log"."msg" IS '提示消息';
COMMENT ON COLUMN "sys_login_log"."login_time" IS '登录时间';

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS "sys_user";
CREATE TABLE "sys_user" (
                            "id" VARCHAR(128) NOT NULL,
                            "username" VARCHAR(128) NOT NULL,
                            "password" VARCHAR(128) NOT NULL,
                            "email" VARCHAR(128) NOT NULL,
                            "nickname" VARCHAR(128) NOT NULL,
                            "avatar" VARCHAR(255) DEFAULT NULL,
                            "status" INTEGER NOT NULL DEFAULT 0,
                            "created_at" TIMESTAMP NOT NULL,
                            "updated_at" TIMESTAMP NOT NULL,
                            "last_login_at" TIMESTAMP DEFAULT NULL,
                            PRIMARY KEY ("id")
);

COMMENT ON TABLE "sys_user" IS '用户表';
COMMENT ON COLUMN "sys_user"."id" IS '用户ID';
COMMENT ON COLUMN "sys_user"."username" IS '用户名';
COMMENT ON COLUMN "sys_user"."password" IS '密码';
COMMENT ON COLUMN "sys_user"."email" IS '邮箱';
COMMENT ON COLUMN "sys_user"."nickname" IS '昵称';
COMMENT ON COLUMN "sys_user"."avatar" IS '头像';
COMMENT ON COLUMN "sys_user"."status" IS '用户状态 0正常 1禁用';
COMMENT ON COLUMN "sys_user"."created_at" IS '创建时间';
COMMENT ON COLUMN "sys_user"."updated_at" IS '更新时间';
COMMENT ON COLUMN "sys_user"."last_login_at" IS '最后登录时间';

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO "sys_user" VALUES
    ('01jrvgs943q0f43h0aa5mjde0y', 'admin', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', '459102951@qq.com', '丁大圣33', 'https://csdn-665-inscode.s3.cn-north-1.jdcloud-oss.com/inscode/202303/628c9f991a7e4862742d8a2f/1680072908255-49035150-ttVQUH7YUEaCdHRZenaoQrUQPxtaBUay/large', 0, '2025-04-15 09:25:22', '2025-11-17 14:05:14', '2025-11-17 14:05:14');

-- ----------------------------
-- Table structure for sys_user_transfer_setting
-- ----------------------------
DROP TABLE IF EXISTS "sys_user_transfer_setting";
CREATE TABLE "sys_user_transfer_setting" (
                                             "id" BIGSERIAL NOT NULL,
                                             "user_id" VARCHAR(128) NOT NULL,
                                             "download_location" VARCHAR(255) DEFAULT NULL,
                                             "is_default_download_location" BOOLEAN NOT NULL DEFAULT false,
                                             "download_speed_limit" INTEGER NOT NULL DEFAULT 5,
                                             "concurrent_upload_quantity" INTEGER NOT NULL DEFAULT 1,
                                             "concurrent_download_quantity" INTEGER NOT NULL DEFAULT 1,
                                             "created_at" TIMESTAMP NOT NULL,
                                             "updated_at" TIMESTAMP NOT NULL,
                                             PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "uk_user_id" ON "sys_user_transfer_setting" ("user_id");

COMMENT ON TABLE "sys_user_transfer_setting" IS '用户传输设置';
COMMENT ON COLUMN "sys_user_transfer_setting"."user_id" IS '用户ID';
COMMENT ON COLUMN "sys_user_transfer_setting"."download_location" IS '文件下载位置';
COMMENT ON COLUMN "sys_user_transfer_setting"."is_default_download_location" IS '是否默认该路径为下载路径，如果否则每次下载询问保存地址';
COMMENT ON COLUMN "sys_user_transfer_setting"."download_speed_limit" IS '下载速率限制 单位：MB/S';
COMMENT ON COLUMN "sys_user_transfer_setting"."concurrent_upload_quantity" IS '并发上传数量';
COMMENT ON COLUMN "sys_user_transfer_setting"."concurrent_download_quantity" IS '并发下载数量';
COMMENT ON COLUMN "sys_user_transfer_setting"."created_at" IS '创建时间';
COMMENT ON COLUMN "sys_user_transfer_setting"."updated_at" IS '修改时间';

COMMENT ON INDEX "uk_user_id" IS '用户ID唯一索引';

-- ----------------------------
-- Records of sys_user_transfer_setting
-- ----------------------------
INSERT INTO "sys_user_transfer_setting" VALUES
    (1, '01jrvgs943q0f43h0aa5mjde0y', NULL, false, 5, 1, 1, '2025-11-11 14:45:27', '2025-11-11 14:45:29');

SELECT setval('sys_user_transfer_setting_id_seq', 1, true);

-- ----------------------------
-- Table structure for user_quota_usage
-- ----------------------------
DROP TABLE IF EXISTS "user_quota_usage";
CREATE TABLE "user_quota_usage" (
                                    "id" BIGSERIAL NOT NULL,
                                    "user_id" VARCHAR(128) NOT NULL,
                                    "storage_used" INTEGER NOT NULL,
                                    "files_count" INTEGER NOT NULL,
                                    "bandwidth_used_month" BIGINT NOT NULL,
                                    "bandwidth_reset_date" DATE DEFAULT NULL,
                                    "last_calculated_at" TIMESTAMP NOT NULL,
                                    "updated_at" TIMESTAMP NOT NULL,
                                    PRIMARY KEY ("id")
);

COMMENT ON TABLE "user_quota_usage" IS '用户配额使用情况表';
COMMENT ON COLUMN "user_quota_usage"."user_id" IS '用户ID';
COMMENT ON COLUMN "user_quota_usage"."storage_used" IS '已使用存储(GB)';
COMMENT ON COLUMN "user_quota_usage"."files_count" IS '文件数量';
COMMENT ON COLUMN "user_quota_usage"."bandwidth_used_month" IS '带宽使用情况(按月统计)';
COMMENT ON COLUMN "user_quota_usage"."bandwidth_reset_date" IS '带宽重置日期';
COMMENT ON COLUMN "user_quota_usage"."last_calculated_at" IS '最后统计时间';
COMMENT ON COLUMN "user_quota_usage"."updated_at" IS '更新时间';

-- ----------------------------
-- Table structure for user_subscription
-- ----------------------------
DROP TABLE IF EXISTS "user_subscription";
CREATE TABLE "user_subscription" (
                                     "id" BIGSERIAL NOT NULL,
                                     "user_id" VARCHAR(128) NOT NULL,
                                     "plan_id" BIGINT NOT NULL,
                                     "status" BOOLEAN NOT NULL DEFAULT false,
                                     "subscription_date" TIMESTAMP NOT NULL,
                                     "expire_date" TIMESTAMP NOT NULL,
                                     PRIMARY KEY ("id")
);

COMMENT ON TABLE "user_subscription" IS '用户订阅表';
COMMENT ON COLUMN "user_subscription"."user_id" IS '租户id';
COMMENT ON COLUMN "user_subscription"."plan_id" IS '套餐id';
COMMENT ON COLUMN "user_subscription"."status" IS '订阅状态 0-生效中，1-已过期';
COMMENT ON COLUMN "user_subscription"."subscription_date" IS '订阅日期';
COMMENT ON COLUMN "user_subscription"."expire_date" IS '到期日期';
