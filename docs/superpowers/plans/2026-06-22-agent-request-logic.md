# Agent Request Logic Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace fixed super agent chat configuration with dynamic agent detail lookup and add snapshot plus optional child agent selection to task creation.

**Architecture:** Backend integration owns external platform calls and exposes list/detail DTOs to the frontend. Task creation persists the selected super agent, selected snapshot bundle, and optional child alias. Task execution resolves the selected agent detail and posts to the dynamic chat endpoint.

**Tech Stack:** Java 21, Spring Boot, MyBatis Plus, PostgreSQL DDL, JUnit 5, Mockito, Vue 3, TypeScript, Element Plus.

---

### Task 1: Backend Agent Detail DTOs And Tests

**Files:**
- Modify: `backend/src/test/java/com/evalsystem/integration/service/PlatformIntegrationServiceTest.java`
- Modify: `backend/src/main/java/com/evalsystem/integration/config/PlatformIntegrationProperties.java`
- Modify: `backend/src/main/java/com/evalsystem/integration/api/dto/response/PlatformAgentDefinition.java`
- Create: `backend/src/main/java/com/evalsystem/integration/api/dto/response/PlatformAgentDetailResponse.java`
- Create: `backend/src/main/java/com/evalsystem/integration/api/dto/response/PlatformSuperAgentDetail.java`
- Create: `backend/src/main/java/com/evalsystem/integration/api/dto/response/PlatformAgentInstance.java`
- Create: `backend/src/main/java/com/evalsystem/integration/api/dto/response/PlatformLoadedAgent.java`
- Modify: `backend/src/main/java/com/evalsystem/integration/service/PlatformIntegrationService.java`

- [ ] Write a failing integration service test that calls `service.getAgentDetail("agent-1")`, verifies request headers `Cookie` and `x-space-id`, and asserts versions are `bundle-main`, `bundle-old` with blank/duplicate instance bundle versions removed.
- [ ] Run `mvn -Dtest=PlatformIntegrationServiceTest#getAgentDetailSendsAuthHeadersAndNormalizesSnapshots test` and confirm it fails because `getAgentDetail` does not exist.
- [ ] Add DTO records for detail response, detail object, instances, and loaded agents.
- [ ] Add `agentDetailUrl` property with getter/setter.
- [ ] Implement `getAgentDetail`, URL template replacement, snapshot normalization, child alias normalization, and agent detail mapping.
- [ ] Run the new test and confirm it passes.

### Task 2: Dynamic Agent Chat Tests And Implementation

**Files:**
- Modify: `backend/src/test/java/com/evalsystem/integration/service/PlatformIntegrationServiceTest.java`
- Modify: `backend/src/main/java/com/evalsystem/integration/service/PlatformIntegrationService.java`

- [ ] Write a failing test that stubs `/agents/agent-1` returning `accessUrl`, calls `invokeAgent("agent-1", "", request)`, and verifies the post goes to `/dynamic/chat/completions` without `x-agent-alias`.
- [ ] Write a failing test that calls `invokeAgent("agent-1", "child-a", request)` and verifies `x-agent-alias: child-a`.
- [ ] Run both tests and confirm they fail because the current code posts to `super-agent-chat-url` and ignores per-task alias.
- [ ] Change `invokeAgent` signature to `(String agentId, String agentAlias, PlatformAgentChatRequest request)`.
- [ ] Resolve detail inside `invokeAgent`, build the chat URL from `accessUrl`, and send `x-agent-alias` only when nonblank.
- [ ] Run the targeted integration service tests and confirm they pass.

### Task 3: Persist Child Agent Alias On Tasks

**Files:**
- Modify: `DDL/04_eval_task.sql`
- Modify: `backend/src/main/java/com/evalsystem/task/entity/EvalTask.java`
- Modify: `backend/src/main/java/com/evalsystem/task/api/dto/request/CreateTaskRequest.java`
- Modify: `backend/src/main/java/com/evalsystem/task/api/dto/response/TaskBase.java`
- Modify: `backend/src/main/java/com/evalsystem/task/repository/TaskRepository.java`
- Modify: `backend/src/main/resources/mapper/TaskMapper.xml`
- Modify: `backend/src/main/java/com/evalsystem/task/service/TaskService.java`
- Modify: `backend/src/test/java/com/evalsystem/task/service/TaskServicePresetDisplayTest.java`

- [ ] Write a failing task service test that creates an agent task with `appAgentAlias = "child-a"` and verifies `insertTask` receives that alias.
- [ ] Run the task service test and confirm it fails because the request/insert signatures lack the field.
- [ ] Add `app_agent_alias` to DDL and entity.
- [ ] Add `appAgentAlias` to request, response, repository insert, mapper columns, and normalized task record.
- [ ] Normalize alias to blank for non-agent tasks and trim it for agent tasks.
- [ ] Change task execution to call `integrationService.invokeAgent(base.appId(), base.appAgentAlias(), request)`.
- [ ] Update existing tests and run the targeted task service tests.

### Task 4: Backend API And Configuration

**Files:**
- Modify: `backend/src/main/java/com/evalsystem/integration/api/PlatformIntegrationController.java`
- Modify: `backend/src/main/resources/application-dev.yml`
- Modify: `backend/src/main/resources/application-prod.yml`

- [ ] Add `GET /api/integration/agents/{agentId}` that returns `PlatformAgentDefinition`.
- [ ] Add `agent-detail-url` to dev/prod configuration next to `agent-list-url`.
- [ ] Run `mvn -Dtest=PlatformIntegrationServiceTest,TaskServicePresetDisplayTest test` and confirm targeted backend tests pass.

### Task 5: Frontend Lazy Agent Detail Selection

**Files:**
- Modify: `frontend/src/api/integration.ts`
- Modify: `frontend/src/api/task.ts`
- Modify: `frontend/src/types.ts`
- Modify: `frontend/src/modules/task/composables/useTaskCreate.ts`
- Modify: `frontend/src/views/TaskCreateView.vue`
- Modify: `frontend/src/views/TaskDetailView.vue`
- Modify: `frontend/src/views/TaskManagementView.vue`

- [ ] Add frontend types for `childAgents` and `appAgentAlias`.
- [ ] Add `integrationApi.getAgentDetail(agentId)`.
- [ ] Keep list loading lazy on dropdown open.
- [ ] Add a watcher on `form.appId` that loads detail only after a concrete agent is selected.
- [ ] Populate versions, child aliases, inputs, and outputs from the selected detail.
- [ ] Add a child agent select with a blank option for super agent mode.
- [ ] Include `appAgentAlias` in create task payload.
- [ ] Display child alias in task list/detail when present.
- [ ] Run `npm run build`.

### Task 6: Full Verification, Commit, Push

**Files:**
- All modified files.

- [ ] Run `cd backend && mvn test`.
- [ ] Run `cd frontend && npm run build`.
- [ ] Run `git diff --check`.
- [ ] Inspect `git diff --stat` and `git status --short`.
- [ ] Commit all relevant changes with message `智能体请求逻辑修改`.
- [ ] Push `master` to the configured GitHub remote.
