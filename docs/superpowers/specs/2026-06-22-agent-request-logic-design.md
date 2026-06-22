# Agent Request Logic Design

## Goal

Support evaluating a selected super agent, an optional child agent, and a selected snapshot bundle while resolving the chat endpoint dynamically from the selected agent detail.

## Current Flow

The task creation page lazily loads the super agent list from `/api/integration/agents`. A created task stores `appId` and `appVersionId`. During task execution, `TaskService` calls `PlatformIntegrationService.invokeAgent`, which posts to the configured `integration.platform.super-agent-chat-url` and only sends `x-agent-alias` when the global config value is present.

## Target Flow

The agent list remains lightweight and lazy. After the user selects a super agent, the frontend calls a new backend endpoint to load that agent's detail. The backend calls the configured `integration.platform.agent-detail-url` with the existing login Cookie and `x-space-id`.

The detail response is converted into a frontend-safe definition:

- `accessUrl` is retained by the backend and used to build `{accessUrl}/chat/completions`.
- Snapshot choices come from `bundleVersion`, `currentBundleId`, and `instances[*].bundleVersion`, with blank values removed and duplicates collapsed.
- Child agent choices come from `loadedAgents[*].agentAlias`, with blank values removed and duplicates collapsed.
- Inputs and outputs keep the existing static field definitions: `query` input and `text/reasoning/debug/error/rawText` outputs.

Task creation stores:

- `appId`: selected super agent ID.
- `appVersionId`: selected snapshot bundle version.
- `appAgentAlias`: optional child agent alias. Blank means evaluate the super agent.

During execution, `TaskService` passes `appId`, `appAgentAlias`, and the chat request to integration. The integration service loads the selected agent detail, builds the dynamic chat URL from `accessUrl`, sends Cookie, and sends `x-agent-alias` only when `appAgentAlias` is nonblank.

## Data Model

Add `app_agent_alias VARCHAR(128)` to `t_eval_task`. Existing tasks receive an empty value via `COALESCE` in query mapping. Existing `appId/appVersionId` semantics are preserved.

## Configuration

Add `integration.platform.agent-detail-url` to dev/prod configuration and `PlatformIntegrationProperties`. The URL supports `{agentId}` and `{agentid}` placeholders. If no placeholder is present, append the URL-encoded agent ID as the last path segment.

Keep `super-agent-chat-url` and `x-agent-alias` properties only for backwards configuration compatibility; the new task execution path does not depend on them.

## Error Handling

Agent detail calls reuse the existing login cookie refresh behavior. Invalid or blank `accessUrl` causes the sample's agent result to fail without stopping the whole task loop, matching current task execution semantics.

## Verification

Backend tests cover:

- Agent detail request sends Cookie and `x-space-id`.
- Snapshot bundle choices are deduplicated and blanks are ignored.
- Child agent aliases are deduplicated and blanks are ignored.
- Agent chat posts to `accessUrl + /chat/completions`.
- Empty child alias does not send `x-agent-alias`; nonempty child alias does.

Frontend build verifies TypeScript and Vue template compatibility.
