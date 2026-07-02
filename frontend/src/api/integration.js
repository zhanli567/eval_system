import { http, unwrap } from './http';
export const integrationApi = {
    listSpaces(pageSize = 20, curPage = 1) {
        return unwrap(http.get(`/integration/spaces/${pageSize}/${curPage}`));
    },
    listModels() {
        return unwrap(http.get('/integration/models'));
    },
    listAgents() {
        return unwrap(http.get('/integration/agents'));
    },
    getAgentDetail(agentId) {
        return unwrap(http.get(`/integration/agents/${encodeURIComponent(agentId)}`));
    },
    listAgentBundles(agentId) {
        return unwrap(http.get(`/integration/agents/${encodeURIComponent(agentId)}/bundles`));
    },
    chatModel(modelId, message) {
        return unwrap(http.post(`/integration/models/${encodeURIComponent(modelId)}/chat`, { message }));
    }
};
