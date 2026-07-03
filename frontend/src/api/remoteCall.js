import { http, unwrap } from './http';
export const remoteCallApi = {
    listSpaces(pageSize = 20, curPage = 1) {
        return unwrap(http.get(`/remoteCall/spaces/${pageSize}/${curPage}`));
    },
    listModels() {
        return unwrap(http.get('/remoteCall/models'));
    },
    listAgents() {
        return unwrap(http.get('/remoteCall/agents'));
    },
    getAgentDetail(agentId) {
        return unwrap(http.get(`/remoteCall/agents/${encodeURIComponent(agentId)}`));
    },
    listAgentBundles(agentId) {
        return unwrap(http.get(`/remoteCall/agents/${encodeURIComponent(agentId)}/bundles`));
    },
    chatModel(modelId, message) {
        return unwrap(http.post(`/remoteCall/models/${encodeURIComponent(modelId)}/chat`, { message }));
    }
};
