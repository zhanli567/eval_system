# Agent 侧评测观测适配需求

## 1. 改造边界

Agent 保持现有独立部署方式和 `chat/completion` 接口，不在 Agent 内执行评估器，不读取 Eval-System 数据库，也不接收评测集标签、评估器配置、ticket 或 manifestId。

Agent 侧只负责在现有 SSE `choices[].delta.content[]` 中补充结构化执行观测数据。Eval-System 负责解析、持久化并将其作为 Jiuwen Evaluator 的 `predict` 输入。

## 2. 请求协议保持不变

Eval-System 继续发送：

```http
POST /chat/completion
Content-Type: application/json
Accept: text/event-stream, application/json
x-super-agent-id: {agentId}
x-bundle-id: {bundleId}
x-agent-alias: {agentAlias}
x-space-id: {spaceId}
```

```json
{
  "conversationId": "taskId-itemId",
  "messages": [{"role": "user", "content": "..."}],
  "stream": true
}
```

不新增“是否评测”Header。Agent 版本继续由现有 Header 表达。

## 3. 新增 trajectory 类型

Agent 在关键执行节点输出一个或多个 `trajectory` content block：

```json
{
  "choices": [{
    "index": 0,
    "delta": {
      "role": "assistant",
      "content": [{
        "type": "trajectory",
        "sequence": 3,
        "eventType": "tool_call",
        "stage": "react",
        "timestamp": 1786593600123,
        "eventId": "evt-3",
        "parentId": "model-call-1",
        "payload": {
          "toolCallId": "call-1",
          "toolName": "search",
          "arguments": {"query": "九问"}
        }
      }]
    }
  }]
}
```

字段要求：

| 字段 | 必填 | 说明 |
|---|---:|---|
| `type` | 是 | 固定为 `trajectory` |
| `sequence` | 是 | 单次会话内严格递增，用于恢复事件顺序 |
| `eventType` | 是 | 事件类型 |
| `stage` | 否 | 所属执行阶段，例如 `react`、`model`、`tool`、`final` |
| `timestamp` | 是 | Unix 毫秒时间戳 |
| `eventId` | 否 | 当前事件唯一标识 |
| `parentId` | 否 | 父事件标识，用于恢复调用关系 |
| `payload` | 是 | 事件原始结构，不要预先拼成字符串 |

第一期至少支持以下 `eventType`：

- `agent_start`
- `model_call`
- `model_response`
- `tool_call`
- `tool_response`
- `skill_trigger`
- `agent_finish`
- `agent_error`

不得在 trajectory 中输出模型私有思维链。可输出阶段名称、模型调用元数据、工具调用、工具结果摘要和错误信息。

## 4. 新增 execution_metrics 类型

Agent 在本次请求结束前至少输出一次汇总指标：

```json
{
  "choices": [{
    "index": 0,
    "delta": {
      "role": "assistant",
      "content": [{
        "type": "execution_metrics",
        "latencyMs": 1250,
        "modelCallCount": 2,
        "toolCallCount": 1,
        "inputTokens": 320,
        "outputTokens": 96,
        "attributes": {
          "finishReason": "stop",
          "failedToolCallCount": 0
        }
      }]
    }
  }]
}
```

字段要求：

| 字段 | 必填 | 说明 |
|---|---:|---|
| `type` | 是 | 固定为 `execution_metrics` |
| `latencyMs` | 是 | Agent 接收请求到完成响应的总耗时 |
| `modelCallCount` | 是 | 模型调用次数 |
| `toolCallCount` | 是 | 工具调用次数 |
| `inputTokens` | 否 | 能获取时返回，否则省略 |
| `outputTokens` | 否 | 能获取时返回，否则省略 |
| `attributes` | 否 | 扩展指标 Map，值必须可 JSON 序列化 |

若分阶段发送多条 `execution_metrics`，最后一条必须是汇总值，并在 `attributes.final=true` 标识。第一期推荐只发送最终汇总，降低聚合歧义。

## 5. SSE 完成与错误要求

- 保留现有 `text`、`reasoning`、`skill_trigger`、`tool_call`、`tool_response`、`references`、`debug`、`error` 和 `gen_ui` 类型。
- 新类型不得改变已有文本回答的 chunk 顺序和内容。
- 正常完成时，先发送最终 `execution_metrics`，再发送带 `finish_reason` 的 chunk，最后发送 `data: [DONE]`。
- 执行失败时发送 `agent_error` trajectory；能生成指标时仍应发送最终 `execution_metrics`，然后按现有 `error` 协议结束。
- 所有 `data:` 内容必须是单行合法 JSON；不要把一个 JSON 对象拆到多个 SSE `data:` 行。
- 字段名优先使用 camelCase。Eval-System 会兼容 snake_case，但 Agent 端不应混用。

## 6. Agent 侧验收用例

1. 无工具调用：返回 text、开始/结束 trajectory、最终 metrics 和 `[DONE]`。
2. 单工具调用：trajectory 能通过 `sequence` 和 `toolCallId` 关联调用与返回。
3. 多模型/多工具调用：sequence 严格递增，汇总次数正确。
4. 工具失败：存在 `agent_error` 或失败工具事件，`failedToolCallCount` 正确。
5. Agent 整体失败：保留现有 error content，并尽可能返回轨迹和指标。
6. 旧客户端兼容：忽略未知 content type 后仍能正常拼接最终 text。

交付给 Eval-System 联调时，请提供上述六类场景的脱敏 SSE 原文，以及字段来源说明。
