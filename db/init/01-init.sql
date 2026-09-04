-- auto-article 数据库首次初始化脚本
-- 仅在数据卷为空（首次启动 postgres 容器）时执行。
-- 注意：docker-entrypoint-initdb.d 脚本在 POSTGRES_DB 指向的库上执行，
-- 而 POSTGRES_DB=auto_article 已由 postgres 容器自动创建，因此这里无需建库。
-- 表结构由 JPA ddl-auto=update 自动维护，这里仅安装常用扩展。

-- UUID 支持（如实体使用 UUID 主键）
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
