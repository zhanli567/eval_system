# Jiuwen Evaluation Engine Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 保留 Eval-System 现有任务编排和 Agent `chat/completion` 调用方式，将 SSE 轨迹与执行指标结构化，并把 `TaskService` 中手写的评估器执行逻辑迁移到基于 Jiuwen `Case`、`BaseEvaluator`、`MetricEvaluator`、`Metric` 和 `EvaluatedCase` 的评测引擎。

**Architecture:** Agent 仍是独立服务，只通过 SSE 返回回答、轨迹和执行指标；Eval-System 负责 Case 快照、评估器配置、任务编排和结果持久化。Jiuwen 能力位于 Eval-System 后端的评测引擎适配层，不进入 Agent 业务执行链，也不要求 Agent 查询 Eval 数据库。

**Tech Stack:** Java 21、Spring Boot 3.3.5、PostgreSQL 17、MyBatis-Plus、Jiuwen agent-core-java 0.1.14、Vue 3、Vite 5。

---

## 范围与边界

本计划只包含普通 Case 评测主链：

```text
Eval Task -> chat/completion -> SSE chunks -> AgentExecutionResult
          -> Jiuwen Case + predict -> BaseEvaluator -> EvaluatedCase
          -> Eval result persistence
```

本期不包含 `EvolutionPipeline`、SkillsBench、自定义 Benchmark，不在 Agent 内加入 `EvaluationHarness`，也不向 Agent 下发标签、评估器、ticket 或 manifestId。Benchmark 后续使用独立计划实现。

## 文件结构

新增后端包：

```text
backend/src/main/java/com/agentnexus/backend/evaluation/
├── EvaluationEngine.java                 统一评测引擎接口
├── EvaluationRequest.java                Case、预测结果和评估器快照
├── EvaluationOutcome.java                原始分数、归一化分数、原因和明细
├── EvaluationRequestFactory.java         从任务绑定生成稳定请求
├── legacy/LegacyEvaluationEngine.java    迁出的现有 LLM Judge，支持灰度回退
└── jiuwen/
    ├── JiuwenEvaluationEngine.java       调用 Jiuwen BaseEvaluator
    ├── JiuwenCaseAdapter.java            Eval 数据行转换为 Jiuwen Case
    ├── JiuwenEvaluatorFactory.java       根据 Eval 配置创建评估器
    ├── JiuwenResultMapper.java            EvaluatedCase 转换为 Eval 结果
    └── ConfiguredLlmJudgeEvaluator.java  保留自定义 Prompt 和任意评分区间

backend/src/main/java/com/agentnexus/backend/remoteCall/model/
├── AgentExecutionResult.java
├── TrajectoryEvent.java
└── ExecutionMetrics.java
```

修改现有文件：

- `backend/pom.xml`：接入 Jiuwen 依赖及测试依赖。
- `backend/src/main/java/com/agentnexus/backend/remoteCall/api/dto/response/ChatCompletionChunk.java`：增加两种 SSE content 类型。
- `backend/src/main/java/com/agentnexus/backend/remoteCall/api/dto/response/AgentChatResponse.java`：保存结构化轨迹、指标和原始 chunks。
- `backend/src/main/java/com/agentnexus/backend/remoteCall/service/RemoteCallService.java`：解析和聚合新增类型。
- `backend/src/main/java/com/agentnexus/backend/task/service/TaskService.java`：只保留任务编排，委托 `EvaluationEngine`。
- `DDL/increment_2608.sql`：增加 Agent 执行元数据和评测引擎结果字段。

## Task 0：建立可重复运行基线

- [ ] 使用真正的 JDK 21 验证环境，禁止使用当前默认 JDK 8 或本机 JDK 23 代替。
- [ ] 启动本地 PostgreSQL，确认 `localhost:5432` 可连接。
- [ ] 执行 `DDL/01_eval_dataset.sql` 至 `DDL/04_eval_task.sql` 初始化 `eval_system`。
- [ ] 在 `backend` 执行 `mvn test`，预期所有源码编译通过。
- [ ] 在 `frontend` 执行 `npm install --no-package-lock`、`npm test` 和 `npm run build`。
- [ ] 启动后端 8080 和前端 5173，验证任务列表页面可访问。
- [ ] 记录 Agent `chat/completion` 的脱敏 SSE 样例，覆盖 `text`、`reasoning`、`skill_trigger`、`tool_call`、`tool_response` 和 `[DONE]`。

