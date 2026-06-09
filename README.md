# Eval System

面向应用与智能体评测场景的前后端分离评测平台。系统围绕“评测集 -> 评估器/标签 -> 评测任务 -> 结果与人工标注”的流程组织，帮助团队维护结构化评测数据、配置自动评估维度，并在任务维度查看评估结果和标注进度。

> 当前项目仍处于开发阶段：智能体应用接入、真实 LLM/Code 执行器与预置评估器初始化数据尚未完整落地。任务执行阶段已提供本地模拟评估流程，便于先打通评测任务闭环。

## 目录

- [项目特性](#项目特性)
- [功能模块](#功能模块)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [使用流程](#使用流程)
- [接口文档](#接口文档)
- [常用命令](#常用命令)
- [当前边界](#当前边界)
- [参考资料](#参考资料)

## 项目特性

- 评测集版本化管理：支持草稿版本编辑、发布版本只读、历史版本覆盖草稿。
- 灵活表结构：评测集字段支持文本、数字、布尔类型，并可导入 `.xlsx` / `.xls` 数据。
- 多类型人工标签：支持分类、布尔、数字、文本标签，分类/布尔选项可映射 Pass/Fail。
- 评估器配置：支持预置评估器查看、自定义 LLM 评估器、自定义 Code 评估器、变量参数与版本发布。
- 评测任务闭环：任务可绑定评测集发布版本、评估器、人工标签，并展示数据行级结果。
- 统一接口返回：后端接口统一返回 `code`、`msg`、`data`，便于前端和调用方处理。

## 功能模块

| 模块 | 路由 | 说明 |
| --- | --- | --- |
| 评测集管理 | `/datasets` | 创建评测集、维护表头、单条/批量导入数据、发布版本、覆盖草稿。 |
| 标签管理 | `/tags` | 创建和维护分类、布尔、数字、文本四类人工评测标签。 |
| 评估器管理 | `/evaluators` | 查看预置评估器，创建、复制、编辑和发布自定义评估器版本。 |
| 评测任务 | `/tasks` | 创建任务，绑定评测集版本、评估器和标签，查看评测明细并完成人工标注。 |

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3、TypeScript、Vite、Vue Router、Element Plus、Axios |
| 后端 | Java 21、Spring Boot 3.3.5、MyBatis-Plus、Apache POI |
| 数据库 | MySQL |
| 构建工具 | npm、Maven |

## 项目结构

```text
eval_system
|-- backend                         # Spring Boot 后端服务
|   |-- src/main/java/com/evalsystem
|   |   |-- common                  # 统一响应、分页、异常处理
|   |   |-- dataset                 # 评测集领域
|   |   |-- tag                     # 标签领域
|   |   |-- evaluator               # 评估器领域
|   |   `-- task                    # 评测任务领域
|   `-- src/main/resources
|       |-- application.yml         # 本地服务与数据库配置
|       `-- mapper                  # MyBatis XML
|-- frontend                        # Vue 前端应用
|   |-- src/api                     # API 请求封装
|   |-- src/modules                 # 业务组合逻辑
|   |-- src/router                  # 页面路由
|   `-- src/views                   # 页面组件
|-- DDL                             # 数据库建表脚本
|-- 阿里云百炼平台评测系统示例图片       # 产品参考截图
`-- 接口文档.md                      # 接口说明
```

## 环境要求

- JDK 21
- Maven 3.8+
- Node.js 18+ 与 npm
- MySQL 8.x

## 快速开始

### 1. 克隆项目

```bash
git clone <repo-url>
cd eval_system
```

### 2. 初始化数据库

先创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS eval_system
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

再按顺序执行 DDL：

```bash
mysql -u root -p eval_system < DDL/01_eval_dataset.sql
mysql -u root -p eval_system < DDL/02_eval_tag.sql
mysql -u root -p eval_system < DDL/03_eval_evaluator.sql
mysql -u root -p eval_system < DDL/04_eval_task.sql
```

后端默认连接配置位于 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/eval_system
    username: root
    password: 123456
```

如本地 MySQL 账号或端口不同，请先修改该文件。

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认服务地址：

```text
http://localhost:8080
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认前端地址：

```text
http://localhost:5173
```

Vite 已配置 `/api` 代理到 `http://localhost:8080`，本地开发时前端请求会自动转发到后端。

## 使用流程

1. 在“评测集管理”创建评测集，维护字段，并添加或导入数据。
2. 发布评测集草稿版本。评测任务只能选择已发布版本。
3. 在“标签管理”创建人工评测标签，例如通过/不通过、质量分、备注文本等。
4. 在“评估器管理”创建自定义评估器，或基于预置评估器创建自定义版本并发布。
5. 在“评测任务”创建任务，选择评测集版本、评估器、字段映射和标签。
6. 启动任务后查看数据明细、评估器结果和人工标注状态。
7. 进入标注页面补齐人工标签，任务会随行级结果完成情况更新状态。

## 接口文档

详细接口见 [接口文档.md](接口文档.md)。主要资源路径如下：

| 资源 | 路径 |
| --- | --- |
| 评测集 | `/api/datasets` |
| 评测集版本与数据 | `/api/datasets/versions/{versionId}` |
| 标签 | `/api/tags` |
| 评估器 | `/api/evaluators` |
| 评估任务 | `/api/tasks` |

接口统一响应结构：

```json
{
  "code": 0,
  "msg": "success",
  "data": {}
}
```

## 常用命令

后端：

```bash
cd backend
mvn spring-boot:run
mvn test
mvn package
```

前端：

```bash
cd frontend
npm run dev
npm run build
npm run preview
```

## 当前边界

- 预置评估器表结构已存在，但仓库当前未包含初始化数据脚本；如需展示预置评估器，需要补充 `eval_preset_evaluator_category`、`eval_preset_evaluator`、`eval_evaluator_param` 数据。
- `agent` 智能体应用入口在前端处于禁用状态，后端保留了字段映射与应用输出结构。
- 任务启动时会进行本地模拟评估：必填参数映射为空会失败，否则按评估器通过阈值写入 Pass 结果。
- Code 评估器目前保存执行函数配置，但尚未接入真实 Python 沙箱或代码执行服务。
- 仓库当前未包含 `LICENSE` 文件，正式对外发布前建议补充许可证。

## 参考资料

在组织 README 时参考了 GitHub 官方 README 建议，以及 React、Kubernetes、Visual Studio Code 等高 star 项目的常见结构：项目定位、快速开始、文档入口、贡献/支持信息和许可证说明。结合本项目当前状态，本文档优先覆盖“快速理解”和“本地跑通”两件事。
