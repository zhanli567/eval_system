# Mock服务接口

Mock模块用于在未接入真实智能体、评估LLM和Code执行器前，模拟评测任务的执行闭环。接口均走项目统一返回结构：

```json
{
  "code": 0,
  "msg": "success",
  "data": {}
}
```

## 1. 智能体模拟

`GET /api/mock/agents`

返回当前用于任务创建页联调的 Mock 智能体定义：

```json
{
  "code": 0,
  "msg": "success",
  "data": [
    {
      "id": "mock-agent",
      "agentName": "Mock智能体",
      "description": "用于评测任务联调的本地模拟智能体，会把映射后的输入拼接为回答。",
      "versions": [
        {
          "id": "mock-agent-v1",
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
          "id": "answer",
          "fieldName": "answer",
          "fieldType": "string",
          "description": "标准应用输出字段",
          "displayOrder": 1
        },
        {
          "id": "content",
          "fieldName": "content",
          "fieldType": "string",
          "description": "兼容消息内容字段",
          "displayOrder": 2
        },
        {
          "id": "rawText",
          "fieldName": "rawText",
          "fieldType": "string",
          "description": "原始文本输出字段",
          "displayOrder": 3
        }
      ]
    }
  ]
}
```

`POST /api/mock/agent/chat`

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
    "conversationId": "mock_conversation_id",
    "message": {
      "content": "Mock智能体回复：question: 什么是评测系统？",
      "role": "assistant"
    },
    "status": "completed",
    "outputs": {
      "answer": "Mock智能体回复：question: 什么是评测系统？",
      "content": "Mock智能体回复：question: 什么是评测系统？",
      "rawText": "Mock智能体回复：question: 什么是评测系统？"
    },
    "latencyMs": 1,
    "errorMessage": "",
    "rawOutput": "Mock智能体回复：question: 什么是评测系统？"
  }
}
```

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
