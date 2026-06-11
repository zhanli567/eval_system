# Mock服务接口

Mock模块用于在未接入真实智能体、评估LLM和Code执行器前，模拟评测任务的执行闭环。接口均走项目统一返回结构：

```json
{
  "code": 0,
  "msg": "success",
  "data": {}
}
```

## 0. 配置真实超级智能体地址

在 `backend/src/main/resources/application.yml` 中配置：

```yaml
mock:
  agent:
    # 留空时使用本地Mock响应；配置真实地址后，Mock模块会转发请求到该URL。
    url: ""
    connect-timeout-ms: 5000
    read-timeout-ms: 60000
```

调用链保持不变：

```text
评测任务 -> Mock模块 -> 真实超级智能体URL
```

Mock模块转发时会：

- 使用原请求体结构：`conversationId/message/stream`
- 强制将转发请求中的 `stream` 设置为 `true`，以便接收真实智能体SSE响应
- 携带请求头 `x-agent-alias`，值来自调用 Mock 接口时传入的请求头；评测任务内调用时使用任务绑定的 `appId`，默认是 `router-agent`
- 兼容 `data: {...}` SSE、普通JSON响应和非JSON文本响应
- 将流式返回的 `debug/reasoning/text/error` 聚合为 `outputs.debug / outputs.reasoning / outputs.text / outputs.error / outputs.rawText`
- 如果响应中出现 `type=error` 的内容块，Mock模块会将本次智能体调用状态标记为 `failed`，评测任务会记录错误并跳过该条数据的自动评估器执行

## 1. 智能体模拟

`GET /api/mock/agents`

返回当前用于任务创建页联调的 Mock 智能体定义：

```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "id": "router-agent",
      "agentName": "Mock超级智能体",
      "description": "用于评测任务联调的本地模拟超级智能体，按debug/reasoning/text三类消息模拟SSE输出。",
      "versions": [
        {
          "id": "router-agent-v1",
          "versionName": "V1"
        }
      ],
      "inputs": [
        {
          "id": "query",
          "fieldName": "query",
          "fieldType": "string",
          "description": "用户输入或问题",
          "displayOrder": 1
        }
      ],
      "outputs": [
        {
          "id": "text",
          "fieldName": "text",
          "fieldType": "string",
          "description": "返回给用户的信息",
          "displayOrder": 1
        },
        {
          "id": "reasoning",
          "fieldName": "reasoning",
          "fieldType": "string",
          "description": "智能体思考过程",
          "displayOrder": 2
        },
        {
          "id": "debug",
          "fieldName": "debug",
          "fieldType": "string",
          "description": "智能体调试信息",
          "displayOrder": 3
        },
        {
          "id": "error",
          "fieldName": "error",
          "fieldType": "string",
          "description": "智能体错误信息",
          "displayOrder": 4
        },
        {
          "id": "rawText",
          "fieldName": "rawText",
          "fieldType": "string",
          "description": "消息合并后的原始文本",
          "displayOrder": 5
        }
      ]
    }
  ]
}
```

`POST /api/mock/agent/chat`

请求头：

```http
x-agent-alias: router-agent
```

请求：

```json
{
  "conversationId": "",
  "message": [
    {
      "content": "question: 什么是评测系统？",
      "role": "user"
    }
  ],
  "stream": false
}
```

