# 部署与运维手册（DevOps）

本文档说明 auto-article 的容器化部署、CI/CD 流水线、健康检查、回滚与监控方案。

## 技术说明

- 数据库实际使用 **PostgreSQL 16**（见 `pom.xml` 中的 `postgresql` 驱动；任务描述中的 “MySQL” 与代码库不符，以代码为准）。
- 应用容器内端口 `8080`，对外映射 `8081`（http://115.29.229.16:8081）。
- 健康检查接口：`GET /actuator/health`（Spring Actuator，已暴露 `health,info`）。

## 目录结构

```
Dockerfile                 # 多阶段构建：maven 构建 → JRE 运行，非 root，含 HEALTHCHECK
.dockerignore              # 排除无关文件，减小构建上下文
docker-compose.yml         # app + db(postgres) 一键编排
db/init/01-init.sql        # 数据库首次初始化（扩展）
.env.example               # 环境变量示例（复制为 .env，勿提交）
.github/workflows/ci-cd.yml# GitHub Actions：构建→测试→镜像→部署→健康检查→回滚
```

## 敏感信息（环境变量，绝不硬编码）

| 变量 | 说明 |
|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | 数据库 |
| `CREDENTIALS_SECRET` | 平台账号凭据加密密钥 |
| `LLM_API_KEY` / `LLM_BASE_URL` / `LLM_MODEL` | 大模型 |
| `APP_CRAWLER_ENABLED` / `APP_CRAWLER_CRON` | 热点采集定时任务 |

本地开发：`cp .env.example .env` 后填入真实值。生产由 GitHub Secrets 注入。

> ⚠️ **必填环境变量**：`POSTGRES_DB`、`POSTGRES_USER`、`POSTGRES_PASSWORD`、`CREDENTIALS_SECRET`、`LLM_API_KEY` 缺失时应用启动直接失败（fail-fast），不再静默使用默认值。生产必须通过环境变量或 secret manager 注入，切勿提交真实值到仓库。
>
> 📌 **生产数据库 schema**：`application-prod.yml` 中 `ddl-auto: validate`，禁止 Hibernate 自动改表；表结构变更须通过脚本/迁移管理（默认 `default` profile 仍为 `update`，仅用于本地开发）。

## 本地启动（含数据库）

```bash
cp .env.example .env       # 填入真实值
docker compose up -d --build
docker compose ps          # 查看健康状态
curl http://localhost:8081/actuator/health
```

## 生产服务器（115.29.229.16）

前置条件：
1. 服务器已安装 Docker 与 Docker Compose 插件。
2. 防火墙/安全组放行 8081 端口。
3. 部署目录 `/opt/auto-article/`。

在 GitHub 仓库 **Settings → Secrets and variables → Actions** 配置：

| Secret | 说明 |
|---|---|
| `DEPLOY_HOST` | `115.29.229.16` |
| `DEPLOY_USER` | SSH 登录用户 |
| `DEPLOY_SSH_KEY` | SSH 私钥 |
| `DEPLOY_PORT` | SSH 端口（默认 22） |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | 数据库 |
| `CREDENTIALS_SECRET` | 凭据加密密钥 |
| `LLM_API_KEY` / `LLM_BASE_URL` / `LLM_MODEL` | 大模型 |

## CI/CD 流水线

触发：`push` 到 `main`、或手动 `workflow_dispatch`。`pull_request` 只跑构建+测试，不部署。

1. **build-and-test**：JDK17 + Maven，`mvn test` 跑单测，`mvn package` 打包，上传测试报告。
2. **deploy**（仅 main）：
   - 用 git 短 SHA 作为镜像标签（可追溯、可回滚）。
   - 构建镜像 → `docker save` 打包 → `scp` 上传 → `ssh` 部署。
   - 写入 `.env`（来自 Secrets，`chmod 600`）。
   - 记录当前版本 → 加载新镜像 → 滚动重建 `app` 容器。
   - 轮询 `/actuator/health`（最多 120s）。
   - **失败自动回滚**到上一个镜像版本，再以非零退出使流水线标红。
   - 成功后 `docker image prune` 清理无用镜像。

## 回滚方案

- **方案1（自动）**：健康检查失败时流水线自动回滚到上一镜像版本。
- **方案2（手动镜像回滚）**：
  ```bash
  cd /opt/auto-article
  sed -i 's/^IMAGE_TAG=.*/IMAGE_TAG=<上一个SHA>/' .env
  docker compose up -d --no-deps --force-recreate app
  ```
- **方案3（代码回滚）**：`git revert <commit>` 推送 main，触发流水线重新部署。

回滚后验证：容器运行、`/actuator/health` 返回 UP、核心页面可访问、日志无报错。

## 监控与日志

- **日志**：应用输出到 stdout，由 Docker 收集：`docker compose logs -f app`。
- **健康检查**：Docker HEALTHCHECK + `docker compose ps` 状态 + `/actuator/health`。
- **资源**：`docker stats` 查看 CPU/内存；可在 compose 中为服务加 `deploy.resources.limits`。
- **磁盘**：流水线部署后 `docker image prune -f`；建议服务器加磁盘使用率 >80% 告警与定期清理。