验收：基线命令可重复执行；若 Java 21 下仍有 Lombok 编译问题，先单独修复构建链，不进入迁移开发。

## Task 1：锁定 SSE 新协议

- [ ] 为 `ChatCompletionChunk` 增加 `TrajectoryContent`，字段至少包含 `sequence`、`eventType`、`stage`、`timestamp`、`parentId` 和 `payload`。
- [ ] 增加 `ExecutionMetricsContent`，字段至少包含 `latencyMs`、`modelCallCount`、`toolCallCount`、`inputTokens`、`outputTokens` 和可扩展 `attributes`。
- [ ] 规定 `type` 固定为 `trajectory` 和 `execution_metrics`，字段使用 camelCase；解析器兼容 snake_case 输入。
- [ ] 对未知 `type` 保留原始 JSON，不再无条件降级成普通 text。
- [ ] 增加 `RemoteCallService` 单元测试，输入多条 SSE chunk，断言文本、事件顺序、指标和原始 chunk 均未丢失。

Agent 返回示例：

```json
{"choices":[{"delta":{"content":[{"type":"trajectory","sequence":2,"eventType":"tool_call","stage":"react","payload":{"toolName":"search","arguments":{"query":"九问"}}}]}}]}
```

```json
{"choices":[{"delta":{"content":[{"type":"execution_metrics","latencyMs":1200,"modelCallCount":2,"toolCallCount":1,"inputTokens":100,"outputTokens":50,"attributes":{}}]}}]}
```

验收：旧 SSE 样例输出不变；新类型结构化保留；未知类型可追溯。

## Task 2：建立 AgentExecutionResult

- [ ] 新建 `TrajectoryEvent` 和 `ExecutionMetrics` 不可变对象。
- [ ] 新建 `AgentExecutionResult`，包含 `answer`、`outputs`、`trajectory`、`metrics`、`rawChunks`、`conversationId`、`model`、`latencyMs`、`status` 和 `errorMessage`。
- [ ] 将 `RemoteCallService.parseAgentStream()` 的返回结果转换为该模型，不再把所有信息压入 `Map<String,String>`。
- [ ] 保留 `answer/text/content/reasoning/toolCall/toolResponse` 兼容字段，保证现有评估器参数映射可继续使用。
- [ ] 修改任务持久化映射，保存回答展示文本，同时单独保存轨迹、metrics 和原始响应 JSON。

验收：同一 SSE 输入既能生成旧的应用输出字段，也能生成结构化轨迹和指标。

## Task 3：从 TaskService 抽出 EvaluationEngine

- [ ] 写 `EvaluationEngine.evaluate(EvaluationRequest)` 接口测试。
- [ ] 定义 `EvaluationRequest`：`caseId`、`inputs`、`labels`、`prediction`、`trajectory`、`metrics` 和不可变评估器版本快照。
- [ ] 定义 `EvaluationOutcome`：`status`、`rawScore`、`normalizedScore`、`passResult`、`reason`、`perMetric`、`rawResult` 和 `errorMessage`。
- [ ] 将 `evaluateWithRemoteCall()`、Prompt 渲染、结果解析迁移到 `LegacyEvaluationEngine`，迁移前后测试结果必须一致。
- [ ] `TaskService.evaluateBoundEvaluator()` 改为创建请求并调用引擎，不再直接调用 Judge 模型。
- [ ] 使用配置 `evaluation.engine=legacy|jiuwen` 选择实现，默认先保持 `legacy`。

验收：使用 legacy 引擎跑原有 Case，数据库中的 score、pass/fail、reason 和错误状态不变。

## Task 4：接入 Jiuwen 数据模型

