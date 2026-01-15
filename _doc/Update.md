# Free-Fs Change Log

## V2.0.2 @2026-01-14

- 新增: 文件列表新增文件右键菜单
- 新增: 文件列表新增文件双击预览
- 优化: 优化文件列表视图切换
- 优化: 移除Redisson相关支持，简化Redis的配置


## V2.0.1 @2026-01-12
- 新增: 新增 `@StoragePlugin` 注解，支持声明式定义插件元数据
- 新增: 新增存储插件自动注册功能，应用启动时自动同步插件信息到数据库
- 新增: 新增 `StoragePluginMetadata` DTO，统一管理插件元数据
- 新增: `TEXT`文本类型预览，包含`TXT`、`LOG`、`INI`、`PROPERTIES`、`YAML`、`YML`、`CONF`
- 重构: 重构 `StoragePluginRegistry`，基于注解验证和加载插件
- 重构: 简化 `IStorageOperationService` 接口，移除 `getPlatformIdentifier()` 和 `getConfigSchema()` 方法
- 废弃: 废弃 `StoragePlatformIdentifierEnum` 枚举类，改用 `@StoragePlugin` 注解
- 优化: 统一使用 `StorageUtils.LOCAL_PLATFORM_IDENTIFIER` 常量管理 Local 标识符
- 优化: Local 存储插件简化配置，仅保留必要的注解属性
> **升级注意**: 自定义存储插件需要添加 `@StoragePlugin` 注解才能被系统识别，详见文档。

## v2.0.0-alpha (2026-01-05)

新特性

- 脱胎换骨，全新架构升级
- 支持多存储平台（本地、MinIO、阿里云 OSS 等各类S3体系云存储平台）
- 分片上传 + 断点续传 - 支持 TB 级大文件上传，网络中断后可继续上传
- 秒传功能 - 基于 MD5 双重校验，相同文件秒级完成
- 插件化存储 - SPI 机制热插拔，5 分钟接入一个新存储平台
- 模块化架构 - 清晰的分层设计，易于维护和扩展
- 安全可靠 - 集成SaToken做API认证、文件完整性校验
- 响应式前端，多端适配

## V1.2.6 @2024-07-26
- 升级: `Mybatis-Flex`版本升级到`1.9.4`
- 新增: 新增对`AWS S3`存储平台的支持
- 优化: 优化了项目的部分代码

## V1.2.5 @2024-06-11
- 升级: `SpringBoot`版本升级到`3.3.0`
- 升级: 项目`JDK`版本由1.8升级到17（1.8分支保留，但后续不在维护）
- 升级: `Sa-Token`版本升级到`1.38.0`
- 升级: `Fastjson2`版本升级到`2.0.51`
- 升级: `Hutool`版本升级到`5.8.28`
- 重构: 重构了项目的代码结构，分层更加明确和清晰
- 新增: 新增了对`Minio`存储平台的支持
- 替换: 使用`Mybatis-Flex`替换了`Mybatis-Plus`作为项目的ORM框架
- 替换: 替换了`Mysql`驱动包以支持`Mysql 8.0`以上版本
- 优化: 优化了项目的部分代码，提升了代码的可读性和可维护性
- 移除: 移除了`验证码`的功能
- 修复: 修复多层的文件夹读取时，文件夹为空的问题
- 修复: 修复`SpringBoot3.x`下，`JustAuth`第三方登录Bean装配失败问题



应该优化存储平台，一个用户针对某个存储平台可以配置多个配置，因为存储桶可能不同
