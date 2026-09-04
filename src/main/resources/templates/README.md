# 前端模板集成说明 (Thymeleaf)

本文档供 `backend-dev` 在接入控制器时参考，列出每个模板页面对 `Model` 属性（`addObject`）的预期，以及相关路由约定。

所有模板均通过 Thymeleaf **Layout Dialect** 共享布局 `templates/fragments/layout.html`，依赖：

```xml
<dependency>
    <groupId>nz.net.ultraq.thymeleaf</groupId>
    <artifactId>thymeleaf-layout-dialect</artifactId>
</dependency>
```

静态资源放于 `src/main/resources/static/`（`css/app.css`、`js/app.js`），页面通过 `th:href="@{/css/app.css}"` / `th:src="@{/js/app.js}"` 引用。

## 通用约定

- **flash 提示**：控制器执行写操作后通过 `RedirectAttributes` 传入 `successMessage` / `errorMessage`，页面用 `fragments/common :: flashMessages` 渲染。
- **表单校验**：前端使用 Bootstrap `needs-validation` + `novalidate`，`js/app.js` 自动附加校验反馈；后端仍需二次校验。
- **删除确认**：给按钮加 `data-confirm="提示语"` 即可触发确认框；若提示语包含动态表达式（如 `${account.name}`），必须用 `th:attr="data-confirm=|...|"`（普通属性内 Thymeleaf 不解析表达式，会导致乱码）。
- **错误页**：`error/404.html`、`error/500.html` 由 Spring Boot 自动错误处理渲染，500 页接收默认 `message` 属性。
- **✋ XSS 约定（安全硬性约束）**：`articles/detail.html` 使用 `th:utext` 渲染 `article.contentHtml`（Markdown→HTML 富文本正文）。**后端必须**在将 Markdown 转换为 HTML 时做白名单清洗（如 OWASP Java HTML Sanitizer `PolicyFactory` / jsoup `Safelist`），只保留安全的标签与属性（标题、段落、列表、引用、链接、加粗、行内代码、图片等），并**去除 `javascript:`、事件属性（`on*`）等危险内容**，再注入 `contentHtml`。`contentHtml` 永远不应包含未经清洗的服务端原始内容。

## 页面与路由

### 1. 系统首页 `/`
模板：`dashboard.html`
- `stats.articleCount`（Long）、`stats.hotTopicCount`、`stats.accountCount`、`stats.publishCount`
- `recentArticles`（`List<ArticleDto>`，字段：`id`、`title`、`status`、`wordCount`、`updatedAt`）

### 2. 热点管理
- 列表 `/hot-topics` → `hot-topics/list.html`
  - `topics`（`List<HotTopicDto>`：`id`、`title`、`source`、`rank`、`hotLevel`、`status`、`collectedAt`）
  - `total`、`totalPages`、`currentPage`
  - 筛选参数：`keyword`、`source`、`status`
- 详情 `/hot-topics/{id}` → `hot-topics/detail.html`
  - `topic`（含 `sourceUrl`）、`articles`（关联文章，可空）
- 采集路由（按单个热点触发，与架构文档 §4.1 一致）：`POST /hot-topics/{id}/refresh`（表单提交，成功后重定向回列表）
- 手动采集（整批，JZZ-16）：`POST /hot-topics/collect`（列表页右上角「立即采集」按钮，触发 TopicCrawler 抓取 + 去重 + 入库，成功后 flash 返回新增数量）

### 3. 文章管理
- 列表 `/articles` → `articles/list.html`
  - `articles`（`ArticleDto`：`id`、`title`、`status`、`wordCount`、`aiProvider`、`updatedAt`）
  - `total`、`totalPages`、`currentPage`
  - 参数：`keyword`、`status`、`sort`
- 详情 `/articles/{id}` → `articles/detail.html`
  - `article`（含 `content`、`contentHtml`、`summary`、`createdAt`、`updatedAt`、`aiProvider`、`wordCount`）
  - `publishRecords`（`List<PublishRecordDto>`：`accountId`、`accountName`、`status`）
- 编辑 `/articles/{id}/edit` → `articles/edit.html`；更新提交 `PUT /articles/{id}`（表单 + `_method=put`）

### 4. AI 生成 `/generate` → `generate/index.html`
- `topics`（用于下拉选择）、`selectedTopicId`（可选）、`style`、`length`（保留上次选择）
- 生成提交（基于热点新建并生成）：`POST /articles`（字段：`topicId`、`title`、`style`、`length`、`prompt`、`saveDraft`）——返回 `taskId`，对应架构文档 §4.2 的生成创建流程
- 重新生成（对已有文章）：`POST /articles/{id}/generate`（文章详情页「重新生成」按钮，对应架构文档 §4.2 `POST /articles/{id}/generate`）
- 生成进行中渲染 `generation`（`taskId`、`status`、`message`、`articleId`），页面会轮询 `GET /tasks/{taskId}`（返回 JSON：`status`、`message`、`articleId`）

### 5. 平台管理
- 列表 `/platform-accounts` → `accounts/list.html`
  - `accounts`（`PlatformAccountDto`：`id`、`name`、`platform`、`status`、`lastVerifyAt`）
- 新增 `/platform-accounts/new`、编辑 `/platform-accounts/{id}/edit` → `accounts/form.html`
  - `account`、`credentials`（脱敏后的凭据视图，如 `appId`）
  - 提交：`POST /platform-accounts`（新增）/ `POST /platform-accounts/{id}`（编辑，`method=post`；平台不可变更，禁用下拉并携带隐藏 `platform` 值）
- 验证：`POST /platform-accounts/{id}/verify`；删除：`POST /platform-accounts/{id}/delete`

### 6. 发布管理 `/publish-records` → `publish/index.html`
- `publishableArticles`（`List<ArticleDto>`）、`accounts`（已验证与全部账号）
- `records`（`PublishRecordDto`：`id`、`articleId`、`articleTitle`、`accountId`、`accountName`、`status`、`retryCount`、`publishedAt`、`scheduledAt`、`errorMessage`）
- `total`、`totalPages`、`currentPage`
- 提交：`POST /publish-records`（字段：`articleId`、`accountIds[]`、`scheduledAt`）
- 重试：`POST /publish-records/{id}/retry`

## 说明
- 平台类型枚举（`platform`）：`gzh`、`xiaohongshu`、`zhihu`、`toutiao`
- 文章状态（`status`）：`draft`、`generated`、`published`、`failed`
- 热点状态：`unused`、`generating`、`generated`；热度：`high`、`middle`、`normal`
- 账号状态：`unverified`、`verified`、`disabled`
- 发布状态：`pending`、`publishing`、`success`、`failed`
