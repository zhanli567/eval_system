import axios from 'axios';
const http = axios.create({
    baseURL: '/api',
    timeout: 10000
});
function unwrap(request) {
    return request
        .then((res) => {
        if (res.data.code !== 0) {
            throw new Error(res.data.msg);
        }
        return res.data.data;
    })
        .catch((error) => {
        const message = error?.response?.data?.msg || error?.message || '请求失败';
        throw new Error(message);
    });
}
export const integrationApi = {
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
