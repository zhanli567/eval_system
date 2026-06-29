# Eval System

Eval System 是一个智能体与大模型应用评测系统 MVP。系统把评测集、人工标签、自动评估器、待评测智能体和评测任务组织在一起，用于创建可追踪的评测流程，并保存样本级输出、自动评分和人工标注结果。

## 当前功能

- 评测集管理：评测集创建、字段维护、数据行维护、Excel 导入、草稿版本和发布版本。
- 标签管理：分类、布尔、数字、文本四类人工标签配置。
- 评估器管理：自定义 LLM/code 评估器、预置评估器、参数配置和版本发布。
- 评测任务：绑定评测集版本、智能体、评估器和人工标签，执行后查看样本、应用输出、评估结果和标注结果。
- 外部平台集成：模型列表、模型对话、智能体列表、智能体详情和版本列表。

## 技术栈

- 后端：Java 21、Spring Boot 3.3.5、Spring MVC、JAX-RS 风格注解适配、MyBatis Plus、PostgreSQL、Apache POI、Maven。
- 前端：Vue 3.5、TypeScript、Vite 5、Vue Router、Element Plus、Axios、npm。
- 数据库：PostgreSQL，建表脚本在 `DDL/`。

## 目录结构

```text
eval_system/
├── backend/                 # Spring Boot 后端
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/agentnexus/backend/
│       │   ├── common/      # 通用响应、分页、异常处理、上下文
│       │   ├── config/      # Web、CORS、JAX-RS 注解适配
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
└── DDL/                     # 数据库建表脚本
```

## 本地运行

后端默认端口为 `8080`，配置文件为 `backend/src/main/resources/application.yml`：

```powershell
cd backend
mvn spring-boot:run
```

前端默认端口为 `5173`：

```powershell
cd frontend
npm install
npm run dev
```

Vite 开发代理配置在 `frontend/vite.config.ts`。前端请求使用 `/api` 作为代理前缀，代理到 `http://localhost:8080` 后会去掉 `/api`。后端真实接口路径不包含 `/api`。

## 数据库

默认数据库连接：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/eval_system
    username: postgres
    password: 123456
```

建表脚本按顺序执行：

```text
DDL/01_eval_dataset.sql
DDL/02_eval_tag.sql
DDL/03_eval_evaluator.sql
DDL/04_eval_task.sql
```

主要表分组：

- `t_eval_dataset*`：评测集、版本、字段、数据行和单元格。
- `t_eval_tag*`：人工标签和分类选项。
- `t_eval_evaluator*`：自定义评估器、版本和参数。
- `t_eval_task*`：评测任务、字段映射、评估器绑定、标签绑定、任务项和结果。

## 接口概览

后端控制器使用 JAX-RS 风格注解，并由 `WebConfig` 适配到 Spring MVC。接口统一返回：

```json
{
  "code": 0,
  "msg": "success",
  "data": {}
}
```

业务参数错误返回 HTTP 400，`code = 400`；未处理异常返回 HTTP 500，`code = 500`。

主要接口前缀：

- `/datasets`：评测集、版本、字段、数据行、Excel 导入。
- `/tags`：人工标签配置。
- `/evaluators`：自定义评估器、预置评估器、评估器版本。
- `/tasks`：评测任务创建、列表、详情、启动、删除、人工标注。
- `/integration`：外部模型和智能体平台集成。

## 前端路由

- `/datasets`：评测集列表。
- `/datasets/:datasetId`：评测集详情。
- `/tags`：标签管理。
- `/evaluators`：评估器管理。
- `/evaluators/create`：创建评估器。
- `/evaluators/:evaluatorId`：编辑评估器。
- `/tasks`：任务列表。
- `/tasks/create`：创建任务。
- `/tasks/:taskId`：任务详情。
- `/tasks/:taskId/items/:taskItemId/annotation`：人工标注。

## 常用校验命令

后端测试：

```powershell
cd backend
mvn test
```

前端构建：

```powershell
cd frontend
npm run build
```

## 生成物

以下目录或文件属于本地依赖、构建产物或临时产物，不应提交：

- `backend/target/`
- `frontend/node_modules/`
- `frontend/dist/`
- `frontend/package-lock.json`
- `frontend/*.tsbuildinfo`
- `frontend/vite.config.js`
- `frontend/vite.config.d.ts`
