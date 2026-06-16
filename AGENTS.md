# AGENTS.md

本文件给后续 AI/coding agent 使用，作用类似 Claude Code 的 `CLAUDE.md` 或 OpenCode 的 `AGENTS.md`。除非用户另有说明，本文件的约定适用于整个仓库。

## 项目概览

Eval System 是一个面向智能体与大模型应用的评测系统 MVP。它把评测集数据、待评测智能体、自动评估器和人工标签组织成可追踪的评测任务。

当前主要能力：

- 评测集管理：字段、数据行、Excel 导入、草稿/发布版本。
- 标签管理：分类、布尔、数字、文本四类人工标注维度。
- 评估器管理：自定义 LLM/code 评估器、预置评估器、版本发布。
- 评测任务：绑定评测集版本、智能体、评估器和标签，执行后保存样本级输出、评分和标注。
- 外部平台集成：模型列表、模型对话、智能体列表、Super 智能体调用、可选 IAM 模型调用。

## 技术栈

- 后端：Java 21、Spring Boot 3.3.5、Spring MVC、MyBatis Plus、MySQL、Apache POI、Maven。
- 前端：Vue 3.5、TypeScript、Vite 5、Vue Router、Element Plus、Axios、npm。
- 数据库：MySQL，建表脚本在 `DDL/`。

## 目录地图

```text
eval_system/
├── backend/                 # Spring Boot 后端
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/evalsystem/
│       │   ├── common/      # ApiResponse、PageResponse、全局异常处理
│       │   ├── config/      # Web/CORS 配置
│       │   ├── dataset/     # 评测集模块
│       │   ├── evaluator/   # 自定义和预置评估器模块
│       │   ├── integration/ # 外部模型/智能体平台集成
│       │   ├── tag/         # 人工标签模块
│       │   └── task/        # 评测任务编排和执行
│       ├── main/resources/
│       │   ├── application*.yml
│       │   └── mapper/      # MyBatis XML
│       └── test/java/       # JUnit 测试
├── frontend/                # Vue 前端
│   ├── package.json
│   └── src/
│       ├── api/             # Axios API 封装
│       ├── config/          # 导航模块配置
│       ├── layouts/         # 应用布局
│       ├── modules/         # 页面 composables
│       ├── router/          # 路由
│       ├── views/           # 页面组件
│       ├── styles.css       # 全局样式
│       └── types.ts         # 前端共享类型
└── DDL/                     # 数据库建表脚本，按编号顺序执行
```

搜索或批量阅读时排除生成目录：

```powershell
rg "pattern" backend frontend DDL --glob "!frontend/node_modules/**" --glob "!frontend/dist/**" --glob "!backend/target/**"
```

## 常用命令

后端：

```powershell
cd backend
mvn test
mvn spring-boot:run
```

前端：

```powershell
cd frontend
npm install
npm run dev
npm run build
```

本地默认地址：

- 后端：`http://localhost:8080`
- 前端：`http://localhost:5173`
- Vite 代理：`/api -> http://localhost:8080`

数据库初始化顺序：

```text
DDL/01_eval_dataset.sql
DDL/02_eval_tag.sql
DDL/03_eval_evaluator.sql
DDL/04_eval_task.sql
```

默认数据库配置在 `backend/src/main/resources/application.yml`，连接 `jdbc:mysql://localhost:3306/eval_system`，用户名 `root`，密码 `123456`。本地开发时按实际 MySQL 信息调整。

## 后端约定

主入口是 `com.evalsystem.EvalSystemApplication`，通过 `@MapperScan` 扫描 dataset、tag、evaluator、task 四组 mapper。

模块结构基本一致：

- `controller/`：REST API，返回 `ApiResponse<T>`。
- `service/` 和 `service/impl/`：业务校验、事务和编排。
- `mapper/`：MyBatis mapper 接口。
- `dto/`：接口层数据结构，多数为 Java record。
- `pojo/`：表对象。

通用响应：

- 成功：`ApiResponse.ok(data)`，`code = 0`，`msg = "success"`。
- 业务参数错误：`IllegalArgumentException` 映射为 HTTP 400，`code = 400`。
- 未处理异常：HTTP 500，`code = 500`。

重要业务约定：

- ID 使用 `UUID.randomUUID().toString().replace("-", "")`。
- 时间字段以 `String.valueOf(System.currentTimeMillis())` 保存。
- 草稿版本使用 `version_no = 0`，发布版本使用正整数。
- 评测集、评估器、任务多为软删除：`is_deleted = 1`。
- 字段类型通常为 `string`、`number`、`boolean`。
- 标签类型为 `category`、`boolean`、`number`、`text`。
- 评估器类型为 `llm` 或 `code`；code 评估器当前有配置入口，但任务执行里仍提示未接入真实代码执行接口。
- 任务状态主要为 `pending`、`running`、`completed`、`failed`；任务项还包含 `annotation_pending`。
- `TaskServiceImpl` 使用 `TaskExecutor` 在事务提交后异步执行任务，单条样本失败会落到样本/评估结果，不应中断整个任务循环。

