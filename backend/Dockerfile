# 文件：Dockerfile
# 多阶段构建：Maven 编译阶段 + JRE 运行阶段
# 构建：docker build -t springboot-template:1.0.0 .
# 运行：docker run --rm -p 8080:8080 --env-file .env springboot-template:1.0.0

# ---------- 构建阶段 ----------
FROM maven:3.9.11-eclipse-temurin-21 AS builder

WORKDIR /build

# 先复制 pom.xml，利用 Docker 层缓存预下载依赖
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline

# 再复制源码并打包
COPY src ./src
RUN mvn -B -q -DskipTests clean package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

# 非 root 用户运行，并提前创建日志目录
RUN groupadd --system spring \
    && useradd --system --gid spring --home-dir /app --shell /usr/sbin/nologin spring \
    && mkdir -p /app/logs \
    && chown -R spring:spring /app

COPY --from=builder --chown=spring:spring /build/target/springboot-template.jar /app/app.jar

USER spring

EXPOSE 8080

# 使用 sh -c 以支持 JAVA_OPTS 环境变量；exec 让 java 成为 PID 1，能正确接收 SIGTERM
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
