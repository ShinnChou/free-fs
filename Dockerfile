# 基础镜像
FROM bellsoft/liberica-openjdk-rocky:17.0.16-cds

# 维护者
LABEL maintainer="free-fs"

# 挂载点
VOLUME /tmp

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 【核心修改点】
# 因为是多模块，Docker 构建上下文在根目录，所以要指定子模块路径
# 将 fs-admin 下的 jar 包复制为 app.jar
COPY fs-admin/target/fs-admin.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "/app.jar"]