主要 API 前缀：

- `/api/datasets`：评测集、版本、字段、行、Excel 导入。
- `/api/tags`：标签配置。
- `/api/evaluators`：自定义评估器、版本、预置评估器。
- `/api/tasks`：任务创建、列表、详情、启动、删除、人工标注。
- `/api/integration`：模型列表、智能体列表、模型对话调试。

外部集成配置在 `application-dev.yml` 和 `application-prod.yml` 的 `integration.platform.*`。支持 `model-list-url`、`agent-list-url`、`model-chat-url`、`super-agent-chat-url`、`x-space-id`、可选 `x-agent-alias`、登录参数和 IAM 参数。修改这些文件时注意保留中文内容的编码。

## 前端约定

前端入口：

- `frontend/src/main.ts`：注册 Vue、Element Plus、全量 Element Plus icons、router 和全局样式。
- `frontend/src/App.vue`：只渲染 `RouterView`。
- `frontend/src/router/index.ts`：根路径重定向到 `/datasets`，所有页面挂在 `AppLayout` 下。

主要路由：

- `/datasets`：评测集列表。
- `/datasets/:datasetId`：评测集版本、字段、数据行。
- `/tags`：标签管理。
- `/evaluators`：评估器管理。
- `/evaluators/create` 和 `/evaluators/:evaluatorId`：评估器编辑。
- `/tasks`：任务列表。
- `/tasks/create`：创建任务。
- `/tasks/:taskId`：任务详情。
- `/tasks/:taskId/items/:taskItemId/annotation`：人工标注。

前端 API 位于 `frontend/src/api/*.ts`，每个模块都创建 `axios.create({ baseURL: "/api", timeout: 10000 })` 并通过 `unwrap` 处理 `ApiResponse`。新增接口时优先保持这个模式，类型放到 `frontend/src/types.ts` 或对应 API 文件附近。

页面逻辑主要拆到 `frontend/src/modules/*/composables/`，页面组件在 `frontend/src/views/`。修改复杂页面时，优先把状态、加载、提交、校验逻辑放在 composable 中，保持 Vue SFC 模板聚焦展示。

UI 使用 Element Plus 和全局 `styles.css`。当前布局偏桌面工作台，`body` 设置了 `min-width: 1180px` 和固定工作区滚动；不要在小改动里顺手重构整套响应式布局。

## 数据库和领域模型

建表文件按领域拆分：

- `01_eval_dataset.sql`：评测集、版本、字段、数据行、单元格。
- `02_eval_tag.sql`：标签和标签选项。
- `03_eval_evaluator.sql`：预置评估器、自定义评估器、版本、参数。
- `04_eval_task.sql`：任务、应用字段映射、任务评估器、参数映射、任务标签、任务项、自动评估结果、人工标签结果。

新增字段时需要同步：

- DDL。
- 对应 `pojo`、`dto` 或 record。
- Mapper 接口和 XML resultMap/SQL。
- Service 校验和转换。
- 前端 `types.ts`、API 封装、页面展示或表单。

## 测试现状

后端已有 JUnit 测试，主要覆盖：

- `PresetEvaluatorStoreTest`
- `AgentOutputFormatterTest`
- `TaskServiceImplPresetDisplayTest`
- `PlatformIntegrationServiceImplTest`

修改后端业务逻辑时优先运行：

```powershell
cd backend
mvn test
```

修改前端类型或页面逻辑时优先运行：

```powershell
cd frontend
npm run build
```

仅改文档时不需要跑完整测试，但应至少检查 diff。

## Agent 工作注意事项

- 不要编辑或提交 `frontend/node_modules/`、`frontend/dist/`、`backend/target/`、`*.tsbuildinfo` 等生成物。
- `frontend/package-lock.json` 被 `.gitignore` 忽略；不要因为 `npm install` 产生它就主动纳入版本控制。
- 工作区可能已有用户变更。先看 `git status --short`，不要恢复或覆盖非本任务产生的改动。
- `README.md` 可能不存在或处于删除状态时，不要擅自恢复；本文件就是 agent 初始化入口。
- 修改接口时保持后端 `ApiResponse` 包装和前端 `unwrap` 约定一致。
- 修改 MyBatis 查询时同步检查 XML resultMap、DTO 构造参数顺序和字段别名。
- 修改评测任务执行时特别小心状态流转、事务边界和异步执行；任务执行失败应尽量落到样本或评估结果维度。
- 修改中文文案或注释时注意文件编码，避免把已有中文写成乱码。
