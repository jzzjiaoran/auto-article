# syntax=docker/dockerfile:1

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 依赖单独一层：pom 不变时命中缓存，加快构建
COPY pom.xml .
RUN mvn -B dependency:go-offline

# 拷贝源码并打包（跳过测试，测试在 CI 流水线单独执行）
COPY src ./src
RUN mvn -B -DskipTests package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# 健康检查所需的 curl
RUN apt-get update && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*

# 非 root 用户运行
RUN groupadd -r app && useradd -r -g app app

# 分层拷贝可执行 jar
COPY --from=build /app/target/auto-article-*.jar app.jar

RUN chown -R app:app /app
USER app

# 应用内端口（容器外经 8081 暴露，见 docker-compose）
EXPOSE 8080

# JVM 容器感知 + 时区
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Shanghai"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
