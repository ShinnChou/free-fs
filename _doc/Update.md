# Free-Fs Change Log

## V1.3.0 @2026-01-12
- 新增: 新增 `@StoragePlugin` 注解，支持声明式定义插件元数据
- 新增: 新增存储插件自动注册功能，应用启动时自动同步插件信息到数据库
- 新增: 新增 `StoragePluginMetadata` DTO，统一管理插件元数据
- 重构: 重构 `StoragePluginRegistry`，基于注解验证和加载插件
- 重构: 简化 `IStorageOperationService` 接口，移除 `getPlatformIdentifier()` 和 `getConfigSchema()` 方法
- 废弃: 废弃 `StoragePlatformIdentifierEnum` 枚举类，改用 `@StoragePlugin` 注解
- 优化: 统一使用 `StorageUtils.LOCAL_PLATFORM_IDENTIFIER` 常量管理 Local 标识符
- 优化: Local 存储插件简化配置，仅保留必要的注解属性

> **升级注意**: 自定义存储插件需要添加 `@StoragePlugin` 注解才能被系统识别，详见文档。

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