返回：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "id": "mock_chunk_id",
    "conversationId": "mock_conversation_id",
    "masterAgent": "router-agent",
    "metaAgent": "mock-meta-agent",
    "object": "com.evalsystem.mock.dto.MockAgentChatResponse",
    "created": 1710000000000,
    "nmodel": "mock-super-agent-model",
    "choices": [
      {
        "index": 0,
        "delta": {
          "role": "assistant",
          "content": [
            {
              "type": "debug",
              "text": "Mock调试信息：x-agent-alias=router-agent，stream=false",
              "reasoning": null,
              "error": null
            }
          ],
          "tool_calls": null,
          "extra": null
        },
        "finish_reason": null
      },
      {
        "index": 1,
        "delta": {
          "role": "assistant",
          "content": [
            {
              "type": "reasoning",
              "text": null,
              "reasoning": "Mock思考过程：读取用户输入，抽取关键信息，并生成用于评测的稳定回复。",
              "error": null
            }
          ],
          "tool_calls": null,
          "extra": null
        },
        "finish_reason": null
      },
      {
        "index": 2,
        "delta": {
          "role": "assistant",
          "content": [
            {
              "type": "text",
              "text": "Mock智能体回复：question: 什么是评测系统？",
              "reasoning": null,
              "error": null
            }
          ],
          "tool_calls": null,
          "extra": null
        },
        "finish_reason": "stop"
      }
    ],
    "status": "completed",
    "outputs": {
      "debug": "Mock调试信息：x-agent-alias=router-agent，stream=false",
      "reasoning": "Mock思考过程：读取用户输入，抽取关键信息，并生成用于评测的稳定回复。",
      "text": "Mock智能体回复：question: 什么是评测系统？",
      "error": "",
      "answer": "Mock智能体回复：question: 什么是评测系统？",
      "content": "Mock智能体回复：question: 什么是评测系统？",
      "rawText": "Mock调试信息：x-agent-alias=router-agent，stream=false\nMock思考过程：读取用户输入，抽取关键信息，并生成用于评测的稳定回复。\nMock智能体回复：question: 什么是评测系统？"
    },
    "latencyMs": 1,
    "errorMessage": "",
    "rawOutput": "Mock调试信息：x-agent-alias=router-agent，stream=false\nMock思考过程：读取用户输入，抽取关键信息，并生成用于评测的稳定回复。\nMock智能体回复：question: 什么是评测系统？"
  }
}
```

评测任务启动时会从 `choices[].delta.content[]` 中归并三类输出：

| 类型 | 任务输出键 | 用途 |
| --- | --- | --- |
| `debug` | `debug` | 调试信息 |
| `reasoning` | `reasoning` | 智能体思考过程 |
| `text` | `text` | 返回给用户的信息，默认用于评估器映射 |
| `error` | `error` | 智能体错误信息，出现时任务会按智能体调用失败处理 |

为了兼容旧任务，任务模块也会把 `text` 映射为 `answer` 和 `content` 别名；`app_output` 数据库存储为包含 `debug/reasoning/text/error/rawText` 的JSON字符串。

## 2. 评估器模拟

`POST /api/mock/evaluators/evaluate`

LLM型请求：

```json
{
  "taskId": "task_001",
  "taskItemId": "item_001",
  "taskEvaluatorId": "task_eval_001",
  "evaluatorName": "答案质量评估",
  "evaluatorType": "llm",
  "modelId": "mock-llm",
  "promptTemplate": "请根据问题和回答打分。问题：${question}\n回答：${answer}",
  "renderedPrompt": "请根据问题和回答打分。问题：什么是评测系统？\n回答：Mock智能体回复。",
  "executeCode": "",
  "scoreMin": 1,
  "scoreMax": 5,
  "passThreshold": 3,
  "params": {
    "question": "什么是评测系统？",
    "answer": "Mock智能体回复。"
  }
}
```

Code型请求：

```json
{
  "taskId": "task_001",
  "taskItemId": "item_001",
  "taskEvaluatorId": "task_eval_002",
  "evaluatorName": "关键词匹配",
  "evaluatorType": "code",
  "promptTemplate": "",
  "renderedPrompt": "",
  "executeCode": "def evaluate(params):\n    return {\"score\": 1, \"reason\": \"ok\"}",
  "scoreMin": 0,
  "scoreMax": 100,
  "passThreshold": 60,
  "params": {
    "prediction": "Mock智能体回复。",
    "reference": "参考答案"
  }
}
```

返回：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "status": "completed",
    "score": 4.2312,
    "reason": "Mock LLM评估完成，已根据渲染后的Prompt生成稳定分数。",
    "errorMessage": "",
    "rawOutput": "{\"score\":4.2312,\"reason\":\"Mock LLM评估完成，已根据渲染后的Prompt生成稳定分数。\"}",
    "latencyMs": 1
  }
}
```

任务模块会使用 `score` 和评估器的 `passThreshold` 自行计算 `pass/fail`，并写入 `eval_task_evaluator_result`。

## 3. Mock控制指令

在智能体消息、评估Prompt或参数值中放入以下指令可模拟边界情况：

| 指令 | 效果 |
| --- | --- |
| `[mock:agent_fail]` | 智能体调用返回失败 |
| `[mock:evaluator_fail]` | 评估器调用返回失败 |
| `[mock:timeout]` | 模拟调用超时失败 |
| `[mock:score=88]` | 强制评估器返回指定分数，会按评分范围裁剪 |
| `[mock:agent_output=指定输出]` | 强制智能体返回指定内容 |
| `[mock:debug=指定调试信息]` | 强制智能体返回指定debug内容 |
| `[mock:reasoning=指定思考过程]` | 强制智能体返回指定reasoning内容 |
| `[mock:error=指定错误信息]` | 强制智能体返回指定error内容，并标记调用失败 |