- [ ] 在 `backend/pom.xml` 接入可用的 Jiuwen 0.1.14 构件；若尚未发布，先以本地 Maven 构件验证，计划中不得复制 Jiuwen 源码进 Eval。
- [ ] `JiuwenCaseAdapter` 将评测集当前行映射为 `new Case(inputs, labels, null, caseId)`。
- [ ] prediction 使用结构化 Map：`answer`、`outputs`、`trajectory`、`metrics`。
- [ ] `JiuwenResultMapper` 将 `EvaluatedCase` 映射为 `EvaluationOutcome`。
- [ ] 增加 Case 转换测试，覆盖必填 inputs/label、空字段、数字和布尔值。

验收：Eval Case ID、输入、标签和 Agent 预测在 Jiuwen 对象中一一对应。

## Task 5：实现 Jiuwen 评估器工厂

- [ ] `JiuwenEvaluatorFactory` 根据 Eval 评估器类型和版本快照构造 `BaseEvaluator`。
- [ ] 第一种内置映射：精确匹配 -> `MetricEvaluator(new ExactMatchMetric(...))`。
- [ ] LLM 类型映射到 `ConfiguredLlmJudgeEvaluator extends BaseEvaluator`，继续使用 Eval 选择的模型、Prompt、参数映射和返回 JSON 协议。
- [ ] 暂不启用 Code 评估器；明确返回“不支持”的失败结果，不能静默回退。
- [ ] 增加工厂测试，断言每种配置得到正确类型，非法配置得到稳定错误码。

验收：评估器管理面无需改变即可生成 Jiuwen evaluator。

## Task 6：兼容任意评分范围

- [ ] 保持 Eval 的 `rawScore`，例如 1～5 分中的 4.5。
- [ ] 使用 `(rawScore-scoreMin)/(scoreMax-scoreMin)` 生成 `normalizedScore`，仅作为九问统一指标和统计扩展使用。
- [ ] `passResult` 始终使用 `rawScore >= passThreshold` 计算。
- [ ] 不允许 `EvaluatedCase` 的 0～1 约束覆盖 Eval 原始分数。
- [ ] 增加边界测试：最小值、最大值、阈值、越界分数、小数分数和负数区间。

验收：现有页面展示和通过率不因接入 Jiuwen 改变。

## Task 7：双跑与灰度切换

- [ ] 增加 `evaluation.shadow-enabled`；开启时 legacy 结果用于业务，Jiuwen 结果只写对比日志或对比表。
- [ ] 对比字段包括 score 差值、pass 是否一致、reason 是否可解析、错误类型和执行耗时。
- [ ] 为 shadow 执行设置独立超时，shadow 失败不得影响主结果。
- [ ] 选取至少一组精确匹配和一组 LLM Judge 任务执行双跑。
- [ ] 达到约定一致率后，将默认引擎切换为 `jiuwen`，保留 legacy 回退一个发布周期。

验收：可以通过配置切换或回退，且无需修改 Agent。

## Task 8：端到端验证

- [ ] 使用固定 Case 调用真实 Agent `chat/completion`，确认 SSE 完整消费到 `[DONE]`。
- [ ] 验证旧 content 类型和新增两种类型均被保存。
- [ ] 验证 Jiuwen evaluator 可读取 answer、trajectory 和 metrics。
- [ ] 验证任务停止、Agent 失败、Evaluator 失败和重跑语义保持不变。
- [ ] 执行后端测试、前端测试、前端构建和数据库迁移回归。
- [ ] 输出脱敏验证记录：Case ID、conversationId、引擎版本、评估器版本、score 和耗时。

验收：一条 Case 从数据集进入 Agent，到 SSE 聚合、Jiuwen 评估和页面展示全链路可追踪。

## 后续独立计划

Benchmark 路线单独规划：

```text
Eval Benchmark Task -> Evolution Worker -> EvolutionPipeline
                    -> SkillsBenchAdapter -> RemoteAgentAdapter
                    -> Agent chat/completion
```

第一期只支持 SkillsBench；自定义 Benchmark 通过 Jiuwen `BaseBenchAdapter` 扩展，不与普通 Case 主链共用任务表和状态机。
